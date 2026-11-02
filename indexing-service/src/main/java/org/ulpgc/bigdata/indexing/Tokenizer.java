package org.ulpgc.bigdata.indexing;

import java.util.HashSet;
import java.util.Set;

/**
 * Estructura de salida: Conjunto de términos únicos por documento (Set<String>).
 */
public class Tokenizer {

    public Set<String> tokenize(String content) {
        Set<String> uniqueWords = new HashSet<>();

        if (content == null || content.isBlank()) {
            return uniqueWords;
        }

        // Separamos el contenido por líneas para procesarlo eficientemente en memoria
        String[] lines = content.split("\\r?\\n");

        for (String line : lines) {
            // Aplicamos exactamente las mismas reglas de normalización del Stage 1
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