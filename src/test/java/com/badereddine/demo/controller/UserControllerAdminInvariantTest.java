package com.badereddine.demo.controller;

import com.badereddine.demo.exception.LastActiveAdminException;
import com.badereddine.demo.exception.UserRestExceptionHandler;
import com.badereddine.demo.model.ERole;
import com.badereddine.demo.model.User;
import com.badereddine.demo.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerAdminInvariantTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;
    private User admin;

    @BeforeEach
    void setUp() {
        UserController controller = UserControllerTestFactory.builder()
                .userService(userService)
                .build();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new UserRestExceptionHandler())
                .build();
        admin = new User();
        admin.setId(7L);
        admin.setUsername("ada");
    }

    @Test
    void preservesSuccessfulDeleteResponse() throws Exception {
        when(userService.deleteUser(7L)).thenReturn(admin);

        mockMvc.perform(delete("/api/users/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User 'ada' deleted successfully"));
    }

    @Test
    void preservesSuccessfulRoleChangeResponse() throws Exception {
        when(userService.findById(7L)).thenReturn(Optional.of(admin));
        when(userService.changeRole(7L, ERole.ROLE_USER)).thenReturn(admin);

        mockMvc.perform(patch("/api/users/7/role").param("role", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User role updated to ROLE_USER"));

        verify(userService).changeRole(7L, ERole.ROLE_USER);
    }

    @Test
    void preservesSuccessfulStatusChangeResponse() throws Exception {
        when(userService.setEnabled(7L, false)).thenReturn(admin);

        mockMvc.perform(patch("/api/users/7/status").param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User 'ada' has been disabled"));
    }

    @Test
    void mapsInvariantViolationToStableConflictResponse() throws Exception {
        when(userService.setEnabled(7L, false)).thenThrow(new LastActiveAdminException());

        mockMvc.perform(patch("/api/users/7/status").param("enabled", "false"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(LastActiveAdminException.MESSAGE))
                .andExpect(jsonPath("$.timeStamp").isNumber());
    }
}
