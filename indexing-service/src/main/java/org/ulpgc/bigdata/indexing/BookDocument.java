package org.ulpgc.bigdata.indexing;

public record BookDocument(
        int bookId,
        String title,
        String author,
        String language,
        Integer year
) {
    public boolean matches(String authorFilter, String languageFilter, Integer yearFilter) {
        return (authorFilter == null || author.toLowerCase().contains(authorFilter.toLowerCase()))
                && (languageFilter == null || language.equalsIgnoreCase(languageFilter))
                && (yearFilter == null || yearFilter.equals(year));
    }
}
