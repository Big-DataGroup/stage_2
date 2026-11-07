package org.ulpgc.bigdata.indexing;

import java.util.List;

public record SearchResult(
        String query,
        Filters filters,
        int total,
        int offset,
        int limit,
        List<BookDocument> results
) {
    public record Filters(String author, String language, Integer year) {}
}
