
package org.ulpgc.bigdata.ingestion;

import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GutenbergClientTest {

    @Test
    void shouldDownloadAndSeparateBookText()
            throws Exception {

        String text = """
                Book title: Pride and Prejudice
                Author: Jane Austen
                *** START OF THE PROJECT GUTENBERG EBOOK
                It is a truth universally acknowledged.
                *** END OF THE PROJECT GUTENBERG EBOOK
                """;

        HttpClient httpClient = mock(HttpClient.class);

        HttpResponse<String> response = mock(HttpResponse.class);

        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(text);

        when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
        )).thenReturn(response);

        GutenbergClient client = new GutenbergClient(httpClient);

        GutenbergClient.BookText result = client.download(1342);

        assertTrue(result.header().contains("Pride and Prejudice"));
        assertTrue(result.body().contains(
                "It is a truth universally acknowledged."
        ));
    }

    @Test
    void shouldThrowExceptionWhenBookDoesNotExist()
            throws Exception {

        HttpClient httpClient = mock(HttpClient.class);

        HttpResponse<String> response = mock(HttpResponse.class);

        when(response.statusCode()).thenReturn(404);

        when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
        )).thenReturn(response);

        GutenbergClient client = new GutenbergClient(httpClient);

        assertThrows(
                BookNotFoundException.class,
                () -> client.download(999999999)
        );
    }

    @Test
    void shouldThrowExceptionWhenGutenbergReturnsServerError()
            throws Exception {

        HttpClient httpClient = mock(HttpClient.class);

        HttpResponse<String> response = mock(HttpResponse.class);

        when(response.statusCode()).thenReturn(500);

        when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
        )).thenReturn(response);

        GutenbergClient client = new GutenbergClient(httpClient);

        assertThrows(
                GutenbergRequestException.class,
                () -> client.download(1342)
        );
    }

    @Test
    void shouldThrowExceptionWhenMarkersAreMissing()
            throws Exception {

        HttpClient httpClient = mock(HttpClient.class);

        HttpResponse<String> response = mock(HttpResponse.class);

        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(
                "This text does not contain Gutenberg markers."
        );

        when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
        )).thenReturn(response);

        GutenbergClient client = new GutenbergClient(httpClient);

        assertThrows(
                BookNotFoundException.class,
                () -> client.download(1342)
        );
    }
}
