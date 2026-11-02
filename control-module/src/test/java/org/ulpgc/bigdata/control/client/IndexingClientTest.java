
package org.ulpgc.bigdata.control.client;

import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IndexingClientTest {

    @Test
    void shouldSendPostRequestWithBookContent()
            throws Exception {

        // Arrange
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> expectedResponse = mock(HttpResponse.class);

        String content = "This is the content of book 1342.";

        when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
        )).thenReturn(expectedResponse);

        IndexingClient indexingClient =
                new IndexingClient(
                        httpClient,
                        "http://localhost:7002"
                );

        // Act
        HttpResponse<String> actualResponse =
                indexingClient.indexBook(1342, content);

        // Assert
        assertSame(expectedResponse, actualResponse);

        verify(httpClient).send(
                argThat(request ->
                        request.uri().toString().equals(
                                "http://localhost:7002/index/update/1342"
                        )
                                && request.method().equals("POST")
                                && request.headers().firstValue("Content-Type")
                                .orElse("")
                                .equals("text/plain")
                                && request.bodyPublisher().isPresent()
                ),
                any(HttpResponse.BodyHandler.class)
        );
    }

    @Test
    void shouldSendTheCorrectBookContent()
            throws Exception {

        // Arrange
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> expectedResponse = mock(HttpResponse.class);

        String content = "Hello world from book 1342.";

        when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
        )).thenReturn(expectedResponse);

        IndexingClient indexingClient =
                new IndexingClient(
                        httpClient,
                        "http://localhost:7002"
                );

        // Act
        indexingClient.indexBook(1342, content);

        // Assert
        verify(httpClient).send(
                argThat(request ->
                        request.bodyPublisher().isPresent()
                ),
                any(HttpResponse.BodyHandler.class)
        );
    }
}
