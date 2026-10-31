package org.ulpgc.bigdata.ingestion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IngestionRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void newRepositoryShouldHaveNoBooks() throws IOException {

        Path file = tempDir.resolve("books.json");

        IngestionRepository repository =
                new IngestionRepository(file);

        assertTrue(repository.listBooks().isEmpty());
    }

    @Test
    void markAvailableShouldStoreBook() throws IOException {

        Path file = tempDir.resolve("books.json");

        IngestionRepository repository =
                new IngestionRepository(file);

        repository.markAvailable(1342);

        assertTrue(repository.isAvailable(1342));
    }

    @Test
    void repositoryShouldPersistBooks() throws IOException {

        Path file = tempDir.resolve("books.json");

        IngestionRepository repository =
                new IngestionRepository(file);

        repository.markAvailable(1342);

        IngestionRepository secondRepository =
                new IngestionRepository(file);

        assertTrue(secondRepository.isAvailable(1342));
    }

    @Test
    void listBooksShouldReturnAvailableBooks() throws IOException {

        Path file = tempDir.resolve("books.json");

        IngestionRepository repository =
                new IngestionRepository(file);

        repository.markAvailable(1342);
        repository.markAvailable(84);

        Map<Integer, String> books = repository.listBooks();

        assertEquals(2, books.size());
        assertEquals("AVAILABLE", books.get(1342));
        assertEquals("AVAILABLE", books.get(84));
    }


    @Test
    void listBooksShouldReturnIndependentCopy() throws IOException {

        Path file = tempDir.resolve("books.json");

        IngestionRepository repository =
                new IngestionRepository(file);

        repository.markAvailable(1342);

        Map<Integer, String> books = repository.listBooks();

        books.put(84, "AVAILABLE");
        books.remove(1342);

        assertTrue(repository.isAvailable(1342));
        assertFalse(repository.isAvailable(84));
    }





    @Test
    void markAvailableShouldRestoreStateWhenSaveFails()
            throws IOException {

        Path directory = tempDir.resolve("repository");
        java.nio.file.Files.createDirectories(directory);

        Path file = directory.resolve("books.json");

        IngestionRepository repository =
                new IngestionRepository(file);

        // Replace the parent directory with a regular file.
        java.nio.file.Files.delete(directory);
        java.nio.file.Files.writeString(directory, "not a directory");

        assertThrows(
                IOException.class,
                () -> repository.markAvailable(1342)
        );

        assertFalse(repository.isAvailable(1342));
    }
}
