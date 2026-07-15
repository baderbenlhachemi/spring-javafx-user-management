package com.badereddine.client.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PackagingConfigurationTest {

    private static final Path CLIENT_ROOT = locateClientRoot();

    @Test
    void packagesTheProductionLauncherAsANamedApplicationImage() throws IOException {
        String script = Files.readString(CLIENT_ROOT.resolve("package.ps1"));

        assertTrue(script.contains("\"--type\", \"app-image\""));
        assertTrue(script.contains("\"--name\", \"TeamAccessHub\""));
        assertTrue(script.contains("\"--main-jar\", \"team-access-hub-client.jar\""));
        assertTrue(script.contains("\"--main-class\", \"com.badereddine.client.Launcher\""));
        assertTrue(script.contains("jpackage failed with exit code"));
        assertTrue(script.contains("expected launcher"));
    }

    @Test
    void runsA_cleanClientTestBuildBeforePackaging() throws IOException {
        String script = Files.readString(CLIENT_ROOT.resolve("package.ps1"));

        int tests = script.indexOf("@(\"-f\", \"javafx-client/pom.xml\", \"clean\", \"test\")");
        int packageBuild = script.indexOf("@(\"-f\", \"javafx-client/pom.xml\", \"package\", \"-DskipTests\")");
        int jpackage = script.indexOf("& $JpackagePath @jpackageArguments");

        assertTrue(tests >= 0, "the packaging workflow must start with a clean client test build");
        assertTrue(packageBuild > tests, "packaging input must be prepared only after tests pass");
        assertTrue(jpackage > packageBuild, "jpackage must run only after the tested client is built");
    }

    @Test
    void documentsReproduciblePackagingAndRuntimeConfiguration() throws IOException {
        String readme = Files.readString(CLIENT_ROOT.resolve("README.md")).replaceAll("\\s+", " ");

        assertTrue(readme.contains("powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\\javafx-client\\package.ps1"));
        assertTrue(readme.contains("javafx-client/target/distribution/TeamAccessHub"));
        assertTrue(readme.contains("TEAM_ACCESS_HUB_API_BASE_URL"));
        assertTrue(readme.contains("platform-specific"));
        assertTrue(readme.contains("does not cross-compile"));
    }

    private static Path locateClientRoot() {
        Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        if (Files.isRegularFile(workingDirectory.resolve("package.ps1"))) {
            return workingDirectory;
        }
        return workingDirectory.resolve("javafx-client");
    }
}
