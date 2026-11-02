
package org.ulpgc.bigdata.search.client;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class IndexingClient {

    private final HttpClient httpClient;
    private final String baseUrl;

    public IndexingClient() {
        this(
                HttpClient.newHttpClient(),
                System.getenv().getOrDefault(
                        "INDEXING_SERVICE_URL",
                        "http://localhost:7002"
                )
        );
    }

    public IndexingClient(HttpClient httpClient, String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
    }

    public HttpResponse<String> search(String word)
            throws IOException, InterruptedException {

        if (word == null || word.isBlank()) {
            throw new IllegalArgumentException(
                    "Search word cannot be empty"
            );
        }

        String encodedWord = URLEncoder.encode(
                word,
                StandardCharsets.UTF_8
        ).replace("+", "%20");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        baseUrl + "/index/search/" + encodedWord
                ))
                .GET()
                .build();

        return httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
    }
}
