package com.badereddine.demo.controller;

import com.badereddine.demo.service.UserTransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserCsvExportControllerTest {

    @Mock
    private UserTransferService userTransferService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserTransferController controller = new UserTransferController(userTransferService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void preservesDownloadContractAndDelegatesUnfilteredExport() throws Exception {
        byte[] csv = "ID,Username,Email,First Name,Last Name,Company,Job Position,City,Country,Mobile,Role,Status,Created At,Last Login\n"
                .getBytes(StandardCharsets.UTF_8);
        when(userTransferService.exportUsersCsv(null)).thenReturn(csv);

        mockMvc.perform(get("/api/users/export/csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"users_export.csv\""))
                .andExpect(content().bytes(
                        csv));

        verify(userTransferService).exportUsersCsv(null);
    }

    @Test
    void preservesSearchParameterForTransferService() throws Exception {
        when(userTransferService.exportUsersCsv("  Ada  ")).thenReturn(new byte[0]);

        mockMvc.perform(get("/api/users/export/csv").param("search", "  Ada  "))
                .andExpect(status().isOk());

        verify(userTransferService).exportUsersCsv("  Ada  ");
    }
}
