
package org.ulpgc.bigdata.control;

import io.javalin.Javalin;
import org.ulpgc.bigdata.control.client.IngestionClient;
import org.ulpgc.bigdata.control.client.IndexingClient;
import org.ulpgc.bigdata.control.service.BookProcessingService;

public class App {

    public static void main(String[] args) {

        // Load the port from environment variables
        int port = Integer.parseInt(
                System.getenv().getOrDefault("CONTROL_PORT", "7000")
        );

        // Create HTTP clients
        IngestionClient ingestionClient = new IngestionClient();
        IndexingClient indexingClient = new IndexingClient();

        // Create the book processing service
        BookProcessingService bookProcessingService =
                new BookProcessingService(
                        ingestionClient,
                        indexingClient
                );

        // Create the HTTP server
        Javalin app = Javalin.create();

        // Health endpoint
        app.get("/health", ctx -> {
            ctx.status(200);
            ctx.result("Control Module is running");
        });

        // Process and index a book
        app.post("/books/{book_id}/ingest", ctx -> {

            try {
                int bookId = Integer.parseInt(
                        ctx.pathParam("book_id")
                );

                var response =
                        bookProcessingService.processBook(bookId);

                ctx.status(response.statusCode());
                ctx.result(response.body());

            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.result("Invalid book ID");

            } catch (IllegalArgumentException e) {
                ctx.status(400);
                ctx.result(e.getMessage());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ctx.status(500);
                ctx.result("Request was interrupted");

            } catch (Exception e) {
                ctx.status(502);
                ctx.result(
                        "Could not process and index the book"
                );
            }
        });

        // Start the server
        app.start(port);

        System.out.println(
                "Control Module running on port " + port
        );
    }
}
