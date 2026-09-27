package com.kfels.shorturl.telegram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class ApiRequestHandlerTests {

    @SuppressWarnings("unchecked")
    private static <T> HttpResponse<T> mockResponse() {
        return (HttpResponse<T>) mock(HttpResponse.class);
    }

    @Test
    void encodesFormDataAndHandlesEmptyForm() {
        Map<String, String> formData = new LinkedHashMap<>();
        formData.put("chat id", "42");
        formData.put("text", "hello world & goodbye");

        assertEquals("chat+id=42&text=hello+world+%26+goodbye",
                ApiRequestHandler.getFormDataAsString(formData));
        assertEquals("", ApiRequestHandler.getFormDataAsString(Map.of()));
    }

    @Test
    void configuresConnectionWithoutOpeningNetworkConnection() throws Exception {
        HttpURLConnection connection = new ApiRequestHandler()
                .openConnection("http://127.0.0.1:1/telegram");

        assertEquals(20000, connection.getReadTimeout());
        assertEquals(20000, connection.getConnectTimeout());
        assertEquals("application/json", connection.getRequestProperty("Content-Type"));
        assertEquals("application/json", connection.getRequestProperty("Accept"));
        connection.disconnect();
    }

    @Test
    void rejectsMalformedConnectionUri() {
        ApiRequestHandler handler = new ApiRequestHandler();

        org.junit.jupiter.api.Assertions.assertThrows(java.net.URISyntaxException.class,
                () -> handler.openConnection("not a uri"));
    }

    @Test
    void getRequestReturnsSuccessfulBodyAndRejectsNonSuccessStatus() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<String> response = mockResponse();
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("telegram response");
        when(client.send(any(HttpRequest.class),
            org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(response);

        try (MockedStatic<HttpClient> factory = mockStatic(HttpClient.class)) {
            factory.when(HttpClient::newHttpClient).thenReturn(client);
            assertEquals("telegram response", ApiRequestHandler.getRequest("https://example.test/get"));
        }

        when(response.statusCode()).thenReturn(404);
        try (MockedStatic<HttpClient> factory = mockStatic(HttpClient.class)) {
            factory.when(HttpClient::newHttpClient).thenReturn(client);
            assertNull(ApiRequestHandler.getRequest("https://example.test/missing"));
        }
    }

    @Test
    void postRequestAcceptsStatusesBelow400AndRejectsErrors() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<String> response = mockResponse();
        when(response.statusCode()).thenReturn(399);
        when(response.body()).thenReturn("accepted");
        when(client.send(any(HttpRequest.class),
            org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(response);
        Map<String, String> form = Map.of("text", "hello world");

        try (MockedStatic<HttpClient> factory = mockStatic(HttpClient.class)) {
            factory.when(HttpClient::newHttpClient).thenReturn(client);
            assertEquals("accepted", ApiRequestHandler.postRequest("https://example.test/post", form));
        }

        when(response.statusCode()).thenReturn(400);
        try (MockedStatic<HttpClient> factory = mockStatic(HttpClient.class)) {
            factory.when(HttpClient::newHttpClient).thenReturn(client);
            assertNull(ApiRequestHandler.postRequest("https://example.test/post", form));
        }
    }

    @Test
    void downloadFileWritesResponseBytesAndReturnsFalseForInvalidUri() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<java.io.InputStream> response = mockResponse();
        when(response.body()).thenReturn(new ByteArrayInputStream(new byte[] { 1, 2, 3 }));
        when(client.send(any(HttpRequest.class),
            org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<java.io.InputStream>>any())).thenReturn(response);
        Path output = Files.createTempFile("telegram-download-", ".bin");

        try {
            try (MockedStatic<HttpClient> factory = mockStatic(HttpClient.class)) {
                factory.when(HttpClient::newHttpClient).thenReturn(client);
                assertTrue(ApiRequestHandler.downloadFile("https://example.test/file", output.toString()));
            }
            assertEquals(3, Files.size(output));
            assertFalse(ApiRequestHandler.downloadFile("not a uri", output.toString()));
        } finally {
            Files.deleteIfExists(output);
        }
    }
}
