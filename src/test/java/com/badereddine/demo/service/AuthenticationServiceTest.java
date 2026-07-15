package com.badereddine.demo.service;

import com.badereddine.demo.exception.UserNotFoundException;
import com.badereddine.demo.model.ERole;
import com.badereddine.demo.model.Role;
import com.badereddine.demo.model.User;
import com.badereddine.demo.payload.request.LoginRequest;
import com.badereddine.demo.payload.request.SignupRequest;
import com.badereddine.demo.payload.response.JwtResponse;
import com.badereddine.demo.security.jwt.JwtUtils;
import com.badereddine.demo.security.services.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-15T12:00:00Z");

    @Mock
    private UserService userService;

    @Mock
    private RoleService roleService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
                userService,
                roleService,
                authenticationManager,
                jwtUtils,
                passwordEncoder,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticateUpdatesLastLoginThenGeneratesCompatibleTokenResponseTransactionally() throws Exception {
        LoginRequest request = loginRequest("member", "test-password");
        User user = user("member", ERole.ROLE_USER);
        UserDetailsImpl principal = UserDetailsImpl.build(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        when(userService.findByUsernameOrEmail("member", null)).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("generated-token-placeholder");

        JwtResponse response = authenticationService.authenticate(request);

        assertThat(response.getAccessToken()).isEqualTo("generated-token-placeholder");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getUsername()).isEqualTo("member");
        assertThat(response.getEmail()).isEqualTo("member@example.test");
        assertThat(response.getRoles()).containsExactly("ROLE_USER");
        assertThat(user.getLastLogin()).isEqualTo(Date.from(NOW));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
        assertTransactional("authenticate", LoginRequest.class);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> tokenCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getPrincipal()).isEqualTo("member");
        assertThat(tokenCaptor.getValue().getCredentials()).isEqualTo("test-password");

        InOrder order = inOrder(userService, authenticationManager, jwtUtils);
        order.verify(userService).findByUsernameOrEmail("member", null);
        order.verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        order.verify(userService).save(user);
        order.verify(jwtUtils).generateJwtToken(authentication);
    }

    @Test
    void authenticateRejectsUnknownUserBeforeAuthenticationOrTokenGeneration() {
        LoginRequest request = loginRequest("missing", "test-password");
        when(userService.findByUsernameOrEmail("missing", null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.authenticate(request))
                .isInstanceOf(UserNotFoundException.class);

        verifyNoInteractions(authenticationManager, jwtUtils);
        verify(userService, never()).save(any());
    }

    @Test
    void registerBuildsAndPersistsCompatibleUserTransactionally() throws Exception {
        SignupRequest request = signupRequest();
        Role userRole = new Role(ERole.ROLE_USER);
        when(userService.existsByUsername("new-user")).thenReturn(false);
        when(userService.existsByEmail("new-user@example.test")).thenReturn(false);
        when(roleService.findByName(ERole.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("test-password")).thenReturn("encoded-password-placeholder");

        AuthenticationService.RegistrationResult result = authenticationService.register(request);

        assertThat(result.successful()).isTrue();
        assertThat(result.message()).isEqualTo(AuthenticationService.REGISTRATION_SUCCESS_MESSAGE);
        assertTransactional("register", SignupRequest.class);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("new-user");
        assertThat(savedUser.getEmail()).isEqualTo("new-user@example.test");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password-placeholder");
        assertThat(savedUser.getFirstName()).isEqualTo("New");
        assertThat(savedUser.getLastName()).isEqualTo("User");
        assertThat(savedUser.getRole()).isSameAs(userRole);
        assertThat(savedUser.getBirthDate()).isEqualTo(Date.from(NOW));
        assertThat(savedUser.getCity()).isEqualTo("Not specified");
        assertThat(savedUser.getCountry()).isEqualTo("Not specified");
        assertThat(savedUser.getCompany()).isEqualTo("Not specified");
        assertThat(savedUser.getJobPosition()).isEqualTo("Not specified");
        assertThat(savedUser.getMobile()).isEqualTo("+212 000000000");
        assertThat(savedUser.getAvatar()).isEqualTo(
                "https://ui-avatars.com/api/?name=New+User&background=6366F1&color=fff"
        );
    }

    @Test
    void registerPreservesDuplicateUsernameResultWithoutPersisting() {
        when(userService.existsByUsername("new-user")).thenReturn(true);

        AuthenticationService.RegistrationResult result = authenticationService.register(signupRequest());

        assertThat(result.successful()).isFalse();
        assertThat(result.message()).isEqualTo(AuthenticationService.USERNAME_TAKEN_MESSAGE);
        verify(userService, never()).existsByEmail(any());
        verify(userService, never()).save(any());
        verifyNoInteractions(roleService, passwordEncoder);
    }

    @Test
    void registerPreservesDuplicateEmailResultWithoutPersisting() {
        when(userService.existsByUsername("new-user")).thenReturn(false);
        when(userService.existsByEmail("new-user@example.test")).thenReturn(true);

        AuthenticationService.RegistrationResult result = authenticationService.register(signupRequest());

        assertThat(result.successful()).isFalse();
        assertThat(result.message()).isEqualTo(AuthenticationService.EMAIL_IN_USE_MESSAGE);
        verify(userService, never()).save(any());
        verifyNoInteractions(roleService, passwordEncoder);
    }

    private void assertTransactional(String methodName, Class<?> parameterType) throws Exception {
        Method method = AuthenticationService.class.getMethod(methodName, parameterType);
        assertThat(method.isAnnotationPresent(Transactional.class)).isTrue();
    }

    private LoginRequest loginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private SignupRequest signupRequest() {
        SignupRequest request = new SignupRequest();
        request.setUsername("new-user");
        request.setEmail("new-user@example.test");
        request.setPassword("test-password");
        request.setFirstName("New");
        request.setLastName("User");
        return request;
    }

    private User user(String username, ERole roleName) {
        User user = new User();
        user.setId(7L);
        user.setUsername(username);
        user.setEmail(username + "@example.test");
        user.setPassword("encoded-password-placeholder");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(new Role(roleName));
        user.setEnabled(true);
        return user;
    }
}
