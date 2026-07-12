package com.badereddine.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequiredEnvironmentInitializerTest {

    private final RequiredEnvironmentInitializer initializer = new RequiredEnvironmentInitializer();

    @Test
    void missingRuntimeConfigurationFailsWithVariableNamesOnly() {
        GenericApplicationContext context = contextWith(new MockEnvironment());

        assertThatThrownBy(() -> initializer.initialize(context))
                .isInstanceOf(org.springframework.context.ApplicationContextException.class)
                .hasMessageContaining("DB_URL")
                .hasMessageContaining("DB_USERNAME")
                .hasMessageContaining("DB_PASSWORD")
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void externallySuppliedRuntimeConfigurationPassesValidation() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("DB_URL", "jdbc:postgresql://database.example/test")
                .withProperty("DB_USERNAME", UUID.randomUUID().toString())
                .withProperty("DB_PASSWORD", UUID.randomUUID().toString())
                .withProperty("JWT_SECRET", UUID.randomUUID().toString());

        assertThatCode(() -> initializer.initialize(contextWith(environment)))
                .doesNotThrowAnyException();
    }

    @Test
    void testProfileUsesItsIsolatedConfiguration() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");

        assertThatCode(() -> initializer.initialize(contextWith(environment)))
                .doesNotThrowAnyException();
    }

    private GenericApplicationContext contextWith(MockEnvironment environment) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.setEnvironment(environment);
        return context;
    }
}
