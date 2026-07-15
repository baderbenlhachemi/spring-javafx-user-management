package com.badereddine.demo.service;

import com.badereddine.demo.model.ERole;
import com.badereddine.demo.model.User;
import com.badereddine.demo.payload.response.GeneratedUserResponse;
import com.badereddine.demo.payload.response.RoleResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserImportExportGenerationServiceTest {

    @Mock
    private FakeDataService fakeDataService;

    @Mock
    private UserImportService userImportService;

    @Mock
    private UserService userService;

    @Mock
    private CsvExportService csvExportService;

    private ObjectMapper objectMapper;
    private UserTransferService userTransferService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        userTransferService = new UserTransferService(
                fakeDataService,
                userImportService,
                userService,
                csvExportService,
                objectMapper
        );
    }

    @Test
    void generatesCompatibleJsonBytesThroughTheBoundedGenerator() throws Exception {
        GeneratedUserResponse generatedUser = new GeneratedUserResponse(
                null,
                "Ada",
                "Lovelace",
                null,
                "London",
                "United Kingdom",
                null,
                "Analytical Engines",
                "Programmer",
                null,
                "ada",
                "ada@example.test",
                new RoleResponse(ERole.ROLE_ADMIN),
                false,
                null,
                null
        );
        when(fakeDataService.generateFakeUsers(3, 1)).thenReturn(List.of(generatedUser));

        byte[] json = userTransferService.generateUsersJson(3, 1);

        JsonNode users = objectMapper.readTree(json);
        assertThat(users).hasSize(1);
        assertThat(users.get(0).path("username").asText()).isEqualTo("ada");
        assertThat(users.get(0).path("role").path("name").asText()).isEqualTo("ROLE_ADMIN");
        assertThat(users.get(0).path("enabled").asBoolean()).isFalse();
        assertThat(users.get(0).has("password")).isFalse();
        verify(fakeDataService).generateFakeUsers(3, 1);
    }

    @Test
    void preservesGenerationBoundFailuresWithoutSerializingOutput() {
        when(fakeDataService.generateFakeUsers(0, 0))
                .thenThrow(new IllegalArgumentException(FakeDataService.INVALID_USER_COUNT_MESSAGE));

        assertThatThrownBy(() -> userTransferService.generateUsersJson(0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(FakeDataService.INVALID_USER_COUNT_MESSAGE);
    }

    @Test
    void delegatesBoundedImportAndPreservesResultCounts() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.json",
                "application/json",
                "[]".getBytes(StandardCharsets.UTF_8)
        );
        UserImportService.UserImportResult expected = new UserImportService.UserImportResult(3, 2, 1);
        when(userImportService.importUsers(file)).thenReturn(expected);

        assertThat(userTransferService.importUsers(file)).isSameAs(expected);
        verify(userImportService).importUsers(file);
    }

    @Test
    void exportsAnUnfilteredBoundedFirstPage() {
        User user = new User();
        when(userService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));
        byte[] expectedCsv = "csv".getBytes(StandardCharsets.UTF_8);
        when(csvExportService.createCsv(List.of(user))).thenReturn(expectedCsv);

        assertThat(userTransferService.exportUsersCsv(null)).isSameAs(expectedCsv);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userService).findAll(pageableCaptor.capture());
        assertBoundedFirstPage(pageableCaptor.getValue());
        verify(userService, never()).searchUsers(any(), any());
        verify(csvExportService).createCsv(List.of(user));
    }

    @Test
    void trimsSearchAndExportsAFilteredBoundedFirstPage() {
        User user = new User();
        when(userService.searchUsers(eq("Ada"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(csvExportService.createCsv(List.of(user))).thenReturn(new byte[0]);

        userTransferService.exportUsersCsv("  Ada  ");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userService).searchUsers(eq("Ada"), pageableCaptor.capture());
        assertBoundedFirstPage(pageableCaptor.getValue());
        verify(csvExportService).createCsv(List.of(user));
    }

    @Test
    void declaresImportAndExportTransactionBoundaries() throws Exception {
        Method importMethod = UserTransferService.class.getMethod(
                "importUsers",
                org.springframework.web.multipart.MultipartFile.class
        );
        Method exportMethod = UserTransferService.class.getMethod("exportUsersCsv", String.class);

        Transactional importTransaction = importMethod.getAnnotation(Transactional.class);
        Transactional exportTransaction = exportMethod.getAnnotation(Transactional.class);
        assertThat(importTransaction).isNotNull();
        assertThat(importTransaction.readOnly()).isFalse();
        assertThat(exportTransaction).isNotNull();
        assertThat(exportTransaction.readOnly()).isTrue();
    }

    private void assertBoundedFirstPage(Pageable pageable) {
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(CsvExportService.MAX_EXPORT_ROWS);
    }
}
