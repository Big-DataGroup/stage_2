
package org.ulpgc.bigdata.ingestion;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class IngestionRepository {

    private final Path filePath;
    private final Gson gson;
    private final Map<Integer, String> books;

    public IngestionRepository(Path filePath) throws IOException {

        this.filePath = filePath;

        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        this.books = load();
    }

    private Map<Integer, String> load() throws IOException {

        if (!Files.exists(filePath)) {
            return new HashMap<>();
        }

        String json = Files.readString(filePath);

        Type type = new TypeToken<Map<Integer, String>>() {}.getType();

        Map<Integer, String> loaded = gson.fromJson(json, type);

        return loaded != null ? loaded : new HashMap<>();
    }

    public synchronized void markAvailable(int bookId)
            throws IOException {

        String previousStatus = books.put(bookId, "AVAILABLE");

        try {
            save();

        } catch (IOException e) {

            // Restore the previous in-memory state
            if (previousStatus == null) {
                books.remove(bookId);
            } else {
                books.put(bookId, previousStatus);
            }

            throw e;
        }
    }

    public synchronized boolean isAvailable(int bookId) {

        return "AVAILABLE".equals(books.get(bookId));
    }

    public synchronized Map<Integer, String> listBooks() {

        return new HashMap<>(books);
    }

    private void save() throws IOException {

        Path parent = filePath.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        String json = gson.toJson(books);

        Path temporaryFile = Files.createTempFile(
                parent != null ? parent : Path.of("."),
                "ingestion-",
                ".tmp"
        );

        try {
            Files.writeString(temporaryFile, json);

            try {
                Files.move(
                        temporaryFile,
                        filePath,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );

            } catch (AtomicMoveNotSupportedException e) {

                Files.move(
                        temporaryFile,
                        filePath,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }
}
