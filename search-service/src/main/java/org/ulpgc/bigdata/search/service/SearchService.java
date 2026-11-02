
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

    public String search(String word)
            throws IOException, InterruptedException {

        if (word == null || word.isBlank()) {
            throw new IllegalArgumentException(
                    "Search word cannot be empty"
            );
        }

        HttpResponse<String> response =
                indexingClient.search(word);

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Indexing Service returned status: "
                            + response.statusCode()
            );
        }

        return response.body();
    }
}
