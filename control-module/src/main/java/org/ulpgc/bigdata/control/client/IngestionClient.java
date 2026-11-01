
package org.ulpgc.bigdata.control.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class IngestionClient {

    private final HttpClient httpClient;
    private final String baseUrl;

    public IngestionClient() {

        this.httpClient = HttpClient.newHttpClient();

        this.baseUrl = System.getenv().getOrDefault(
                "INGESTION_SERVICE_URL",
                "http://localhost:7001"
        );
    }

    public HttpResponse<String> ingestBook(int bookId)
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/ingest/" + bookId))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
    }
}
