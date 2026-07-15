package com.badereddine.demo.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ContainerConfigurationTest {

    @Test
    void backendImageUsesSeparateBuildAndNonRootRuntimeStages() throws IOException {
        String dockerfile = Files.readString(Path.of("Dockerfile"));

        assertThat(dockerfile)
                .contains(" AS build")
                .contains("FROM eclipse-temurin:17-jre-alpine AS runtime")
                .contains("USER app")
                .contains("ENTRYPOINT [\"java\", \"-jar\", \"/app/app.jar\"]");
    }

    @Test
    void composeRequiresExternalSecretsAndDefinesHealthyPersistentServices() throws IOException {
        String compose = Files.readString(Path.of("compose.yaml"));

        assertThat(compose)
                .contains("DB_PASSWORD: ${DB_PASSWORD:?")
                .contains("JWT_SECRET: ${JWT_SECRET:?")
                .contains("pg_isready")
                .contains("/actuator/health/readiness")
                .contains("condition: service_healthy")
                .contains("postgres-data:/var/lib/postgresql/data")
                .doesNotContain("DB_PASSWORD: postgres")
                .doesNotContain("JWT_SECRET: AAAAA");
    }

    @Test
    void dockerBuildContextExcludesLocalSecretsAndGeneratedArtifacts() throws IOException {
        String dockerignore = Files.readString(Path.of(".dockerignore"));

        assertThat(dockerignore)
                .contains(".env")
                .contains("**/target")
                .contains("users.json")
                .contains("users_export.csv");
    }
}
