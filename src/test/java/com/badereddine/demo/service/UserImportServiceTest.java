package com.badereddine.demo.service;

import com.badereddine.demo.exception.UserImportException;
import com.badereddine.demo.model.ERole;
import com.badereddine.demo.model.Role;
import com.badereddine.demo.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserImportServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private RoleService roleService;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UserImportService importService;
    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        importService = new UserImportService(userService, roleService, passwordEncoder, objectMapper);
        adminRole = new Role(ERole.ROLE_ADMIN);
        userRole = new Role(ERole.ROLE_USER);
    }

    @Test
    void importsRestrictedFieldsWithServerGeneratedCredentialsAndDisabledStatus() throws Exception {
        stubPersistence();
        Map<String, Object> admin = validRecord("admin-one", "admin-one@example.test", "ROLE_ADMIN");
        admin.put("id", 91);
        admin.put("password", "client-supplied-secret");
        admin.put("passwordHash", "client-supplied-hash");
        admin.put("enabled", true);
        admin.put("createdAt", 1_700_000_000_000L);
        admin.put("lastLogin", 1_700_000_100_000L);
        @SuppressWarnings("unchecked")
        Map<String, Object> adminRoleInput = (Map<String, Object>) admin.get("role");
        adminRoleInput.put("id", 77);
        Map<String, Object> regular = validRecord("user-one", "user-one@example.test", "ROLE_USER");

        UserImportService.UserImportResult result = importService.importUsers(file(List.of(admin, regular)));

        assertThat(result.totalRecords()).isEqualTo(2);
        assertThat(result.successfulImports()).isEqualTo(2);
        assertThat(result.failedImports()).isZero();

        ArgumentCaptor<String> credentialCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder, times(2)).encode(credentialCaptor.capture());
        assertThat(new HashSet<>(credentialCaptor.getAllValues())).hasSize(2);
        assertThat(credentialCaptor.getAllValues())
                .allMatch(value -> !value.isBlank())
                .noneMatch(value -> value.contains("client-supplied"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService, times(2)).save(userCaptor.capture());
        List<User> savedUsers = userCaptor.getAllValues();
        assertThat(savedUsers)
                .allMatch(user -> !user.isEnabled())
                .allMatch(user -> user.getId() == null)
                .allMatch(user -> user.getCreatedAt() == null)
                .allMatch(user -> user.getLastLogin() == null)
                .allMatch(user -> user.getPassword().startsWith("encoded:"));
        assertThat(savedUsers.get(0).getRole()).isSameAs(adminRole);
        assertThat(savedUsers.get(1).getRole()).isSameAs(userRole);
    }

    @Test
    void countsInFileAndDatabaseDuplicatesAsFailedImports() throws Exception {
        stubPersistence();
        when(userService.existsByEmail(anyString()))
                .thenAnswer(invocation -> "existing@example.test".equals(invocation.getArgument(0, String.class)));
        Map<String, Object> first = validRecord("first", "first@example.test", "ROLE_USER");
        Map<String, Object> duplicateUsername = validRecord("first", "second@example.test", "ROLE_USER");
        Map<String, Object> duplicateEmail = validRecord("third", "first@example.test", "ROLE_ADMIN");
        Map<String, Object> databaseDuplicate = validRecord("fourth", "existing@example.test", "ROLE_USER");

        UserImportService.UserImportResult result = importService.importUsers(
                file(List.of(first, duplicateUsername, duplicateEmail, databaseDuplicate))
        );

        assertThat(result.totalRecords()).isEqualTo(4);
        assertThat(result.successfulImports()).isEqualTo(1);
        assertThat(result.failedImports()).isEqualTo(3);
        verify(userService, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode(anyString());
    }

    @Test
    void rejectsMissingRequiredFieldsBeforePersistence() throws Exception {
        Map<String, Object> invalid = validRecord("missing-email", "missing@example.test", "ROLE_USER");
        invalid.remove("email");

        assertRejected(
                file(List.of(invalid)),
                UserImportService.INVALID_FIELDS_MESSAGE.formatted(1)
        );
    }

    @Test
    void rejectsUnsupportedRolesBeforePersistence() throws Exception {
        assertRejected(
                file(List.of(validRecord("owner", "owner@example.test", "ROLE_OWNER"))),
                UserImportService.INVALID_ROLE_MESSAGE.formatted(1)
        );
    }

    @Test
    void rejectsUnknownNonPrivilegedFieldsBeforePersistence() throws Exception {
        Map<String, Object> invalid = validRecord("unknown", "unknown@example.test", "ROLE_USER");
        invalid.put("permissions", List.of("ALL"));

        assertRejected(file(List.of(invalid)), UserImportService.UNSUPPORTED_FIELDS_MESSAGE);
    }

    @Test
    void rejectsMalformedJsonBeforePersistence() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "users.json", "application/json", "[{not-json}]".getBytes()
        );

        assertRejected(file, UserImportService.MALFORMED_JSON_MESSAGE);
    }

    @Test
    void rejectsMissingEmptyAndOversizedFilesBeforePersistence() {
        MockMultipartFile empty = new MockMultipartFile("file", "users.json", "application/json", new byte[0]);
        MockMultipartFile oversized = new MockMultipartFile(
                "file",
                "users.json",
                "application/json",
                new byte[(int) UserImportService.MAX_FILE_SIZE_BYTES + 1]
        );

        assertRejected(null, UserImportService.FILE_REQUIRED_MESSAGE);
        assertRejected(empty, UserImportService.FILE_EMPTY_MESSAGE);
        assertRejected(oversized, UserImportService.FILE_TOO_LARGE_MESSAGE);
    }

    @Test
    void rejectsUnsafeRecordCountsBeforePersistence() throws Exception {
        List<Map<String, Object>> tooManyRecords = IntStream.range(0, UserImportService.MAX_RECORD_COUNT + 1)
                .mapToObj(index -> validRecord(
                        "user-" + index,
                        "user-" + index + "@example.test",
                        "ROLE_USER"
                ))
                .toList();

        assertRejected(file(List.of()), UserImportService.RECORD_COUNT_MESSAGE);
        assertRejected(file(tooManyRecords), UserImportService.RECORD_COUNT_MESSAGE);
    }

    private void stubPersistence() {
        when(roleService.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(roleService.findByName(ERole.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString()))
                .thenAnswer(invocation -> "encoded:" + invocation.getArgument(0, String.class));
        when(userService.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void assertRejected(MockMultipartFile file, String message) {
        assertThatThrownBy(() -> importService.importUsers(file))
                .isInstanceOf(UserImportException.class)
                .hasMessage(message);
        verifyNoInteractions(userService, roleService, passwordEncoder);
    }

    private MockMultipartFile file(Object records) throws Exception {
        return new MockMultipartFile(
                "file",
                "users.json",
                "application/json",
                objectMapper.writeValueAsBytes(records)
        );
    }

    private Map<String, Object> validRecord(String username, String email, String role) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("firstName", "Test");
        record.put("lastName", "User");
        record.put("birthDate", 0L);
        record.put("city", "Casablanca");
        record.put("country", "Morocco");
        record.put("avatar", "https://example.test/avatar.png");
        record.put("company", "Example");
        record.put("jobPosition", "Engineer");
        record.put("mobile", "+212600000000");
        record.put("username", username);
        record.put("email", email);
        record.put("role", new LinkedHashMap<>(Map.of("name", role)));
        return record;
    }
}
