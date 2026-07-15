package com.badereddine.demo.security;

import com.badereddine.demo.controller.UserController;
import com.badereddine.demo.model.ERole;
import com.badereddine.demo.model.Role;
import com.badereddine.demo.model.User;
import com.badereddine.demo.payload.response.UserResponseMapper;
import com.badereddine.demo.security.jwt.AuthEntryPointJwt;
import com.badereddine.demo.security.jwt.JwtUtils;
import com.badereddine.demo.security.services.UserDetailsServiceImpl;
import com.badereddine.demo.service.CsvExportService;
import com.badereddine.demo.service.FakeDataService;
import com.badereddine.demo.service.RoleService;
import com.badereddine.demo.service.UserImportService;
import com.badereddine.demo.service.UserPaginationPolicy;
import com.badereddine.demo.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {UserController.class, SecurityPolicyProbeController.class},
        properties = {
                "demo.security.registration-enabled=true",
                "demo.security.swagger-enabled=true"
        }
)
@Import({
        WebSecurityConfig.class,
        AuthEntryPointJwt.class,
        UserResponseMapper.class,
        UserPaginationPolicy.class,
        CsvExportService.class
})
@ActiveProfiles("test")
class EnabledPolicySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private RoleService roleService;

    @MockBean
    private FakeDataService fakeDataService;

    @MockBean
    private UserImportService userImportService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void enabledRegistrationPreservesEndpointAndSuccessfulResponse() throws Exception {
        Role userRole = new Role(ERole.ROLE_USER);
        when(userService.existsByUsername("new-user")).thenReturn(false);
        when(userService.existsByEmail("new-user@example.test")).thenReturn(false);
        when(roleService.findByName(ERole.ROLE_USER)).thenReturn(Optional.of(userRole));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "new-user",
                                  "email": "new-user@example.test",
                                  "password": "secret123",
                                  "firstName": "New",
                                  "lastName": "User"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully!"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isSameAs(userRole);
        assertThat(userCaptor.getValue().getPassword()).isNotEqualTo("secret123");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs/security-probe", "/swagger-ui/security-probe"})
    void enabledSwaggerRoutesArePublic(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(content().string("documentation available"));
    }
}
