package com.badereddine.demo.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecureBaselineDocumentationTest {

    private static final Path REPOSITORY_ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Pattern MARKDOWN_LINK = Pattern.compile("!?\\[[^]]*]\\(([^)]+)\\)");

    @Test
    void rootReadmeExplainsTheRunnableSecureBaseline() throws IOException {
        String readme = read("readme.md");

        assertTrue(readme.startsWith("# Team Access Hub"));
        assertContainsAll(readme,
                "## Secure baseline scope",
                "## Architecture",
                "## Prerequisites",
                "## Configuration",
                "## Run locally with JavaFX",
                "## Container startup",
                "## JavaFX startup and packaging",
                "## Known limitations",
                ".\\scripts\\verify.ps1",
                "docker compose up -d postgres",
                "SPRING_PROFILES_ACTIVE=dev",
                "$devAdminPassword = Read-Host",
                "Invoke-RestMethod http://localhost:9090/actuator/health/readiness",
                "docker compose up --build -d",
                "..\\mvnw.cmd javafx:run",
                "powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\\javafx-client\\package.ps1");
    }

    @Test
    void endpointDocumentationMatchesMethodsAndRegistrationPolicy() throws IOException {
        String rootReadme = read("readme.md");
        String clientReadme = read("javafx-client/README.md");

        assertContainsAll(rootReadme,
                "| `POST` | `/api/auth` | Public login |",
                "| `POST` | `/api/auth/register` | Public only when registration policy is enabled; denied by default |",
                "| `PUT` | `/api/users/me/password` | Authenticated member |",
                "Swagger routes are denied by default");
        assertContainsAll(clientReadme,
                "| `POST` | `/api/auth/register` | Registration when server policy allows it |",
                "| `PUT` | `/api/users/me/password` | Change password |",
                "Registration is denied by the server's default policy");

        assertFalse(rootReadme.matches("(?s).*`POST`\\s*\\|\\s*`/api/users/me/password`.*"));
        assertFalse(clientReadme.matches("(?s).*`POST`\\s*\\|\\s*`/api/users/me/password`.*"));
    }

    @Test
    void publicGuidesDoNotAdvertiseRunnableDefaultCredentials() throws IOException {
        String publicGuides = String.join("\n",
                read("readme.md"),
                read("javafx-client/README.md"),
                read("docs/PROJECT_CONTEXT.md"))
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");

        assertFalse(publicGuides.contains("default login credentials"));
        assertFalse(publicGuides.contains("username `admin` and password"));
        assertFalse(publicGuides.contains("password | `admin`"));
        assertFalse(publicGuides.contains("password: admin"));
        assertFalse(publicGuides.contains("jwtsecret="));
        assertFalse(publicGuides.contains("demo_admin_password=local-admin"));
        assertTrue(publicGuides.contains("there is no documented or committed default login"));
    }

    @Test
    void precedingTasksAndRoadmapStatusAreTruthful() throws IOException {
        String plan = read("docs/IMPLEMENTATION_PLAN.md");
        String roadmap = read("docs/PORTFOLIO_ROADMAP.md");
        String context = read("docs/PROJECT_CONTEXT.md");

        for (String task : List.of(
                "1.1", "1.2", "1.3", "1.4", "1.5", "1.6", "1.7",
                "2.1", "2.2", "2.3", "2.4", "2.5", "2.6", "2.7", "2.8",
                "3.1", "3.2", "3.3", "3.4",
                "4.1", "4.2", "4.3", "4.4", "4.5", "4.6",
                "5.1", "5.2", "5.3", "5.4", "5.5", "5.6", "5.7")) {
            assertTrue(plan.contains("### [x] Task " + task + " "),
                    () -> "Expected completed plan status for Task " + task);
        }

        assertContainsAll(roadmap,
                "Phase 2 is the next product milestone and has not started",
                "Status: delivered by v0.2 for the existing feature set.",
                "Still planned within the broader engineering foundation:");
        assertContainsAll(context,
                "Last verified: 2026-07-15",
                "## Security baseline",
                "## Known limitations and risks");
    }

    @Test
    void localMarkdownLinksAndReferencedCommandsResolveFromTheRepository() throws IOException {
        boolean documentationIsIgnored = read(".gitignore").lines()
                .map(String::trim)
                .anyMatch("/docs/"::equals);
        assertFalse(documentationIsIgnored, "published documentation must be present in a clean checkout");

        for (String document : List.of(
                "readme.md",
                "javafx-client/README.md",
                "docs/PROJECT_CONTEXT.md",
                "docs/ARCHITECTURE.md",
                "docs/PORTFOLIO_ROADMAP.md")) {
            Path documentPath = REPOSITORY_ROOT.resolve(document);
            Matcher matcher = MARKDOWN_LINK.matcher(Files.readString(documentPath));
            while (matcher.find()) {
                String target = matcher.group(1).trim();
                if (target.startsWith("http://") || target.startsWith("https://") || target.startsWith("#")) {
                    continue;
                }

                String pathPart = target.split("#", 2)[0].replace("%20", " ");
                Path resolved = documentPath.getParent().resolve(pathPart).normalize();
                assertTrue(Files.exists(resolved),
                        () -> document + " contains a broken local link to " + target);
            }
        }

        for (String requiredPath : List.of(
                "scripts/verify.ps1",
                "compose.yaml",
                ".env.example",
                "mvnw.cmd",
                "javafx-client/pom.xml",
                "javafx-client/package.ps1")) {
            assertTrue(Files.exists(REPOSITORY_ROOT.resolve(requiredPath)),
                    () -> "Documented command dependency is missing: " + requiredPath);
        }
    }

    private static String read(String path) throws IOException {
        return Files.readString(REPOSITORY_ROOT.resolve(path));
    }

    private static void assertContainsAll(String value, String... expectedValues) {
        for (String expected : expectedValues) {
            assertTrue(value.contains(expected), () -> "Missing documentation text: " + expected);
        }
    }
}
