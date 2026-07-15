package com.badereddine.demo.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ContinuousIntegrationConfigurationTest {

    @Test
    void workflowVerifiesEveryPushAndPullRequestWithReadOnlyPermissions() throws IOException {
        String workflow = Files.readString(Path.of(".github", "workflows", "ci.yml"));

        assertThat(workflow)
                .contains("push:")
                .contains("pull_request:")
                .contains("permissions:\n  contents: read")
                .contains("runs-on: ubuntu-latest")
                .contains("timeout-minutes:")
                .doesNotContain("contents: write")
                .doesNotContain("pull-requests: write");
    }

    @Test
    void workflowInstallsJavaCachesBothModulesAndRunsRepositoryVerifierWithDocker() throws IOException {
        String workflow = Files.readString(Path.of(".github", "workflows", "ci.yml"));

        assertThat(workflow)
                .contains("actions/checkout@v5")
                .contains("actions/setup-java@v5")
                .contains("distribution: temurin")
                .contains("java-version: \"17\"")
                .contains("cache: maven")
                .contains("pom.xml")
                .contains("javafx-client/pom.xml")
                .contains("docker info")
                .contains("shell: pwsh")
                .contains("./scripts/verify.ps1");
    }

    @Test
    void dependabotUpdatesBothMavenModulesAndGitHubActionsSeparately() throws IOException {
        String dependabot = Files.readString(Path.of(".github", "dependabot.yml"));

        assertThat(dependabot)
                .contains("version: 2")
                .contains("package-ecosystem: maven")
                .contains("directory: \"/\"")
                .contains("directory: \"/javafx-client\"")
                .contains("package-ecosystem: github-actions")
                .contains("interval: weekly");
    }

    @Test
    void repositoryVerifierSelectsThePlatformMavenWrapper() throws IOException {
        String verifier = Files.readString(Path.of("scripts", "verify.ps1"));

        assertThat(verifier)
                .contains("System.PlatformID]::Win32NT")
                .contains("\"mvnw.cmd\"")
                .contains("\"mvnw\"")
                .contains("& bash $MavenWrapperPath @MavenArguments");
    }
}
