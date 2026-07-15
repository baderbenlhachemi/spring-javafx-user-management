package com.badereddine.demo.controller;

import com.badereddine.demo.model.User;
import com.badereddine.demo.service.CsvExportService;
import com.badereddine.demo.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserCsvExportControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "csvExportService", new CsvExportService());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void preservesDownloadContractAndCapsUnfilteredQuery() throws Exception {
        when(userService.findAll(any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(0), 0));

        mockMvc.perform(get("/api/users/export/csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"users_export.csv\""))
                .andExpect(content().bytes(
                        "ID,Username,Email,First Name,Last Name,Company,Job Position,City,Country,Mobile,Role,Status,Created At,Last Login\n"
                                .getBytes(StandardCharsets.UTF_8)));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userService).findAll(pageableCaptor.capture());
        assertBoundedFirstPage(pageableCaptor.getValue());
    }

    @Test
    void preservesSearchParameterAndCapsFilteredQuery() throws Exception {
        when(userService.searchUsers(eq("Ada"), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<User>(List.of(), invocation.getArgument(1), 0));

        mockMvc.perform(get("/api/users/export/csv").param("search", "  Ada  "))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userService).searchUsers(eq("Ada"), pageableCaptor.capture());
        assertBoundedFirstPage(pageableCaptor.getValue());
    }

    private void assertBoundedFirstPage(Pageable pageable) {
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(CsvExportService.MAX_EXPORT_ROWS);
    }
}
