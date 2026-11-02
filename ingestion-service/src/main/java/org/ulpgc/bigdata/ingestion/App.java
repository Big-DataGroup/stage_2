
package org.ulpgc.bigdata.ingestion;

import com.google.gson.Gson;
import io.javalin.Javalin;

import java.io.IOException;
import java.nio.file.Path;

public class App {

    public static void main(String[] args) throws IOException {

        // Load configuration from environment variables
        int port = Integer.parseInt(
                System.getenv().getOrDefault("INGESTION_PORT", "7001")
        );

        String datalakePath = System.getenv().getOrDefault(
                "INGESTION_DATALAKE_PATH",
                "data/datalake"
        );

        String storageStrategy = System.getenv().getOrDefault(
                "INGESTION_STORAGE_STRATEGY",
                "by_book"
        );

        String repositoryPath = System.getenv().getOrDefault(
                "INGESTION_REPOSITORY_PATH",
                "data/ingestion/ingested_books.json"
        );

        // Create the components
        GutenbergClient gutenbergClient = new GutenbergClient();

        DatalakeStorage storage = new DatalakeStorage(
                Path.of(datalakePath),
                storageStrategy
        );

        IngestionRepository repository = new IngestionRepository(
                Path.of(repositoryPath)
        );

        // Create the HTTP server
        Gson gson = new Gson();

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(
                    new io.javalin.json.JavalinGson(gson, true)
            );
        });

        // Register the health endpoint
        app.get("/health", ctx -> {
            ctx.status(200);
            ctx.result("Ingestion Service is running");
        });

        // Register ingestion endpoints
        IngestionController controller = new IngestionController(
                gutenbergClient,
                storage,
                repository
        );

        controller.registerRoutes(app);

        // Start the server
        app.start(port);

        System.out.println(
                "Ingestion Service running on port " + port
        );
    }
}
