package com.badereddine.demo.controller;

import com.badereddine.demo.payload.response.GeneratedUserResponse;
import com.badereddine.demo.service.FakeDataService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserGenerationControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private FakeDataService fakeDataService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        fakeDataService = new FakeDataService();
        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "fakeDataService", fakeDataService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void generatesCompatibleRedactedDownloadWithoutCreatingRepositoryFile() throws Exception {
        Path repositoryExport = Path.of("users.json").toAbsolutePath();
        BasicFileAttributes attributesBefore = Files.exists(repositoryExport)
                ? Files.readAttributes(repositoryExport, BasicFileAttributes.class)
                : null;

        MvcResult result = mockMvc.perform(get("/api/users/generate/3")
                        .param("adminCount", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/octet-stream"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=users.json"))
                .andReturn();

        JsonNode users = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        assertThat(users.size()).isEqualTo(3);
        Set<String> generatedFields = new HashSet<>();
        users.get(0).fieldNames().forEachRemaining(generatedFields::add);
        assertThat(generatedFields).containsExactlyInAnyOrder(
                "id", "firstName", "lastName", "birthDate", "city", "country",
                "avatar", "company", "jobPosition", "mobile", "username", "email",
                "role", "enabled", "createdAt", "lastLogin"
        );
        assertThat(users.get(0).path("role").size()).isEqualTo(1);
        assertThat(users.get(0).path("birthDate").isNumber()).isTrue();
        assertThat(users.get(0).path("enabled").isBoolean()).isTrue();
        assertThat(users.get(0).get("id").isNull()).isTrue();
        assertThat(users.get(0).get("createdAt").isNull()).isTrue();
        assertThat(users.get(0).get("lastLogin").isNull()).isTrue();
        assertThat(users.get(0).path("role").path("name").asText()).isEqualTo("ROLE_ADMIN");
        assertThat(users.get(1).path("role").path("name").asText()).isEqualTo("ROLE_USER");
        assertThat(users.get(2).path("role").path("name").asText()).isEqualTo("ROLE_USER");
        users.forEach(user -> {
            assertThat(user.path("enabled").asBoolean()).isFalse();
            assertThat(user.has("password")).isFalse();
            assertThat(user.has("firstName")).isTrue();
            assertThat(user.has("lastName")).isTrue();
            assertThat(user.has("email")).isTrue();
        });

        String download = result.getResponse().getContentAsString();
        assertThat(download)
                .doesNotContain("\"password\"")
                .doesNotContain("$2a$")
                .doesNotContain("$2b$")
                .doesNotContain("$2y$");

        if (attributesBefore == null) {
            assertThat(repositoryExport).doesNotExist();
        } else {
            BasicFileAttributes attributesAfter = Files.readAttributes(repositoryExport, BasicFileAttributes.class);
            assertThat(attributesAfter.size()).isEqualTo(attributesBefore.size());
            assertThat(attributesAfter.lastModifiedTime()).isEqualTo(attributesBefore.lastModifiedTime());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1001})
    void rejectsTotalCountsOutsideDocumentedBounds(int count) throws Exception {
        mockMvc.perform(get("/api/users/generate/{count}", count))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(FakeDataService.INVALID_USER_COUNT_MESSAGE));
    }

    @ParameterizedTest
    @CsvSource({"1, -1", "1, 2", "1000, 1001"})
    void rejectsAdminCountsOutsideDocumentedBounds(int count, int adminCount) throws Exception {
        mockMvc.perform(get("/api/users/generate/{count}", count)
                        .param("adminCount", Integer.toString(adminCount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(FakeDataService.INVALID_ADMIN_COUNT_MESSAGE));
    }

    @Test
    void acceptsInclusiveLowerAndUpperBounds() {
        List<GeneratedUserResponse> minimum = fakeDataService.generateFakeUsers(1, 0);
        List<GeneratedUserResponse> maximum = fakeDataService.generateFakeUsers(1000, 1000);

        assertThat(minimum).hasSize(1);
        assertThat(minimum.get(0).enabled()).isFalse();
        assertThat(maximum).hasSize(1000);
        assertThat(maximum)
                .allMatch(user -> !user.enabled())
                .allMatch(user -> user.role().name().name().equals("ROLE_ADMIN"));
    }
}
