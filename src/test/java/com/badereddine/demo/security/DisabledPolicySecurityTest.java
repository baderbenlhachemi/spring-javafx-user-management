package com.badereddine.demo.security;

import com.badereddine.demo.controller.UserController;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {UserController.class, SecurityPolicyProbeController.class},
        properties = {
                "demo.security.registration-enabled=false",
                "demo.security.swagger-enabled=false"
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
class DisabledPolicySecurityTest {

    private static final String VALID_REGISTRATION_JSON = """
            {
              "username": "new-user",
              "email": "new-user@example.test",
              "password": "secret123",
              "firstName": "New",
              "lastName": "User"
            }
            """;

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
    void disabledRegistrationRejectsAnonymousRequests() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REGISTRATION_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService, roleService);
    }

    @Test
    @WithMockUser
    void disabledRegistrationRejectsAuthenticatedRequests() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REGISTRATION_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService, roleService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs/security-probe", "/swagger-ui/security-probe"})
    void disabledSwaggerRoutesRejectAnonymousRequests(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs/security-probe", "/swagger-ui/security-probe"})
    @WithMockUser
    void disabledSwaggerRoutesRejectAuthenticatedRequests(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isForbidden());
    }
}
