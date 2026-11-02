
package org.ulpgc.bigdata.ingestion;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IngestionControllerTest {

    @TempDir
    Path tempDir;

    private Javalin app;
    private IngestionRepository repository;

    @BeforeEach
    void setUp() throws IOException {

        GutenbergClient gutenbergClient = new GutenbergClient();

        DatalakeStorage storage = new DatalakeStorage(
                tempDir.resolve("datalake"),
                "by_book"
        );

        repository = new IngestionRepository(
                tempDir.resolve("ingested_books.json")
        );

        IngestionController controller = new IngestionController(
                gutenbergClient,
                storage,
                repository
        );

        Gson gson = new Gson();

        app = Javalin.create(config -> {
            config.jsonMapper(new io.javalin.json.JavalinGson(gson, true));
        });

        controller.registerRoutes(app);
        app.start(0);
    }

    @AfterEach
    void tearDown() {
        app.stop();
    }

    @Test
    void shouldReturnBookAvailability() throws Exception {

        repository.markAvailable(1342);

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:" + app.port()
                                + "/ingest/status/1342"
                ))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, response.statusCode());

        JsonObject json = JsonParser.parseString(response.body())
                .getAsJsonObject();

        assertEquals(1342, json.get("book_id").getAsInt());
        assertEquals("available", json.get("status").getAsString());
    }

    @Test
    void shouldReturnUnavailableBook() throws Exception {

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:" + app.port()
                                + "/ingest/status/9999"
                ))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, response.statusCode());

        JsonObject json = JsonParser.parseString(response.body())
                .getAsJsonObject();

        assertEquals(9999, json.get("book_id").getAsInt());
        assertEquals("not_available", json.get("status").getAsString());
    }

    @Test
    void shouldReturnListOfIngestedBooks() throws Exception {

        repository.markAvailable(1342);
        repository.markAvailable(84);

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:" + app.port()
                                + "/ingest/list"
                ))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, response.statusCode());

        JsonObject json = JsonParser.parseString(response.body())
                .getAsJsonObject();

        assertEquals(2, json.get("count").getAsInt());

        assertTrue(json.getAsJsonArray("books")
                .toString().contains("1342"));

        assertTrue(json.getAsJsonArray("books")
                .toString().contains("84"));
    }

    @Test
    void shouldIngestBookSuccessfully() throws Exception {

        // Create a mock Gutenberg client
        GutenbergClient gutenbergClient = mock(GutenbergClient.class);

        GutenbergClient.BookText bookText =
                new GutenbergClient.BookText(
                        "Book header",
                        "Book body"
                );

        when(gutenbergClient.download(1342))
                .thenReturn(bookText);

        // Create storage and repository
        DatalakeStorage storage = new DatalakeStorage(
                tempDir.resolve("post-datalake"),
                "by_book"
        );

        IngestionRepository testRepository = new IngestionRepository(
                tempDir.resolve("post-books.json")
        );

        // Create a controller with the mocked client
        IngestionController controller = new IngestionController(
                gutenbergClient,
                storage,
                testRepository
        );

        Javalin testApp = Javalin.create(config -> {
            Gson gson = new Gson();
            config.jsonMapper(new io.javalin.json.JavalinGson(gson, true));
        });

        controller.registerRoutes(testApp);
        testApp.start(0);

        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "http://localhost:" + testApp.port()
                                    + "/ingest/1342"
                    ))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            assertEquals(200, response.statusCode());

            JsonObject json = JsonParser.parseString(response.body())
                    .getAsJsonObject();

            assertEquals(1342, json.get("book_id").getAsInt());
            assertEquals("downloaded", json.get("status").getAsString());
            assertEquals("by_book/1342", json.get("path").getAsString());

            // Verify that the book was stored
            assertTrue(testRepository.isAvailable(1342));

            // Verify that both files were created
            Path bookDirectory = tempDir
                    .resolve("post-datalake")
                    .resolve("by_book")
                    .resolve("1342");

            assertTrue(Files.exists(
                    bookDirectory.resolve("1342.header.txt")
            ));

            assertTrue(Files.exists(
                    bookDirectory.resolve("1342.body.txt")
            ));

            verify(gutenbergClient).download(1342);

        } finally {
            testApp.stop();
        }
    }

    @Test
    void shouldReturn404WhenBookDoesNotExist() throws Exception {

        GutenbergClient gutenbergClient = mock(GutenbergClient.class);

        when(gutenbergClient.download(999999))
                .thenThrow(new BookNotFoundException(
                        "Book not found"
                ));

        DatalakeStorage storage = new DatalakeStorage(
                tempDir.resolve("error-datalake"),
                "by_book"
        );

        IngestionRepository testRepository = new IngestionRepository(
                tempDir.resolve("error-books.json")
        );

        IngestionController controller = new IngestionController(
                gutenbergClient,
                storage,
                testRepository
        );

        Javalin testApp = Javalin.create(config -> {
            Gson gson = new Gson();
            config.jsonMapper(new io.javalin.json.JavalinGson(gson, true));
        });

        controller.registerRoutes(testApp);
        testApp.start(0);

        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "http://localhost:" + testApp.port()
                                    + "/ingest/999999"
                    ))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            assertEquals(404, response.statusCode());
            assertTrue(response.body().contains("Book not found"));

            assertFalse(testRepository.isAvailable(999999));

        } finally {
            testApp.stop();
        }
    }

    @Test
    void shouldReturn502WhenGutenbergReturnsServerError() throws Exception {

        GutenbergClient gutenbergClient = mock(GutenbergClient.class);

        when(gutenbergClient.download(1342))
                .thenThrow(new GutenbergRequestException(
                        "Project Gutenberg returned HTTP 503 for book 1342"
                ));

        DatalakeStorage storage = new DatalakeStorage(
                tempDir.resolve("server-error-datalake"),
                "by_book"
        );

        IngestionRepository testRepository = new IngestionRepository(
                tempDir.resolve("server-error-books.json")
        );

        IngestionController controller = new IngestionController(
                gutenbergClient,
                storage,
                testRepository
        );

        Javalin testApp = Javalin.create(config -> {
            Gson gson = new Gson();
            config.jsonMapper(new io.javalin.json.JavalinGson(gson, true));
        });

        controller.registerRoutes(testApp);
        testApp.start(0);

        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "http://localhost:" + testApp.port()
                                    + "/ingest/1342"
                    ))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            assertEquals(502, response.statusCode());
            assertTrue(response.body().contains(
                    "Error communicating with Project Gutenberg"
            ));

            assertFalse(testRepository.isAvailable(1342));

        } finally {
            testApp.stop();
        }
    }

    @Test
    void shouldReturn400WhenBookIdIsNotANumber() throws Exception {

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:" + app.port()
                                + "/ingest/abc"
                ))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Invalid book ID"));
    }

    @Test
    void shouldReturn400WhenBookIdIsZero() throws Exception {

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:" + app.port()
                                + "/ingest/0"
                ))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains(
                "Book ID must be a positive number"
        ));
    }

    @Test
    void shouldReturn400WhenBookIdIsNegative() throws Exception {

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:" + app.port()
                                + "/ingest/-5"
                ))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains(
                "Book ID must be a positive number"
        ));
    }
}
