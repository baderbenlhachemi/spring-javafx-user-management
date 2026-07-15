package com.badereddine.demo.importexport;

import com.badereddine.demo.model.ERole;
import com.badereddine.demo.model.Role;
import com.badereddine.demo.model.User;
import com.badereddine.demo.repository.RoleRepository;
import com.badereddine.demo.repository.UserRepository;
import com.badereddine.demo.service.FakeDataService;
import com.badereddine.demo.service.UserImportService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "demo.security.registration-enabled=false",
        "demo.security.swagger-enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ImportExportGenerationIntegrationTest.ContainerConfiguration.class)
@WithMockUser(username = "integration-admin", roles = "ADMIN")
class ImportExportGenerationIntegrationTest {

    private static final Path GENERATED_JSON = Path.of("users.json").toAbsolutePath();
    private static final Path EXPORTED_CSV = Path.of("users_export.csv").toAbsolutePath();

    @TestConfiguration(proxyBeanMethods = false)
    static class ContainerConfiguration {

        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgreSQLContainer() {
            return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("team_access_hub_import_export_test")
                    .withUsername("test_user")
                    .withPassword("test_password");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        assertExportArtifactsAbsent();
    }

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
        assertExportArtifactsAbsent();
    }

    @Test
    void importsValidJsonAndPersistsTheAccountDisabled() throws Exception {
        mockMvc.perform(multipart("/api/users/batch")
                        .file(fixture("valid-users.json")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(1))
                .andExpect(jsonPath("$.successfulImports").value(1))
                .andExpect(jsonPath("$.failedImports").value(0));

        User imported = userRepository.findByUsername("grace-import").orElseThrow();
        assertThat(imported.isEnabled()).isFalse();
        assertThat(imported.getPassword()).startsWith("$2");
        assertThat(imported.getLastLogin()).isNull();
    }

    @Test
    void ignoresPrivilegedFieldsAndAssignsServerOwnedValues() throws Exception {
        mockMvc.perform(multipart("/api/users/batch")
                        .file(fixture("privileged-fields-users.json")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(1))
                .andExpect(jsonPath("$.successfulImports").value(1))
                .andExpect(jsonPath("$.failedImports").value(0));

        User imported = userRepository.findByUsername("privileged-import").orElseThrow();
        assertThat(imported.getId()).isNotEqualTo(999L);
        assertThat(imported.isEnabled()).isFalse();
        assertThat(imported.getCreatedAt()).isNotNull();
        assertThat(imported.getLastLogin()).isNull();
        assertThat(imported.getPassword())
                .doesNotContain("client-supplied")
                .startsWith("$2");
        assertThat(passwordEncoder.matches("client-supplied-password", imported.getPassword())).isFalse();
        assertThat(roleNameFor(imported.getId())).isEqualTo(ERole.ROLE_ADMIN.name());
    }

    @Test
    void rejectsMalformedJsonWithoutPersistingAccounts() throws Exception {
        mockMvc.perform(multipart("/api/users/batch")
                        .file(fixture("malformed-users.json")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(UserImportService.MALFORMED_JSON_MESSAGE));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void countsDatabaseAndInFileDuplicatesWithoutPartialDuplicateWrites() throws Exception {
        saveUser("existing-import", "existing-import@example.test", ERole.ROLE_USER);
        List<Map<String, Object>> records = List.of(
                validRecord("existing-import", "new-email@example.test"),
                validRecord("new-username", "existing-import@example.test"),
                validRecord("accepted-import", "accepted-import@example.test"),
                validRecord("accepted-import", "duplicate-in-file@example.test")
        );

        mockMvc.perform(multipart("/api/users/batch")
                        .file(jsonFile(objectMapper.writeValueAsBytes(records))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(4))
                .andExpect(jsonPath("$.successfulImports").value(1))
                .andExpect(jsonPath("$.failedImports").value(3));

        assertThat(userRepository.count()).isEqualTo(2);
        assertThat(userRepository.findByUsername("accepted-import"))
                .get()
                .extracting(User::isEnabled)
                .isEqualTo(false);
    }

    @Test
    void rejectsAnUnsafeImportRecordCountBeforeWriting() throws Exception {
        List<Map<String, Object>> records = IntStream.range(0, UserImportService.MAX_RECORD_COUNT + 1)
                .mapToObj(index -> validRecord(
                        "bulk-user-" + index,
                        "bulk-user-" + index + "@example.test"
                ))
                .toList();

        mockMvc.perform(multipart("/api/users/batch")
                        .file(jsonFile(objectMapper.writeValueAsBytes(records))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(UserImportService.RECORD_COUNT_MESSAGE));

        assertThat(userRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1001})
    void rejectsUnsafeGenerationCounts(int count) throws Exception {
        mockMvc.perform(get("/api/users/generate/{count}", count))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(FakeDataService.INVALID_USER_COUNT_MESSAGE));
    }

    @ParameterizedTest
    @CsvSource({"1, -1", "1, 2"})
    void rejectsUnsafeGenerationAdminCounts(int count, int adminCount) throws Exception {
        mockMvc.perform(get("/api/users/generate/{count}", count)
                        .param("adminCount", Integer.toString(adminCount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(FakeDataService.INVALID_ADMIN_COUNT_MESSAGE));
    }

    @Test
    void generatesDisabledRedactedJsonWithoutCreatingARepositoryFile() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/users/generate/3")
                        .param("adminCount", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().string("Content-Disposition", "attachment; filename=users.json"))
                .andReturn();

        JsonNode users = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        assertThat(users).hasSize(3);
        users.forEach(user -> {
            assertThat(user.path("enabled").asBoolean()).isFalse();
            assertThat(user.has("password")).isFalse();
            assertThat(user.has("passwordHash")).isFalse();
            assertThat(user.has("password_hash")).isFalse();
        });
        assertThat(users.get(0).path("role").path("name").asText()).isEqualTo("ROLE_ADMIN");
        assertThat(users.get(1).path("role").path("name").asText()).isEqualTo("ROLE_USER");
        assertExportArtifactsAbsent();
    }

    @Test
    void exportsStructurallyEscapedUtf8AndNeutralizedFormulaPrefixes() throws Exception {
        User user = saveUser("=2+2", "+mail@example.test", ERole.ROLE_USER);
        user.setFirstName("-Comma,Name");
        user.setLastName("+Quote\"Name");
        user.setCompany("@Café");
        user.setJobPosition("=Engineer");
        user.setCity("-Line\nBreak");
        user.setCountry("München");
        user.setMobile("+212600000000");
        userRepository.saveAndFlush(user);

        MvcResult result = mockMvc.perform(get("/api/users/export/csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string(
                        "Content-Disposition",
                        "form-data; name=\"attachment\"; filename=\"users_export.csv\""
                ))
                .andReturn();

        String csv = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(csv)
                .contains("'=2+2")
                .contains("'+mail@example.test")
                .contains("\"'-Comma,Name\"")
                .contains("\"'+Quote\"\"Name\"")
                .contains("'@Café")
                .contains("'=Engineer")
                .contains("\"'-Line\nBreak\"")
                .contains("München")
                .contains("'+212600000000");
        assertExportArtifactsAbsent();
    }

    private MockMultipartFile fixture(String filename) throws Exception {
        byte[] contents = new ClassPathResource("importexport/" + filename).getContentAsByteArray();
        return jsonFile(filename, contents);
    }

    private MockMultipartFile jsonFile(byte[] contents) {
        return jsonFile("users.json", contents);
    }

    private MockMultipartFile jsonFile(String filename, byte[] contents) {
        return new MockMultipartFile("file", filename, MediaType.APPLICATION_JSON_VALUE, contents);
    }

    private Map<String, Object> validRecord(String username, String email) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("firstName", "Test");
        record.put("lastName", "User");
        record.put("birthDate", 0L);
        record.put("city", "Casablanca");
        record.put("country", "Morocco");
        record.put("avatar", "https://example.test/avatar.png");
        record.put("company", "Example Labs");
        record.put("jobPosition", "Engineer");
        record.put("mobile", "+212600000000");
        record.put("username", username);
        record.put("email", email);
        record.put("role", Map.of("name", "ROLE_USER"));
        return record;
    }

    private User saveUser(String username, String email, ERole roleName) {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("integration-test-password"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setBirthDate(new Date(0L));
        user.setCity("Casablanca");
        user.setCountry("Morocco");
        user.setAvatar("https://example.test/avatar.png");
        user.setCompany("Example Labs");
        user.setJobPosition("Engineer");
        user.setMobile("+212600000000");
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.saveAndFlush(user);
    }

    private String roleNameFor(Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT r.name FROM roles r JOIN users u ON u.role_id = r.id WHERE u.id = ?",
                String.class,
                userId
        );
    }

    private void assertExportArtifactsAbsent() {
        assertThat(GENERATED_JSON).doesNotExist();
        assertThat(EXPORTED_CSV).doesNotExist();
    }
}
