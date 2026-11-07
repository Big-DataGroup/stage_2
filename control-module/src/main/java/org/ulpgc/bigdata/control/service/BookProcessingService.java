
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
    private final WorkflowStateStore workflowStateStore;

    public BookProcessingService(
            IngestionClient ingestionClient,
            IndexingClient indexingClient
    ) {
        this.ingestionClient = ingestionClient;
        this.indexingClient = indexingClient;
        try {
            this.workflowStateStore = new WorkflowStateStore();
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize workflow state", e);
        }
    }

    public HttpResponse<String> processBook(int bookId)
            throws IOException, InterruptedException {

        if (bookId <= 0) {
            throw new IllegalArgumentException(
                    "Book ID must be a positive number"
            );
        }
        try {
            workflowStateStore.mark(bookId, "INGESTING");

            HttpResponse<String> ingestionResponse =
                    ingestionClient.ingestBook(bookId);
            if (ingestionResponse.statusCode() != 200) {
                throw new RuntimeException(
                        "Ingestion failed with status: "
                                + ingestionResponse.statusCode());
            }
            workflowStateStore.mark(bookId, "INGESTED");

            HttpResponse<String> contentResponse =
                    ingestionClient.getBookContent(bookId);
            if (contentResponse.statusCode() != 200) {
                throw new RuntimeException(
                        "Could not retrieve book content. Status: "
                                + contentResponse.statusCode());
            }

            JsonObject bookContent = JsonParser.parseString(
                    contentResponse.body()).getAsJsonObject();
            String header = bookContent.get("header").getAsString();
            String body = bookContent.get("body").getAsString();
            String completeContent = header + "\n" + body;

            HttpResponse<String> indexingResponse =
                    indexingClient.indexBook(bookId, completeContent);
            if (indexingResponse.statusCode() != 200) {
                throw new RuntimeException(
                        "Indexing failed with status: "
                                + indexingResponse.statusCode());
            }
            workflowStateStore.mark(bookId, "INDEXED");
            return indexingResponse;
        } catch (IOException | InterruptedException | RuntimeException e) {
            try {
                workflowStateStore.mark(bookId, failureState(e));
            } catch (IOException stateError) {
                e.addSuppressed(stateError);
            }
            throw e;
        }
    }

    private String failureState(Exception error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) return "FAILED";
        return "FAILED: " + message;
    }
}
