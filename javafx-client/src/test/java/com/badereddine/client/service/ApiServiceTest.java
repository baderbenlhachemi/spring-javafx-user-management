package com.badereddine.client.service;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiServiceTest {

    private MockWebServer server;
    private OkHttpClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        client = new OkHttpClient();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void consumesResponseFromMockWebServer() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"status\":\"ok\"}"));

        Request request = new Request.Builder()
                .url(server.url("/smoke-test"))
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(200, response.code());
            assertNotNull(response.body());
            assertEquals("{\"status\":\"ok\"}", response.body().string());
        }

        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertEquals("/smoke-test", recordedRequest.getPath());
    }
}
