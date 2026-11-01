package org.ulpgc.bigdata.control.service;

import org.ulpgc.bigdata.control.client.IngestionClient;

import java.io.IOException;
import java.net.http.HttpResponse;

public class IngestionService {

    private final IngestionClient ingestionClient;

    public IngestionService(IngestionClient ingestionClient) {
        this.ingestionClient = ingestionClient;
    }

    public HttpResponse<String> ingestBook(int bookId)
            throws IOException, InterruptedException {

        if (bookId <= 0) {
            throw new IllegalArgumentException(
                    "Book ID must be a positive number"
            );
        }

        HttpResponse<String> response =
                ingestionClient.ingestBook(bookId);

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Ingestion failed with status: "
                            + response.statusCode()
            );
        }

        return response;
    }
}
