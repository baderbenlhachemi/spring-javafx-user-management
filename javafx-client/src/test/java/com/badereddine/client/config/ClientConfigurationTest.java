package com.badereddine.client.config;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientConfigurationTest {

    @Test
    void usesCompatibleLocalDevelopmentDefault() {
        ClientConfiguration configuration = ClientConfiguration.load(new Properties(), Map.of());

        assertEquals("http://localhost:9090/api", configuration.apiBaseUrl());
    }

    @Test
    void readsApiBaseUrlFromEnvironment() {
        ClientConfiguration configuration = ClientConfiguration.load(
                new Properties(),
                Map.of(ClientConfiguration.API_BASE_URL_ENVIRONMENT_VARIABLE, "https://api.example.com/team-api/"));

        assertEquals("https://api.example.com/team-api", configuration.apiBaseUrl());
    }

    @Test
    void jvmPropertyTakesPrecedenceAndIsNormalized() {
        Properties properties = new Properties();
        properties.setProperty(ClientConfiguration.API_BASE_URL_PROPERTY, "  http://127.0.0.1:8181/api///  ");

        ClientConfiguration configuration = ClientConfiguration.load(
                properties,
                Map.of(ClientConfiguration.API_BASE_URL_ENVIRONMENT_VARIABLE, "https://api.example.com/api"));

        assertEquals("http://127.0.0.1:8181/api", configuration.apiBaseUrl());
    }

    @Test
    void blankJvmPropertyFallsBackToEnvironment() {
        Properties properties = new Properties();
        properties.setProperty(ClientConfiguration.API_BASE_URL_PROPERTY, "  ");

        ClientConfiguration configuration = ClientConfiguration.load(
                properties,
                Map.of(ClientConfiguration.API_BASE_URL_ENVIRONMENT_VARIABLE, "https://api.example.com/api"));

        assertEquals("https://api.example.com/api", configuration.apiBaseUrl());
    }

    @Test
    void rejectsInvalidApiBaseUrls() {
        String[] invalidValues = {
                "",
                "localhost:9090/api",
                "ftp://example.com/api",
                "http://",
                "https://example.com:70000/api",
                "https://user:password@example.com/api",
                "https://example.com/api?tenant=one",
                "https://example.com/api#section"
        };

        for (String invalidValue : invalidValues) {
            assertThrows(IllegalArgumentException.class,
                    () -> ClientConfiguration.withApiBaseUrl(invalidValue),
                    () -> "Expected invalid base URL to be rejected: " + invalidValue);
        }
    }
}
