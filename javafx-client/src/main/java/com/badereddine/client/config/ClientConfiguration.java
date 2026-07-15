package com.badereddine.client.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Resolves and validates configuration used by the desktop client.
 */
public final class ClientConfiguration {

    public static final String API_BASE_URL_PROPERTY = "teamaccesshub.api.base-url";
    public static final String API_BASE_URL_ENVIRONMENT_VARIABLE = "TEAM_ACCESS_HUB_API_BASE_URL";
    public static final String DEFAULT_API_BASE_URL = "http://localhost:9090/api";

    private final String apiBaseUrl;

    private ClientConfiguration(String apiBaseUrl) {
        this.apiBaseUrl = normalizeApiBaseUrl(apiBaseUrl);
    }

    public static ClientConfiguration load() {
        return load(System.getProperties(), System.getenv());
    }

    static ClientConfiguration load(Properties properties, Map<String, String> environment) {
        Objects.requireNonNull(properties, "properties must not be null");
        Objects.requireNonNull(environment, "environment must not be null");

        String propertyValue = properties.getProperty(API_BASE_URL_PROPERTY);
        if (hasText(propertyValue)) {
            return new ClientConfiguration(propertyValue);
        }

        String environmentValue = environment.get(API_BASE_URL_ENVIRONMENT_VARIABLE);
        if (hasText(environmentValue)) {
            return new ClientConfiguration(environmentValue);
        }

        return new ClientConfiguration(DEFAULT_API_BASE_URL);
    }

    public static ClientConfiguration withApiBaseUrl(String apiBaseUrl) {
        return new ClientConfiguration(apiBaseUrl);
    }

    public String apiBaseUrl() {
        return apiBaseUrl;
    }

    private static String normalizeApiBaseUrl(String value) {
        if (!hasText(value)) {
            throw invalidBaseUrl();
        }

        String candidate = value.trim();
        URI uri;
        try {
            uri = new URI(candidate);
        } catch (URISyntaxException exception) {
            throw invalidBaseUrl();
        }

        String scheme = uri.getScheme();
        boolean supportedScheme = scheme != null
                && (scheme.toLowerCase(Locale.ROOT).equals("http")
                || scheme.toLowerCase(Locale.ROOT).equals("https"));
        if (!supportedScheme
                || uri.getHost() == null
                || uri.getPort() > 65_535
                || uri.getRawUserInfo() != null
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null) {
            throw invalidBaseUrl();
        }

        while (candidate.endsWith("/")) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        return candidate;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static IllegalArgumentException invalidBaseUrl() {
        return new IllegalArgumentException(
                "API base URL must be an absolute HTTP or HTTPS URL without credentials, query parameters, or a fragment");
    }
}
