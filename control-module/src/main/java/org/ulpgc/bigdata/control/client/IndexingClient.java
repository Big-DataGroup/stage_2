
package org.ulpgc.bigdata.control.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class IndexingClient {

    private final HttpClient httpClient;
    private final String baseUrl;

    // Constructor used by the application
    public IndexingClient() {
        this(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(
                                Long.parseLong(System.getenv().getOrDefault("SERVICE_CONNECT_TIMEOUT_SECONDS", "5"))))
                        .build(),
                System.getenv().getOrDefault(
                        "INDEXING_SERVICE_URL",
                        "http://localhost:7002"
                )
        );
    }

    // Constructor used for testing
    public IndexingClient(HttpClient httpClient, String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
    }

    // POST: Send book content to Indexing Service
    public HttpResponse<String> indexBook(
            int bookId,
            String content
    ) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        baseUrl + "/index/update/" + bookId
                ))
                .timeout(Duration.ofSeconds(Long.parseLong(
                        System.getenv().getOrDefault("SERVICE_REQUEST_TIMEOUT_SECONDS", "30"))))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(content))
                .build();

        return httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
    }
}
