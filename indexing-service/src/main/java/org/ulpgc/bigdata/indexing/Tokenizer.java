package org.ulpgc.bigdata.indexing;

import java.util.HashSet;
import java.util.Set;


public class Tokenizer {

    public Set<String> tokenize(String content) {
        Set<String> uniqueWords = new HashSet<>();

        if (content == null || content.isBlank()) {
            return uniqueWords;
        }

        String[] lines = content.split("\\r?\\n");

        for (String line : lines) {
            line = line.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
            String[] words = line.split("\\s+");

            for (String word : words) {
                if (!word.isBlank()) {
                    uniqueWords.add(word);
                }
            }
        }

        return uniqueWords;
    }
}
