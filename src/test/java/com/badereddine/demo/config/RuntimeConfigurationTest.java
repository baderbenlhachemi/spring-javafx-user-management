package com.badereddine.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.PropertyPlaceholderHelper;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuntimeConfigurationTest {

    private static final Map<String, String> REQUIRED_ENVIRONMENT_PROPERTIES = Map.of(
            "spring.datasource.url", "DB_URL",
            "spring.datasource.username", "DB_USERNAME",
            "spring.datasource.password", "DB_PASSWORD",
            "demo.jwtSecret", "JWT_SECRET"
    );

    @Test
    void runtimeCredentialsAreEnvironmentBacked() throws IOException {
        Properties properties = loadProperties("application.properties");

        REQUIRED_ENVIRONMENT_PROPERTIES.forEach((propertyName, environmentName) ->
                assertThat(properties.getProperty(propertyName))
                        .isEqualTo("${" + environmentName + "}"));
    }

    @Test
    void unresolvedRequiredEnvironmentValuesFailWithTheirNames() throws IOException {
        Properties properties = loadProperties("application.properties");
        PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("${", "}", ":", false);

        REQUIRED_ENVIRONMENT_PROPERTIES.forEach((propertyName, environmentName) ->
                assertThatThrownBy(() -> placeholderHelper.replacePlaceholders(
                        properties.getProperty(propertyName), new Properties()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining(environmentName));
    }

    @Test
    void testProfileProvidesIsolatedJwtConfiguration() throws IOException {
        Properties properties = loadProperties("application-test.properties");

        assertThat(properties.getProperty("demo.jwtSecret"))
                .startsWith("${TEST_JWT_SECRET:")
                .endsWith("}");
        assertThat(properties.getProperty("demo.jwtExpirationMs"))
                .startsWith("${TEST_JWT_EXPIRATION_MS:")
                .endsWith("}");
    }

    private Properties loadProperties(String resourceName) throws IOException {
        return PropertiesLoaderUtils.loadProperties(new ClassPathResource(resourceName));
    }
}
