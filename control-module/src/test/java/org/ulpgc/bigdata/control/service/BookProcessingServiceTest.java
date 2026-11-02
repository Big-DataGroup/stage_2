
package org.ulpgc.bigdata.control.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ulpgc.bigdata.control.client.IngestionClient;
import org.ulpgc.bigdata.control.client.IndexingClient;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookProcessingServiceTest {

    private IngestionClient ingestionClient;
    private IndexingClient indexingClient;
    private BookProcessingService bookProcessingService;

    @BeforeEach
    void setUp() {
        ingestionClient = mock(IngestionClient.class);
        indexingClient = mock(IndexingClient.class);

        bookProcessingService = new BookProcessingService(
                ingestionClient,
                indexingClient
        );
    }

    @Test
    void shouldProcessBookSuccessfully() throws Exception {

        // Arrange
        HttpResponse<String> ingestionResponse = mock(HttpResponse.class);
        HttpResponse<String> contentResponse = mock(HttpResponse.class);
        HttpResponse<String> indexingResponse = mock(HttpResponse.class);

        when(ingestionResponse.statusCode()).thenReturn(200);

        when(contentResponse.statusCode()).thenReturn(200);
        when(contentResponse.body()).thenReturn(
                "{\"book_id\":1342,\"header\":\"Book header\",\"body\":\"Book body\"}"
        );

        when(indexingResponse.statusCode()).thenReturn(200);

        when(ingestionClient.ingestBook(1342))
                .thenReturn(ingestionResponse);

        when(ingestionClient.getBookContent(1342))
                .thenReturn(contentResponse);

        when(indexingClient.indexBook(
                1342,
                "Book header\nBook body"
        )).thenReturn(indexingResponse);

        // Act
        HttpResponse<String> result =
                bookProcessingService.processBook(1342);

        // Assert
        assertSame(indexingResponse, result);

        verify(ingestionClient).ingestBook(1342);
        verify(ingestionClient).getBookContent(1342);

        verify(indexingClient).indexBook(
                1342,
                "Book header\nBook body"
        );
    }

    @Test
    void shouldRejectInvalidBookId() {

        // Act + Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> bookProcessingService.processBook(-5)
        );

        verifyNoInteractions(ingestionClient, indexingClient);
    }

    @Test
    void shouldStopWhenIngestionFails() throws Exception {

        // Arrange
        HttpResponse<String> failedResponse = mock(HttpResponse.class);

        when(failedResponse.statusCode()).thenReturn(404);

        when(ingestionClient.ingestBook(1342))
                .thenReturn(failedResponse);

        // Act + Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> bookProcessingService.processBook(1342)
        );

        assertEquals(
                "Ingestion failed with status: 404",
                exception.getMessage()
        );

        verify(ingestionClient).ingestBook(1342);
        verify(ingestionClient, never()).getBookContent(anyInt());
        verifyNoInteractions(indexingClient);
    }

    @Test
    void shouldStopWhenContentRetrievalFails() throws Exception {

        // Arrange
        HttpResponse<String> ingestionResponse = mock(HttpResponse.class);
        HttpResponse<String> failedContentResponse = mock(HttpResponse.class);

        when(ingestionResponse.statusCode()).thenReturn(200);
        when(failedContentResponse.statusCode()).thenReturn(404);

        when(ingestionClient.ingestBook(1342))
                .thenReturn(ingestionResponse);

        when(ingestionClient.getBookContent(1342))
                .thenReturn(failedContentResponse);

        // Act + Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> bookProcessingService.processBook(1342)
        );

        assertEquals(
                "Could not retrieve book content. Status: 404",
                exception.getMessage()
        );

        verifyNoInteractions(indexingClient);
    }

    @Test
    void shouldStopWhenIndexingFails() throws Exception {

        // Arrange
        HttpResponse<String> ingestionResponse = mock(HttpResponse.class);
        HttpResponse<String> contentResponse = mock(HttpResponse.class);
        HttpResponse<String> failedIndexingResponse = mock(HttpResponse.class);

        when(ingestionResponse.statusCode()).thenReturn(200);

        when(contentResponse.statusCode()).thenReturn(200);
        when(contentResponse.body()).thenReturn(
                "{\"book_id\":1342,\"header\":\"Header\",\"body\":\"Body\"}"
        );

        when(failedIndexingResponse.statusCode()).thenReturn(500);

        when(ingestionClient.ingestBook(1342))
                .thenReturn(ingestionResponse);

        when(ingestionClient.getBookContent(1342))
                .thenReturn(contentResponse);

        when(indexingClient.indexBook(1342, "Header\nBody"))
                .thenReturn(failedIndexingResponse);

        // Act + Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> bookProcessingService.processBook(1342)
        );

        assertEquals(
                "Indexing failed with status: 500",
                exception.getMessage()
        );

        verify(indexingClient).indexBook(1342, "Header\nBody");
    }
}
