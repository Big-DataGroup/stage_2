package org.ulpgc.bigdata.indexing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class InvertedIndex {

    private final Map<String, Set<Integer>> index;

    public InvertedIndex() {
        this.index = new ConcurrentHashMap<>();
    }


    public void addDocument(int bookId, Set<String> words) {
        for (String word : words) {
            index.computeIfAbsent(word, k -> ConcurrentHashMap.newKeySet()).add(bookId);
        }
    }

    public void removeDocument(int bookId, Set<String> words) {
        for (String word : words) {
            Set<Integer> postings = index.get(word);
            if (postings != null) {
                postings.remove(bookId);
                if (postings.isEmpty()) {
                    index.remove(word, postings);
                }
            }
        }
    }

    public void clear() {
        index.clear();
    }


    public List<Integer> search(String word) {
        Set<Integer> postings = index.get(word.toLowerCase());
        return postings == null ? java.util.Collections.emptyList() : new ArrayList<>(postings);
    }


    public Map<String, Set<Integer>> getIndex() {
        return index;
    }


    public MemoryFootprint getMemoryFootprint() {
        int totalTerms = index.size();
        long totalPostings = 0;

        for (Set<Integer> postings : index.values()) {
            totalPostings += postings.size();
        }

        long estimatedBytes = (totalTerms * 64L) + (totalPostings * 24L);

        return new MemoryFootprint(totalTerms, totalPostings, estimatedBytes);
    }


    public record MemoryFootprint(int totalTerms, long totalPostings, long estimatedBytes) {
        @Override
        public String toString() {
            return String.format("Terms: %d | Postings: %d | Est. Memory: %.2f KB",
                    totalTerms, totalPostings, estimatedBytes / 1024.0);
        }
    }

    public void printIndex() {
        for (Map.Entry<String, Set<Integer>> entry : index.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }
}
