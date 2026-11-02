
package org.ulpgc.bigdata.search.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ulpgc.bigdata.search.client.IndexingClient;

import java.io.IOException;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SearchServiceTest {

    private IndexingClient indexingClient;
    private SearchService searchService;

    @BeforeEach
    void setUp() {
        indexingClient = mock(IndexingClient.class);
        searchService = new SearchService(indexingClient);
    }

    @Test
    void shouldReturnSearchResults() throws Exception {
        String json = "{\"word\":\"pride\",\"books\":[1342]}";

        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(json);

        when(indexingClient.search("pride")).thenReturn(response);

        String result = searchService.search("pride");

        assertEquals(json, result);
        verify(indexingClient).search("pride");
    }

    @Test
    void shouldRejectEmptySearchWord() {
        assertThrows(
                IllegalArgumentException.class,
                () -> searchService.search(" ")
        );

        verifyNoInteractions(indexingClient);
    }

    @Test
    void shouldRejectNonSuccessfulIndexingResponse() throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(500);

        when(indexingClient.search("pride")).thenReturn(response);

        assertThrows(
                IllegalStateException.class,
                () -> searchService.search("pride")
        );
    }

    @Test
    void shouldPropagateCommunicationErrors() throws Exception {
        when(indexingClient.search("pride"))
                .thenThrow(new IOException("Connection refused"));

        assertThrows(
                IOException.class,
                () -> searchService.search("pride")
        );
    }
}
