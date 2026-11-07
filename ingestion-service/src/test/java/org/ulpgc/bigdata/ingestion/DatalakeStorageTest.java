package org.ulpgc.bigdata.ingestion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DatalakeStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldSaveHeaderAndBodyFiles() throws IOException {

        DatalakeStorage storage = new DatalakeStorage(
                tempDir,
                "by_book"
        );

        GutenbergClient.BookText bookText =
                new GutenbergClient.BookText(
                        "Book title: Pride and Prejudice",
                        "It is a truth universally acknowledged."
                );

        storage.save(1342, bookText);

        Path bookDirectory = tempDir
                .resolve("by_book")
                .resolve("1342");

        Path headerFile = bookDirectory.resolve("1342.header.txt");
        Path bodyFile = bookDirectory.resolve("1342.body.txt");

        assertTrue(Files.exists(headerFile));
        assertTrue(Files.exists(bodyFile));

        assertEquals(
                "Book title: Pride and Prejudice",
                Files.readString(headerFile)
        );

        assertEquals(
                "It is a truth universally acknowledged.",
                Files.readString(bodyFile)
        );
    }

    @Test
    void shouldSaveBookUsingBatchStrategy() throws IOException {

        DatalakeStorage storage = new DatalakeStorage(
                tempDir,
                "by_batch"
        );

        GutenbergClient.BookText bookText =
                new GutenbergClient.BookText(
                        "Book header",
                        "Book body"
                );

        storage.save(1342, bookText);

        Path batchDirectory = tempDir
                .resolve("by_batch")
                .resolve("1000-1999");

        assertTrue(Files.exists(
                batchDirectory.resolve("1342.header.txt")
        ));

        assertTrue(Files.exists(
                batchDirectory.resolve("1342.body.txt")
        ));
    }

    @Test
    void shouldSaveBookUsingTimeStrategy() throws IOException {

        DatalakeStorage storage = new DatalakeStorage(
                tempDir,
                "by_time"
        );

        GutenbergClient.BookText bookText =
                new GutenbergClient.BookText(
                        "Book header",
                        "Book body"
                );

        storage.save(1342, bookText);

        Path timeDirectory = tempDir.resolve("by_time");

        assertTrue(Files.exists(timeDirectory));
        assertTrue(Files.isDirectory(timeDirectory));

        try (var dates = Files.list(timeDirectory)) {
            Path dateDirectory = dates.findFirst().orElseThrow();

            assertTrue(Files.isDirectory(dateDirectory));

            try (var hours = Files.list(dateDirectory)) {
                Path hourDirectory = hours.findFirst().orElseThrow();

                assertTrue(Files.isDirectory(hourDirectory));

                assertTrue(Files.exists(
                        hourDirectory.resolve("1342.header.txt")
                ));

                assertTrue(Files.exists(
                        hourDirectory.resolve("1342.body.txt")
                ));
            }
        }
    }

    @Test
    void shouldRejectUnknownStorageStrategy() {

        DatalakeStorage storage = new DatalakeStorage(
                tempDir,
                "unknown"
        );

        GutenbergClient.BookText bookText =
                new GutenbergClient.BookText(
                        "Book header",
                        "Book body"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> storage.save(1342, bookText)
        );
    }


    @Test
    void shouldReplaceExistingBookFilesWhenSavingAgain()
            throws IOException {

        DatalakeStorage storage = new DatalakeStorage(
                tempDir,
                "by_book"
        );

        GutenbergClient.BookText firstBook =
                new GutenbergClient.BookText(
                        "Original header",
                        "Original body"
                );

        GutenbergClient.BookText updatedBook =
                new GutenbergClient.BookText(
                        "Updated header",
                        "Updated body"
                );

        storage.save(1342, firstBook);

        storage.save(1342, updatedBook);

        Path bookDirectory = tempDir
                .resolve("by_book")
                .resolve("1342");

        Path headerFile = bookDirectory.resolve("1342.header.txt");
        Path bodyFile = bookDirectory.resolve("1342.body.txt");

        assertEquals("Updated header", Files.readString(headerFile));
        assertEquals("Updated body", Files.readString(bodyFile));

        try (var files = Files.list(bookDirectory)) {
            assertEquals(
                    2,
                    files.count(),
                    "Only the header and body files should remain"
            );
        }
    }

}
