package com.badereddine.client.api;

import com.badereddine.client.config.ClientConfiguration;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Objects;

/**
 * Injectable HTTP and JSON transport used by the desktop API service.
 */
public final class ApiClient {

    private final OkHttpClient httpClient;
    private final String baseUrl;
    private final Gson gson;

    public ApiClient(OkHttpClient httpClient, String baseUrl, Gson gson) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.baseUrl = ClientConfiguration.withApiBaseUrl(baseUrl).apiBaseUrl();
        this.gson = Objects.requireNonNull(gson, "gson must not be null");
    }

    public String url(String path) {
        if (path == null || !path.startsWith("/")) {
            throw new IllegalArgumentException("API path must start with '/'");
        }
        return baseUrl + path;
    }

    public String toJson(Object value) {
        return gson.toJson(value);
    }

    public <T> T fromJson(String json, Class<T> type) {
        return gson.fromJson(json, type);
    }

    public Response execute(Request request) throws IOException {
        return httpClient.newCall(request).execute();
    }
}
