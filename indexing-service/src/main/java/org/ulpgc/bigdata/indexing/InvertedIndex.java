package org.ulpgc.bigdata.indexing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Clase que gestiona el Indice Invertido en memoria.
 * Adaptada para Stage 2 (Microservicios): Hilo seguro (Thread-Safe) y desacoplada de I/O.
 */
public class InvertedIndex {

    // Usamos estructuras concurrentes para soportar multiples peticiones HTTP simultaneas
    private final Map<String, Set<Integer>> index;

    public InvertedIndex() {
        this.index = new ConcurrentHashMap<>();
    }

    /**
     * Añade un documento al indice.
     * Recibe directamente las palabras procesadas en lugar de leer de disco.
     */
    public void addDocument(int bookId, Set<String> words) {
        for (String word : words) {
            // computeIfAbsent + newKeySet() garantiza concurrencia sin bloqueos
            // y evita duplicados de forma nativa e instantanea (O(1))
            index.computeIfAbsent(word, k -> ConcurrentHashMap.newKeySet()).add(bookId);
        }
    }

    /**
     * Busca los IDs de los libros que contienen la palabra consultada.
     * Devuelve un List para mantener compatibilidad con la firma original del Stage 1.
     */
    public List<Integer> search(String word) {
        Set<Integer> postings = index.get(word.toLowerCase());
        return postings == null ? java.util.Collections.emptyList() : new ArrayList<>(postings);
    }

    /**
     * Devuelve el mapa subyacente del indice invertido.
     */
    public Map<String, Set<Integer>> getIndex() {
        return index;
    }

    /**
     * Calcula metricas reales de tamaño en memoria del indice (Puntos 16 y 32).
     */
    public MemoryFootprint getMemoryFootprint() {
        int totalTerms = index.size();
        long totalPostings = 0;

        for (Set<Integer> postings : index.values()) {
            totalPostings += postings.size();
        }

        // Estimacion aproximada de bytes en memoria
        long estimatedBytes = (totalTerms * 64L) + (totalPostings * 24L);

        return new MemoryFootprint(totalTerms, totalPostings, estimatedBytes);
    }

    /**
     * Registro auxiliar para la huella de memoria (Puntos 16 y 32).
     */
    public record MemoryFootprint(int totalTerms, long totalPostings, long estimatedBytes) {
        @Override
        public String toString() {
            return String.format("Terms: %d | Postings: %d | Est. Memory: %.2f KB",
                    totalTerms, totalPostings, estimatedBytes / 1024.0);
        }
    }

    // Método para ver el contenido en la consola (útil para depurar)
    public void printIndex() {
        for (Map.Entry<String, Set<Integer>> entry : index.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }
}