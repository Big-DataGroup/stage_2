package org.ulpgc.bigdata.indexing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class IndexManager {
    private final InvertedIndex index = new InvertedIndex();
    private final Tokenizer tokenizer = new Tokenizer();
    private final Map<Integer, BookDocument> documents = new ConcurrentHashMap<>();
    private final Map<Integer, Set<String>> termsByBook = new ConcurrentHashMap<>();
    private final Path statePath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private volatile long lastUpdate;

    public IndexManager() {
        this(Path.of(System.getenv().getOrDefault(
                "INDEX_STATE_PATH", "data/indexing/index-state.json")));
    }

    public IndexManager(Path statePath) {
        this.statePath = statePath;
        load();
    }

    public synchronized void indexText(int bookId, String rawContent) throws IOException {
        if (bookId <= 0 || rawContent == null || rawContent.isBlank()) {
            throw new IllegalArgumentException("Book ID and content are required");
        }
        removeBook(bookId);
        Set<String> tokens = tokenizer.tokenize(rawContent);
        index.addDocument(bookId, tokens);
        termsByBook.put(bookId, new HashSet<>(tokens));
        documents.put(bookId, parseMetadata(bookId, rawContent));
        lastUpdate = System.currentTimeMillis();
        save();
    }

    public synchronized void rebuild() throws IOException {
        index.clear();
        documents.clear();
        termsByBook.clear();
        if (Files.exists(statePath)) {
            Files.delete(statePath);
        }
        lastUpdate = System.currentTimeMillis();
    }

    public SearchResult search(String query, String author, String language, Integer year,
                               int offset, int limit) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query cannot be empty");
        }
        int safeOffset = Math.max(offset, 0);
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        List<BookDocument> matches = new ArrayList<>();
        for (Integer bookId : index.search(query)) {
            BookDocument document = documents.get(bookId);
            if (document != null && document.matches(author, language, year)) {
                matches.add(document);
            }
        }
        matches.sort(Comparator.comparingInt(BookDocument::bookId));
        int from = Math.min(safeOffset, matches.size());
        int to = Math.min(from + safeLimit, matches.size());
        return new SearchResult(query, new SearchResult.Filters(author, language, year),
                matches.size(), safeOffset, safeLimit, matches.subList(from, to));
    }

    public synchronized Map<String, Object> status() {
        Map<String, Object> status = new HashMap<>();
        status.put("books_indexed", documents.size());
        status.put("terms", index.getIndex().size());
        status.put("last_update", lastUpdate == 0 ? null : lastUpdate);
        return status;
    }

    public InvertedIndex getIndex() {
        return index;
    }

    private BookDocument parseMetadata(int bookId, String content) {
        String title = headerValue(content, "Title");
        String author = headerValue(content, "Author");
        String language = headerValue(content, "Language");
        Integer year = null;
        String date = headerValue(content, "Release Date");
        if (date != null) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("(1[5-9]\\d{2}|20\\d{2})").matcher(date);
            if (matcher.find()) {
                year = Integer.valueOf(matcher.group(1));
            }
        }
        return new BookDocument(bookId,
                title == null ? "Book " + bookId : title,
                author == null ? "" : author,
                language == null ? "" : language,
                year);
    }

    private String headerValue(String content, String name) {
        for (String line : content.split("\\R")) {
            if (line.toLowerCase(Locale.ROOT).startsWith(name.toLowerCase(Locale.ROOT) + ":")) {
                return line.substring(name.length() + 1).trim();
            }
        }
        return null;
    }

    private void removeBook(int bookId) {
        Set<String> oldTerms = termsByBook.remove(bookId);
        if (oldTerms != null) {
            index.removeDocument(bookId, oldTerms);
        }
        documents.remove(bookId);
    }

    private void load() {
        if (!Files.exists(statePath)) {
            return;
        }
        try {
            State state = gson.fromJson(Files.readString(statePath), State.class);
            if (state == null) return;
            if (state.documents != null) documents.putAll(state.documents);
            if (state.termsByBook != null) {
                state.termsByBook.forEach((id, terms) -> {
                    Set<String> safeTerms = terms == null ? Set.of() : new HashSet<>(terms);
                    termsByBook.put(Integer.valueOf(id), safeTerms);
                    index.addDocument(Integer.parseInt(id), safeTerms);
                });
            }
            lastUpdate = state.lastUpdate;
        } catch (IOException | RuntimeException ignored) {
            documents.clear();
            termsByBook.clear();
            index.clear();
        }
    }

    private void save() throws IOException {
        Path parent = statePath.getParent();
        if (parent != null) Files.createDirectories(parent);
        State state = new State(new HashMap<>(documents), new HashMap<>(termsByBook), lastUpdate);
        Path temporary = Files.createTempFile(parent == null ? Path.of(".") : parent, "index-", ".tmp");
        try {
            Files.writeString(temporary, gson.toJson(state));
            try {
                Files.move(temporary, statePath, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, statePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static class State {
        Map<Integer, BookDocument> documents;
        Map<String, Set<String>> termsByBook = new HashMap<>();
        long lastUpdate;
        State(Map<Integer, BookDocument> documents, Map<Integer, Set<String>> termsByBook, long lastUpdate) {
            this.documents = new HashMap<>();
            termsByBook.forEach((id, terms) -> this.termsByBook.put(String.valueOf(id), terms));
            this.documents.putAll(documents);
            this.lastUpdate = lastUpdate;
        }
    }
}
