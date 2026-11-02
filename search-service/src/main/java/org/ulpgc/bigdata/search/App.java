
package org.ulpgc.bigdata.search;

import io.javalin.Javalin;
import org.ulpgc.bigdata.search.service.SearchService;

public class App {

    public static void main(String[] args) {

        int port = Integer.parseInt(
                System.getenv().getOrDefault("SEARCH_PORT", "7003")
        );

        SearchService searchService = new SearchService();

        Javalin app = Javalin.create();

        app.get("/health", ctx ->
                ctx.result("Search Service is running")
        );

        app.get("/search/{word}", ctx -> {

            String word = ctx.pathParam("word");

            try {
                String result = searchService.search(word);

                ctx.contentType("application/json");
                ctx.status(200);
                ctx.result(result);

            } catch (IllegalArgumentException e) {
                ctx.status(400);
                ctx.json(new ErrorResponse(e.getMessage()));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

                ctx.status(500);
                ctx.json(new ErrorResponse(
                        "Search request was interrupted"
                ));

            } catch (Exception e) {
                ctx.status(502);
                ctx.json(new ErrorResponse(
                        "Unable to communicate with Indexing Service"
                ));
            }
        });

        app.start(port);

        System.out.println(
                "Search Service running on port " + port
        );
    }

    private record ErrorResponse(String error) {}
}
