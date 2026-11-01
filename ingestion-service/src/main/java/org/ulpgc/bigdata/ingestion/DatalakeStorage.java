
package org.ulpgc.bigdata.ingestion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.NoSuchFileException;
import java.util.Optional;
import java.util.stream.Stream;

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
            // Write both files to temporary locations first
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

            // Replace the final files only after both writes succeed
            moveFile(temporaryHeader, headerFile);
            moveFile(temporaryBody, bodyFile);

        } finally {
            // Remove temporary files if they still exist
            Files.deleteIfExists(temporaryHeader);
            Files.deleteIfExists(temporaryBody);
        }
    }


    public GutenbergClient.BookText read(int bookId) throws IOException {

        if (bookId <= 0) {
            throw new IllegalArgumentException(
                    "Book ID must be a positive number"
            );
        }

        Path bookDirectory;

        if ("by_time".equals(strategy)) {

            Path timeDirectory = rootDirectory.resolve("by_time");

            if (!Files.exists(timeDirectory)) {
                throw new NoSuchFileException(
                        "Book not found: " + bookId
                );
            }

            try (Stream<Path> directories = Files.walk(timeDirectory)) {

                Optional<Path> directory = directories
                        .filter(Files::isDirectory)
                        .filter(path ->
                                Files.exists(path.resolve(bookId + ".header.txt"))
                                        && Files.exists(path.resolve(bookId + ".body.txt"))
                        )
                        .findFirst();

                bookDirectory = directory.orElseThrow(
                        () -> new NoSuchFileException(
                                "Book not found: " + bookId
                        )
                );
            }

        } else {
            bookDirectory = getBookDirectory(bookId);
        }

        Path headerFile = bookDirectory.resolve(
                bookId + ".header.txt"
        );

        Path bodyFile = bookDirectory.resolve(
                bookId + ".body.txt"
        );

        if (!Files.exists(headerFile) || !Files.exists(bodyFile)) {
            throw new NoSuchFileException(
                    "Book not found: " + bookId
            );
        }

        String header = Files.readString(
                headerFile,
                StandardCharsets.UTF_8
        );

        String body = Files.readString(
                bodyFile,
                StandardCharsets.UTF_8
        );

        return new GutenbergClient.BookText(header, body);
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
