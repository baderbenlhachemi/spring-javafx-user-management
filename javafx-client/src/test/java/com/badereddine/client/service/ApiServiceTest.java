package com.badereddine.client.service;

import com.badereddine.client.model.AuthResponse;
import com.badereddine.client.model.User;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiServiceTest {

    private MockWebServer server;
    private OkHttpClient client;
    private ApiService apiService;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private ByteArrayOutputStream capturedOut;
    private ByteArrayOutputStream capturedErr;

    @BeforeEach
    void setUp() throws IOException {
        originalOut = System.out;
        originalErr = System.err;
        server = new MockWebServer();
        server.start(9090);
        client = new OkHttpClient();
        apiService = ApiService.getInstance();
        capturedOut = new ByteArrayOutputStream();
        capturedErr = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(capturedErr, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() throws IOException {
        System.setOut(originalOut);
        System.setErr(originalErr);
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

    @Test
    void successfulAuthenticationDoesNotWriteSensitiveDataToConsole() throws Exception {
        String token = "test-token-" + java.util.UUID.randomUUID();
        String password = java.util.UUID.randomUUID().toString();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "accessToken": "%s",
                          "tokenType": "Bearer",
                          "id": 1,
                          "username": "alice",
                          "email": "alice@example.com",
                          "roles": ["ROLE_USER"]
                        }
                        """.formatted(token)));

        ApiService.ApiResult<AuthResponse> result = apiService.authenticate("alice", password)
                .get(5, TimeUnit.SECONDS);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(token, result.getData().getToken());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertEquals("/api/auth", request.getPath());
        assertNoConsoleOutput();
    }

    @Test
    void successfulProfileRetrievalDoesNotWriteSensitiveDataToConsole() throws Exception {
        String token = "Bearer test-token-" + java.util.UUID.randomUUID();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "id": 1,
                          "firstName": "Alice",
                          "lastName": "Example",
                          "username": "alice",
                          "email": "alice@example.com",
                          "enabled": true,
                          "role": {"id": 1, "name": "ROLE_USER"}
                        }
                        """));

        ApiService.ApiResult<User> result = apiService.getMyProfile(token)
                .get(5, TimeUnit.SECONDS);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("alice", result.getData().getUsername());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("GET", request.getMethod());
        assertEquals("/api/users/me", request.getPath());
        assertEquals(token, request.getHeader("Authorization"));
        assertNoConsoleOutput();
    }

    private void assertNoConsoleOutput() {
        assertEquals("", capturedOut.toString(StandardCharsets.UTF_8));
        assertEquals("", capturedErr.toString(StandardCharsets.UTF_8));
    }
}
