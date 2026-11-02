
package org.ulpgc.bigdata.control.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class IngestionClient {

    private final HttpClient httpClient;
    private final String baseUrl;

    // Constructor used by the application
    public IngestionClient() {
        this(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(
                                Long.parseLong(System.getenv().getOrDefault("SERVICE_CONNECT_TIMEOUT_SECONDS", "5"))))
                        .build(),
                System.getenv().getOrDefault(
                        "INGESTION_SERVICE_URL",
                        "http://localhost:7001"
                )
        );
    }

    // Constructor used for testing
    public IngestionClient(HttpClient httpClient, String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
    }

    // POST: Request book ingestion
    public HttpResponse<String> ingestBook(int bookId)
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/ingest/" + bookId))
                .timeout(Duration.ofSeconds(timeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
    }

    // GET: Retrieve book content
    public HttpResponse<String> getBookContent(int bookId)
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        baseUrl + "/ingest/content/" + bookId
                ))
                .timeout(Duration.ofSeconds(timeoutSeconds()))
                .GET()
                .build();

        return httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private long timeoutSeconds() {
        return Long.parseLong(System.getenv().getOrDefault("SERVICE_REQUEST_TIMEOUT_SECONDS", "30"));
    }
}
