
package org.ulpgc.bigdata.control.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IngestionClientTest {

    @Test
    void shouldSendPostRequestToIngestBook()
            throws Exception {

        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> expectedResponse = mock(HttpResponse.class);

        when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
        )).thenReturn(expectedResponse);

        IngestionClient ingestionClient =
                new IngestionClient(
                        httpClient,
                        "http://localhost:7001"
                );

        HttpResponse<String> actualResponse =
                ingestionClient.ingestBook(1342);

        assertSame(expectedResponse, actualResponse);

        verify(httpClient).send(
                argThat(request ->
                        request.uri().toString().equals(
                                "http://localhost:7001/ingest/1342"
                        )
                                && request.method().equals("POST")
                ),
                any(HttpResponse.BodyHandler.class)
        );
    }

    @Test
    void shouldSendGetRequestToRetrieveBookContent()
            throws Exception {

        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> expectedResponse = mock(HttpResponse.class);

        when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
        )).thenReturn(expectedResponse);

        IngestionClient ingestionClient =
                new IngestionClient(
                        httpClient,
                        "http://localhost:7001"
                );

        HttpResponse<String> actualResponse =
                ingestionClient.getBookContent(1342);

        assertSame(expectedResponse, actualResponse);

        verify(httpClient).send(
                argThat(request ->
                        request.uri().toString().equals(
                                "http://localhost:7001/ingest/content/1342"
                        )
                                && request.method().equals("GET")
                ),
                any(HttpResponse.BodyHandler.class)
        );
    }
}
