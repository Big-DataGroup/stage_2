
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

    public IngestionClient(HttpClient httpClient, String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
    }

    public HttpResponse<String> ingestBook(int bookId)
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/ingest/" + bookId))
                .timeout(Duration.ofSeconds(timeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return sendWithRetry(request);
    }

    public HttpResponse<String> getBookContent(int bookId)
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        baseUrl + "/ingest/content/" + bookId
                ))
                .timeout(Duration.ofSeconds(timeoutSeconds()))
                .GET()
                .build();

        return sendWithRetry(request);
    }

    private long timeoutSeconds() {
        return Long.parseLong(System.getenv().getOrDefault("SERVICE_REQUEST_TIMEOUT_SECONDS", "30"));
    }

    private HttpResponse<String> sendWithRetry(HttpRequest request)
            throws IOException, InterruptedException {
        int attempts = Math.max(1, Integer.parseInt(System.getenv()
                .getOrDefault("SERVICE_MAX_ATTEMPTS", "3")));
        IOException lastError = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException e) {
                lastError = e;
                if (attempt < attempts) Thread.sleep(100L * attempt);
            }
        }
        throw lastError;
    }
}
