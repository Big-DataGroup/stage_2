
package org.ulpgc.bigdata.control.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.ulpgc.bigdata.control.client.IngestionClient;
import org.ulpgc.bigdata.control.client.IndexingClient;

import java.io.IOException;
import java.net.http.HttpResponse;

public class BookProcessingService {

    private final IngestionClient ingestionClient;
    private final IndexingClient indexingClient;

    public BookProcessingService(
            IngestionClient ingestionClient,
            IndexingClient indexingClient
    ) {
        this.ingestionClient = ingestionClient;
        this.indexingClient = indexingClient;
    }

    public HttpResponse<String> processBook(int bookId)
            throws IOException, InterruptedException {

        if (bookId <= 0) {
            throw new IllegalArgumentException(
                    "Book ID must be a positive number"
            );
        }

        // Step 1: Download the book
        HttpResponse<String> ingestionResponse =
                ingestionClient.ingestBook(bookId);

        if (ingestionResponse.statusCode() != 200) {
            throw new RuntimeException(
                    "Ingestion failed with status: "
                            + ingestionResponse.statusCode()
            );
        }

        // Step 2: Retrieve the book content
        HttpResponse<String> contentResponse =
                ingestionClient.getBookContent(bookId);

        if (contentResponse.statusCode() != 200) {
            throw new RuntimeException(
                    "Could not retrieve book content. Status: "
                            + contentResponse.statusCode()
            );
        }

        // Parse the JSON response
        JsonObject bookContent = JsonParser.parseString(
                contentResponse.body()
        ).getAsJsonObject();

        String header = bookContent.get("header").getAsString();
        String body = bookContent.get("body").getAsString();

        // Combine header and body
        String completeContent = header + "\n" + body;

        // Step 3: Send the content to Indexing Service
        HttpResponse<String> indexingResponse =
                indexingClient.indexBook(bookId, completeContent);

        if (indexingResponse.statusCode() != 200) {
            throw new RuntimeException(
                    "Indexing failed with status: "
                            + indexingResponse.statusCode()
            );
        }

        return indexingResponse;
    }
}
