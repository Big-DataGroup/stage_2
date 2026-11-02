package org.ulpgc.bigdata.search.client;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class IndexingClient {
    private final HttpClient httpClient;
    private final String baseUrl;

    public IndexingClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(Long.parseLong(
                System.getenv().getOrDefault("SERVICE_CONNECT_TIMEOUT_SECONDS", "5")))).build(),
                System.getenv().getOrDefault(
                "INDEXING_SERVICE_URL", "http://localhost:7002"));
    }

    public IndexingClient(HttpClient httpClient, String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
    }

    public HttpResponse<String> search(String query, String author, String language,
                                       Integer year, int offset, int limit)
            throws IOException, InterruptedException {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }
        StringBuilder uri = new StringBuilder(baseUrl).append("/index/search?q=")
                .append(encode(query));
        append(uri, "author", author);
        append(uri, "language", language);
        if (year != null) append(uri, "year", String.valueOf(year));
        append(uri, "offset", String.valueOf(offset));
        append(uri, "limit", String.valueOf(limit));
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri.toString()))
                .timeout(Duration.ofSeconds(Long.parseLong(
                        System.getenv().getOrDefault("SERVICE_REQUEST_TIMEOUT_SECONDS", "30"))))
                .GET().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> search(String query)
            throws IOException, InterruptedException {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(
                baseUrl + "/index/search/" + encode(query)))
                .timeout(Duration.ofSeconds(Long.parseLong(
                        System.getenv().getOrDefault("SERVICE_REQUEST_TIMEOUT_SECONDS", "30"))))
                .GET().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void append(StringBuilder uri, String name, String value) {
        if (value != null && !value.isBlank()) uri.append('&').append(name).append('=').append(encode(value));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
