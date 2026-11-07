package org.ulpgc.bigdata.indexing;

import com.google.gson.Gson;
import io.javalin.Javalin;

import java.io.IOException;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        IndexManager indexManager = new IndexManager();
        int port = Integer.parseInt(System.getenv().getOrDefault("INDEXING_PORT", "7002"));

        Gson gson = new Gson();
        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
            config.jsonMapper(new io.javalin.json.JavalinGson(gson, true));
        });

        app.get("/health", ctx -> ctx.json(Map.of(
                "service", "indexing-service", "status", "running")));

        app.post("/index/update/{book_id}", ctx -> {
            try {
                int bookId = Integer.parseInt(ctx.pathParam("book_id"));
                indexManager.indexText(bookId, ctx.body());
                ctx.json(Map.of("status", "updated", "book_id", bookId));
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(Map.of("error", e.getMessage()));
            } catch (IOException e) {
                ctx.status(500).json(Map.of("error", "Could not persist index state"));
            }
        });

        app.get("/index/search", ctx -> {
            String query = ctx.queryParam("q");
            Integer year = parseInteger(ctx.queryParam("year"));
            Integer offset = parseInteger(ctx.queryParam("offset"));
            Integer limit = parseInteger(ctx.queryParam("limit"));
            if (query == null || query.isBlank()
                    || (ctx.queryParam("year") != null && year == null)
                    || (ctx.queryParam("offset") != null && offset == null)
                    || (ctx.queryParam("limit") != null && limit == null)
                    || (offset != null && offset < 0)
                    || (limit != null && (limit < 1 || limit > 100))
                    || (year != null && (year < 1 || year > 3000))) {
                ctx.status(400).json(Map.of("error", "Invalid search parameters"));
                return;
            }
            try {
                ctx.json(indexManager.search(query, ctx.queryParam("author"),
                        ctx.queryParam("language"), year,
                        offset == null ? 0 : offset, limit == null ? 20 : limit));
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(Map.of("error", e.getMessage()));
            }
        });

        app.get("/index/search/{word}", ctx -> ctx.json(
                indexManager.search(ctx.pathParam("word"), null, null, null, 0, 20)));
        app.get("/index/status", ctx -> ctx.json(indexManager.status()));
        app.post("/index/rebuild", ctx -> {
            try {
                indexManager.rebuild();
                ctx.json(Map.of("books_processed", 0, "status", "rebuilt"));
            } catch (IOException e) {
                ctx.status(500).json(Map.of("error", "Could not rebuild index"));
            }
        });

        app.start(port);
    }

    private static Integer parseInteger(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
