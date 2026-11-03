package org.ulpgc.bigdata.search;

import com.google.gson.Gson;
import io.javalin.Javalin;
import org.ulpgc.bigdata.search.service.SearchService;

import java.util.Map;

public class App {
    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("SEARCH_PORT", "7003"));
        SearchService service = new SearchService();
        Gson gson = new Gson();
        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
            config.jsonMapper(new io.javalin.json.JavalinGson(gson, true));
        });

        app.get("/health", ctx -> ctx.json(Map.of(
                "service", "search-service", "status", "running")));
        app.get("/search", ctx -> {
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
                ctx.result(service.search(query, ctx.queryParam("author"),
                        ctx.queryParam("language"), year,
                        offset == null ? 0 : offset, limit == null ? 20 : limit));
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(Map.of("error", e.getMessage()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ctx.status(503).json(Map.of("error", "Search interrupted"));
            } catch (Exception e) {
                ctx.status(502).json(Map.of("error", "Indexing Service unavailable"));
            }
        });
        app.get("/search/{word}", ctx -> ctx.redirect(
                "/search?q=" + java.net.URLEncoder.encode(
                        ctx.pathParam("word"), java.nio.charset.StandardCharsets.UTF_8)));
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
