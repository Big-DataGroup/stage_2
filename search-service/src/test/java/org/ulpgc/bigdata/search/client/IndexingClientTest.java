
package org.ulpgc.bigdata.search.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IndexingClientTest {

    private HttpClient httpClient;
    private IndexingClient indexingClient;

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        indexingClient = new IndexingClient(
                httpClient,
                "http://localhost:7002"
        );
    }

    @Test
    void shouldSendGetRequestToIndexingService() throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);

        when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
        )).thenReturn(response);

        indexingClient.search("pride");

        verify(httpClient).send(
                argThat(request ->
                        request.method().equals("GET")
                                && request.uri().equals(
                                URI.create(
                                        "http://localhost:7002/index/search/pride"
                                )
                        )
                ),
                any(HttpResponse.BodyHandler.class)
        );
    }

    @Test
    void shouldEncodeSpecialCharactersInWord() throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);

        when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
        )).thenReturn(response);

        indexingClient.search("hello world");

        verify(httpClient).send(
                argThat(request ->
                        request.uri().getRawPath().equals(
                                "/index/search/hello%20world"
                        )
                ),
                any(HttpResponse.BodyHandler.class)
        );
    }

    @Test
    void shouldRejectEmptyWord() {
        assertThrows(
                IllegalArgumentException.class,
                () -> indexingClient.search(" ")
        );

        verifyNoInteractions(httpClient);
    }

    @Test
    void shouldPropagateCommunicationErrors() throws Exception {
        when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
        )).thenThrow(new IOException("Connection refused"));

        assertThrows(
                IOException.class,
                () -> indexingClient.search("pride")
        );
    }
}
