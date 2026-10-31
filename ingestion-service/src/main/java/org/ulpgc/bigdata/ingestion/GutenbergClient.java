
package org.ulpgc.bigdata.ingestion;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GutenbergClient {

    private static final String START_MARKER =
            "*** START OF THE PROJECT GUTENBERG EBOOK";

    private static final String END_MARKER =
            "*** END OF THE PROJECT GUTENBERG EBOOK";

    private final HttpClient httpClient;

    public GutenbergClient() {
        this(HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build());
    }

    GutenbergClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public BookText download(int bookId)
            throws IOException, InterruptedException,
            BookNotFoundException, GutenbergRequestException {

        String url = "https://www.gutenberg.org/cache/epub/"
                + bookId + "/pg" + bookId + ".txt";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(45))
                .header("User-Agent", "ULPGC-BigData-Stage2/1.0")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        // The book does not exist
        if (response.statusCode() == 404) {
            throw new BookNotFoundException(
                    "Book " + bookId
                            + " was not found on Project Gutenberg"
            );
        }

        // Gutenberg returned an unexpected HTTP error
        if (response.statusCode() != 200) {
            throw new GutenbergRequestException(
                    "Project Gutenberg returned HTTP "
                            + response.statusCode()
                            + " for book " + bookId
            );
        }

        String text = response.body();

        int start = text.indexOf(START_MARKER);
        int end = text.indexOf(END_MARKER);

        if (start < 0 || end < 0 || end <= start) {
            throw new BookNotFoundException(
                    "Book " + bookId
                            + " was not found or Gutenberg markers are missing"
            );
        }

        String header = text.substring(0, start).strip();

        String body = text.substring(
                start + START_MARKER.length(),
                end
        ).strip();

        return new BookText(header, body);
    }

    public record BookText(String header, String body) {}
}
