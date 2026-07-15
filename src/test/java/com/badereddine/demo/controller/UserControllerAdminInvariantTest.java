package com.badereddine.demo.controller;

import com.badereddine.demo.exception.LastActiveAdminException;
import com.badereddine.demo.exception.UserRestExceptionHandler;
import com.badereddine.demo.model.ERole;
import com.badereddine.demo.service.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerAdminInvariantTest {

    @Mock
    private AdminUserService adminUserService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserController controller = UserControllerTestFactory.builder()
                .adminUserService(adminUserService)
                .build();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new UserRestExceptionHandler())
                .build();
    }

    @Test
    void preservesSuccessfulDeleteResponse() throws Exception {
        when(adminUserService.deleteUser(7L)).thenReturn("ada");

        mockMvc.perform(delete("/api/users/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User 'ada' deleted successfully"));
    }

    @Test
    void preservesSuccessfulRoleChangeResponse() throws Exception {
        when(adminUserService.changeUserRole(7L, "ROLE_USER")).thenReturn(ERole.ROLE_USER);

        mockMvc.perform(patch("/api/users/7/role").param("role", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User role updated to ROLE_USER"));

        verify(adminUserService).changeUserRole(7L, "ROLE_USER");
    }

    @Test
    void preservesSuccessfulStatusChangeResponse() throws Exception {
        when(adminUserService.setUserEnabled(7L, false)).thenReturn("ada");

        mockMvc.perform(patch("/api/users/7/status").param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User 'ada' has been disabled"));
    }

    @Test
    void mapsInvariantViolationToStableConflictResponse() throws Exception {
        when(adminUserService.setUserEnabled(7L, false)).thenThrow(new LastActiveAdminException());

        mockMvc.perform(patch("/api/users/7/status").param("enabled", "false"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(LastActiveAdminException.MESSAGE))
                .andExpect(jsonPath("$.timeStamp").isNumber());
    }
}
