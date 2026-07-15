package com.badereddine.demo.controller;

import com.badereddine.demo.exception.UserImportException;
import com.badereddine.demo.service.UserImportService;
import com.badereddine.demo.service.UserTransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserImportControllerTest {

    @Mock
    private UserTransferService userTransferService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserController controller = UserControllerTestFactory.builder()
                .userTransferService(userTransferService)
                .build();
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void preservesMultipartFieldAndSuccessfulResultCountShape() throws Exception {
        MockMultipartFile file = jsonFile("[]");
        when(userTransferService.importUsers(any()))
                .thenReturn(new UserImportService.UserImportResult(3, 2, 1));

        mockMvc.perform(multipart("/api/users/batch").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(3))
                .andExpect(jsonPath("$.successfulImports").value(2))
                .andExpect(jsonPath("$.failedImports").value(1));

        verify(userTransferService).importUsers(file);
    }

    @Test
    void returnsStableBadRequestForInvalidImport() throws Exception {
        MockMultipartFile file = jsonFile("[{not-json}]");
        when(userTransferService.importUsers(any()))
                .thenThrow(new UserImportException(UserImportService.MALFORMED_JSON_MESSAGE));

        mockMvc.perform(multipart("/api/users/batch").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(UserImportService.MALFORMED_JSON_MESSAGE));
    }

    private MockMultipartFile jsonFile(String json) {
        return new MockMultipartFile("file", "users.json", "application/json", json.getBytes());
    }
}
