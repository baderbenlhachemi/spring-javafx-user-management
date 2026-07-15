package com.badereddine.demo.security;

import com.badereddine.demo.controller.UserController;
import com.badereddine.demo.config.TimeConfiguration;
import com.badereddine.demo.model.ERole;
import com.badereddine.demo.model.Role;
import com.badereddine.demo.model.User;
import com.badereddine.demo.payload.response.UserResponseMapper;
import com.badereddine.demo.repository.UserRepository;
import com.badereddine.demo.security.jwt.AuthEntryPointJwt;
import com.badereddine.demo.security.jwt.JwtUtils;
import com.badereddine.demo.security.services.UserDetailsImpl;
import com.badereddine.demo.security.services.UserDetailsServiceImpl;
import com.badereddine.demo.service.AdminUserService;
import com.badereddine.demo.service.AuthenticationService;
import com.badereddine.demo.service.ProfileService;
import com.badereddine.demo.service.RoleService;
import com.badereddine.demo.service.UserService;
import com.badereddine.demo.service.UserStatisticsService;
import com.badereddine.demo.service.UserTransferService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Key;
import java.util.Date;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserController.class,
        properties = {
                "demo.security.registration-enabled=false",
                "demo.security.swagger-enabled=false"
        }
)
@Import({
        WebSecurityConfig.class,
        AuthEntryPointJwt.class,
        AuthenticationService.class,
        ProfileService.class,
        TimeConfiguration.class,
        JwtUtils.class,
        UserDetailsServiceImpl.class,
        UserResponseMapper.class,
        UserStatisticsService.class
})
@ActiveProfiles("test")
class AuthenticationAuthorizationMvcTest {

    private static final String TEST_JWT_SECRET =
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
    private static final String PASSWORD = "test-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserService userService;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private RoleService roleService;

    @MockBean
    private UserTransferService userTransferService;

    @Test
    void anonymousUserCanLogInThroughPublicEndpoint() throws Exception {
        User member = user("member-login", ERole.ROLE_USER, true);
        when(userService.findByUsernameOrEmail(member.getUsername(), null))
                .thenReturn(Optional.of(member));
        when(userRepository.findByUsername(member.getUsername()))
                .thenReturn(Optional.of(member));

        mockMvc.perform(post("/api/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "member-login",
                                  "password": "test-password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value(member.getUsername()))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));

        verify(userService).save(member);
    }

    @Test
    void authenticatedMemberCanReadOwnProfile() throws Exception {
        User member = user("member-profile", ERole.ROLE_USER, true);
        when(userRepository.findByUsername(member.getUsername()))
                .thenReturn(Optional.of(member));
        when(userService.findByUsername(member.getUsername()))
                .thenReturn(Optional.of(member));

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(member.getUsername()))
                .andExpect(jsonPath("$.role.name").value("ROLE_USER"));
    }

    @Test
    void anonymousUserReceivesUnauthorizedForAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/stats/users"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userRepository, userService);
    }

    @Test
    void authenticatedMemberIsDeniedAdminEndpoint() throws Exception {
        User member = user("member-forbidden", ERole.ROLE_USER, true);
        when(userRepository.findByUsername(member.getUsername()))
                .thenReturn(Optional.of(member));

        mockMvc.perform(get("/api/stats/users")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(member)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("The request could not be processed"));

        verifyNoInteractions(userService);
    }

    @Test
    void authenticatedAdministratorCanAccessAdminEndpoint() throws Exception {
        User administrator = user("admin-allowed", ERole.ROLE_ADMIN, true);
        when(userRepository.findByUsername(administrator.getUsername()))
                .thenReturn(Optional.of(administrator));

        mockMvc.perform(get("/api/stats/users")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(administrator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(0))
                .andExpect(jsonPath("$.totalAdmins").value(0))
                .andExpect(jsonPath("$.totalRegularUsers").value(0))
                .andExpect(jsonPath("$.newUsersToday").value(0));
    }

    @Test
    void disabledUserTokenCannotAccessProtectedEndpoint() throws Exception {
        User disabledMember = user("member-disabled", ERole.ROLE_USER, false);
        when(userRepository.findByUsername(disabledMember.getUsername()))
                .thenReturn(Optional.of(disabledMember));

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(disabledMember)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    void invalidJwtCannotAccessProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userRepository, userService);
    }

    @Test
    void expiredJwtCannotAccessProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken("member-expired")))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userRepository, userService);
    }

    private User user(String username, ERole roleName, boolean enabled) {
        User user = new User();
        user.setId(7L);
        user.setUsername(username);
        user.setEmail(username + "@example.test");
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(new Role(roleName));
        user.setEnabled(enabled);
        return user;
    }

    private String bearerToken(User user) {
        UserDetailsImpl principal = UserDetailsImpl.build(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );
        return "Bearer " + jwtUtils.generateJwtToken(authentication);
    }

    private String expiredToken(String username) {
        long now = System.currentTimeMillis();
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_JWT_SECRET));
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(now - 120_000))
                .setExpiration(new Date(now - 60_000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
