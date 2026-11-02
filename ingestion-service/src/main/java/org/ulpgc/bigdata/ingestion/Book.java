package org.ulpgc.bigdata.ingestion;

public class Book {

    private String id;
    private String title;
    private String sourceUrl;
    private String status;

    public Book(String id, String title, String sourceUrl, String status) {
        this.id = id;
        this.title = title;
        this.sourceUrl = sourceUrl;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getStatus() {
        return status;
    }
}
