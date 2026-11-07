
package org.ulpgc.bigdata.ingestion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class DatalakeStorage {

    private final Path rootDirectory;
    private final String strategy;

    public DatalakeStorage(Path rootDirectory, String strategy) {
        this.rootDirectory = rootDirectory;
        this.strategy = strategy;
    }

    public void save(int bookId, GutenbergClient.BookText bookText)
            throws IOException {

        Path bookDirectory = getBookDirectory(bookId);

        Files.createDirectories(bookDirectory);

        Path headerFile = bookDirectory.resolve(
                bookId + ".header.txt"
        );

        Path bodyFile = bookDirectory.resolve(
                bookId + ".body.txt"
        );

        Path temporaryHeader = Files.createTempFile(
                bookDirectory, bookId + "-", ".header.tmp"
        );

        Path temporaryBody = Files.createTempFile(
                bookDirectory, bookId + "-", ".body.tmp"
        );

        try {
            Files.writeString(
                    temporaryHeader,
                    bookText.header(),
                    StandardCharsets.UTF_8
            );

            Files.writeString(
                    temporaryBody,
                    bookText.body(),
                    StandardCharsets.UTF_8
            );

            moveFile(temporaryHeader, headerFile);
            moveFile(temporaryBody, bodyFile);

        } finally {
            Files.deleteIfExists(temporaryHeader);
            Files.deleteIfExists(temporaryBody);
        }
    }

    public Map<String, String> read(int bookId) throws IOException {

        Path bookDirectory = getBookDirectory(bookId);

        Path headerFile = bookDirectory.resolve(
                bookId + ".header.txt"
        );

        Path bodyFile = bookDirectory.resolve(
                bookId + ".body.txt"
        );

        if (!Files.exists(headerFile) || !Files.exists(bodyFile)) {
            throw new IOException("Book content not found: " + bookId);
        }

        String header = Files.readString(
                headerFile,
                StandardCharsets.UTF_8
        );

        String body = Files.readString(
                bodyFile,
                StandardCharsets.UTF_8
        );

        return Map.of(
                "header", header,
                "body", body
        );
    }

    private void moveFile(Path source, Path destination)
            throws IOException {

        try {
            Files.move(
                    source,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );

        } catch (AtomicMoveNotSupportedException e) {
            Files.move(
                    source,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    private Path getBookDirectory(int bookId) {

        return switch (strategy) {

            case "by_book" ->
                    rootDirectory.resolve("by_book")
                            .resolve(String.valueOf(bookId));

            case "by_batch" -> {
                int batchStart = (bookId / 1000) * 1000;
                int batchEnd = batchStart + 999;

                String batchName = batchStart + "-" + batchEnd;

                yield rootDirectory.resolve("by_batch")
                        .resolve(batchName);
            }

            case "by_time" -> {
                LocalDateTime now = LocalDateTime.now();

                String date = now.format(
                        DateTimeFormatter.ofPattern("yyyyMMdd")
                );

                String hour = now.format(
                        DateTimeFormatter.ofPattern("HH")
                );

                yield rootDirectory.resolve("by_time")
                        .resolve(date)
                        .resolve(hour);
            }

            default -> throw new IllegalArgumentException(
                    "Unknown storage strategy: " + strategy
            );
        };
    }
}
