package org.ulpgc.bigdata.control.service;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.ulpgc.bigdata.control.client.IngestionClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IngestionServiceTest {

    @Test
    void shouldRejectInvalidBookId() {

        // Arrange
        IngestionClient ingestionClient = mock(IngestionClient.class);
        IngestionService ingestionService =
                new IngestionService(ingestionClient);

        // Act + Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> ingestionService.ingestBook(-5)
        );

        // Verify
        verifyNoInteractions(ingestionClient);
    }

    @Test
    void shouldReturnResponseWhenIngestionSucceeds() throws Exception {

        // Arrange
        IngestionClient ingestionClient = mock(IngestionClient.class);

        IngestionService ingestionService =
                new IngestionService(ingestionClient);

        HttpResponse<String> expectedResponse = mock(HttpResponse.class);

        when(expectedResponse.statusCode()).thenReturn(200);
        when(expectedResponse.body()).thenReturn(
                "{\"book_id\":1342,\"status\":\"downloaded\",\"path\":\"by_book/1342\"}"
        );

        when(ingestionClient.ingestBook(1342))
                .thenReturn(expectedResponse);

        // Act
        HttpResponse<String> actualResponse =
                ingestionService.ingestBook(1342);

        // Assert
        assertEquals(200, actualResponse.statusCode());
        assertEquals(expectedResponse.body(), actualResponse.body());

        verify(ingestionClient, times(1)).ingestBook(1342);
    }

    @Test
    void shouldThrowExceptionWhenIngestionFails() throws Exception {

        // Arrange
        IngestionClient ingestionClient = mock(IngestionClient.class);

        IngestionService ingestionService =
                new IngestionService(ingestionClient);

        HttpResponse<String> failedResponse = mock(HttpResponse.class);

        when(failedResponse.statusCode()).thenReturn(404);

        when(ingestionClient.ingestBook(1342))
                .thenReturn(failedResponse);

        // Act + Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> ingestionService.ingestBook(1342)
        );

        assertEquals(
                "Ingestion failed with status: 404",
                exception.getMessage()
        );

        verify(ingestionClient, times(1)).ingestBook(1342);
    }
}