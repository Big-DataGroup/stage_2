
package org.ulpgc.bigdata.ingestion;

import io.javalin.Javalin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class IngestionController {

    private final GutenbergClient gutenbergClient;
    private final DatalakeStorage storage;
    private final IngestionRepository repository;

    public IngestionController(
            GutenbergClient gutenbergClient,
            DatalakeStorage storage,
            IngestionRepository repository
    ) {
        this.gutenbergClient = gutenbergClient;
        this.storage = storage;
        this.repository = repository;
    }

    public void registerRoutes(Javalin app) {

        // POST: Ingest a book
        app.post("/ingest/{book_id}", ctx -> {

            int bookId;

            try {
                bookId = Integer.parseInt(
                        ctx.pathParam("book_id")
                );

                if (bookId <= 0) {
                    ctx.status(400);
                    ctx.result("Book ID must be a positive number");
                    return;
                }

            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.result("Invalid book ID. It must be a number.");
                return;
            }

            try {
                if (!repository.isAvailable(bookId)) {

                    GutenbergClient.BookText book =
                            gutenbergClient.download(bookId);

                    storage.save(bookId, book);

                    repository.markAvailable(bookId);
                }

                ctx.status(200);
                ctx.json(Map.of(
                        "book_id", bookId,
                        "status", "downloaded",
                        "path", "by_book/" + bookId
                ));

            } catch (BookNotFoundException e) {
                ctx.status(404);
                ctx.result(e.getMessage());

            } catch (GutenbergRequestException e) {
                ctx.status(502);
                ctx.result(
                        "Error communicating with Project Gutenberg: "
                                + e.getMessage()
                );

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ctx.status(500);
                ctx.result("Book download was interrupted");

            } catch (IOException e) {
                ctx.status(500);
                ctx.result("Storage error: " + e.getMessage());
            }
        });

        // GET: Retrieve book content
        app.get("/ingest/content/{book_id}", ctx -> {

            int bookId;

            try {
                bookId = Integer.parseInt(
                        ctx.pathParam("book_id")
                );

                if (bookId <= 0) {
                    ctx.status(400);
                    ctx.result("Book ID must be a positive number");
                    return;
                }

            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.result("Invalid book ID. It must be a number.");
                return;
            }

            if (!repository.isAvailable(bookId)) {
                ctx.status(404);
                ctx.result("Book not found: " + bookId);
                return;
            }

            try {
                GutenbergClient.BookText book = storage.read(bookId);

                ctx.status(200);
                ctx.json(Map.of(
                        "book_id", bookId,
                        "header", book.header(),
                        "body", book.body()
                ));

            } catch (IOException e) {
                ctx.status(500);
                ctx.result(
                        "Error reading book content: " + e.getMessage()
                );
            }
        });

        // GET: Check book availability
        app.get("/ingest/status/{book_id}", ctx -> {

            int bookId;

            try {
                bookId = Integer.parseInt(
                        ctx.pathParam("book_id")
                );

                if (bookId <= 0) {
                    ctx.status(400);
                    ctx.result("Book ID must be a positive number");
                    return;
                }

            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.result("Invalid book ID. It must be a number.");
                return;
            }

            String status = repository.isAvailable(bookId)
                    ? "available"
                    : "not_available";

            ctx.json(Map.of(
                    "book_id", bookId,
                    "status", status
            ));
        });

        // GET: List ingested books
        app.get("/ingest/list", ctx -> {

            List<Integer> books = new ArrayList<>(
                    repository.listBooks().keySet()
            );

            Collections.sort(books);

            ctx.json(Map.of(
                    "count", books.size(),
                    "books", books
            ));
        });
    }
}
