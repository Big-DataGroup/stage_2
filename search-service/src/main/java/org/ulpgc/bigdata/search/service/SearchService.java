package org.ulpgc.bigdata.search.service;

import org.ulpgc.bigdata.search.client.IndexingClient;

import java.io.IOException;
import java.net.http.HttpResponse;

public class SearchService {
    private final IndexingClient indexingClient;

    public SearchService() {
        this(new IndexingClient());
    }

    public SearchService(IndexingClient indexingClient) {
        this.indexingClient = indexingClient;
    }

    public String search(String query, String author, String language,
                         Integer year, int offset, int limit)
            throws IOException, InterruptedException {
        HttpResponse<String> response = indexingClient.search(
                query, author, language, year, offset, limit);
        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Indexing Service returned status: " + response.statusCode());
        }
        return response.body();
    }

    public String search(String query) throws IOException, InterruptedException {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }
        HttpResponse<String> response = indexingClient.search(query);
        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Indexing Service returned status: " + response.statusCode());
        }
        return response.body();
    }
}
