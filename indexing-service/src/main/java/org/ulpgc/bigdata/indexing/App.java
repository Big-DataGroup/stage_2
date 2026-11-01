package org.ulpgc.bigdata.indexing;

import com.google.gson.Gson;
import io.javalin.Javalin;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        // 1. Instanciar el núcleo que ya terminaste
        IndexManager indexManager = new IndexManager();
        Gson gson = new Gson();

        // 2. Levantar el servidor en el puerto acordado (ej. 7002)
        Javalin app = Javalin.create().start(7002);

        // ENDPOINT 1: Recibir texto de un libro e indexarlo (POST)
        app.post("/index/update/{book_id}", ctx -> {
            try {
                int bookId = Integer.parseInt(ctx.pathParam("book_id"));
                String content = ctx.body();

                if (content == null || content.isBlank()) {
                    ctx.status(400).result(gson.toJson(Map.of("status", "error", "message", "Body empty")));
                    return;
                }

                // Llamamos a tu clase IndexManager para que trabaje
                indexManager.indexText(bookId, content);

                ctx.status(200).result(gson.toJson(Map.of(
                        "status", "success",
                        "book_id", bookId
                )));
            } catch (NumberFormatException e) {
                ctx.status(400).result(gson.toJson(Map.of("status", "error")));
            }
        });

        // ENDPOINT 2: Buscar una palabra (GET)
        app.get("/index/search/{word}", ctx -> {
            String word = ctx.pathParam("word");
            var result = indexManager.getIndex().search(word);

            ctx.status(200).result(gson.toJson(Map.of(
                    "word", word,
                    "books", result
            )));
        });
    }
}