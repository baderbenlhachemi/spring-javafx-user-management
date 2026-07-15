package com.badereddine.demo.controller;

import com.badereddine.demo.model.ERole;
import com.badereddine.demo.model.Role;
import com.badereddine.demo.model.User;
import com.badereddine.demo.payload.request.ProfileUpdateRequest;
import com.badereddine.demo.payload.response.UserResponse;
import com.badereddine.demo.payload.response.UserResponseMapper;
import com.badereddine.demo.security.services.UserDetailsImpl;
import com.badereddine.demo.service.UserService;
import com.badereddine.demo.service.UserPaginationPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerResponseTest {

    @Mock
    private UserService userService;

    private UserController controller;
    private MockMvc mockMvc;
    private User user;

    @BeforeEach
    void setUp() {
        controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "userResponseMapper", new UserResponseMapper());
        ReflectionTestUtils.setField(controller, "userPaginationPolicy", new UserPaginationPolicy());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        user = representativeUser();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsSingleUserDtoWithCompatibleFieldsAndNoPersistenceOrPasswordFields() throws Exception {
        when(userService.findById(7L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/id/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.lastName").value("Lovelace"))
                .andExpect(jsonPath("$.birthDate").exists())
                .andExpect(jsonPath("$.city").value("London"))
                .andExpect(jsonPath("$.country").value("United Kingdom"))
                .andExpect(jsonPath("$.avatar").value("https://example.test/ada.png"))
                .andExpect(jsonPath("$.company").value("Analytical Engines"))
                .andExpect(jsonPath("$.jobPosition").value("Programmer"))
                .andExpect(jsonPath("$.mobile").value("+44 20 0000 0000"))
                .andExpect(jsonPath("$.username").value("ada"))
                .andExpect(jsonPath("$.email").value("ada@example.test"))
                .andExpect(jsonPath("$.role.name").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.lastLogin").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.role.id").doesNotExist())
                .andExpect(jsonPath("$.hibernateLazyInitializer").doesNotExist());
    }

    @Test
    void returnsPaginatedUserDtosWithCompatiblePaginationFields() throws Exception {
        PageRequest request = PageRequest.of(1, 2);
        when(userService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user), request, 5));

        mockMvc.perform(get("/api/users")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].id").value(7))
                .andExpect(jsonPath("$.users[0].role.name").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.users[0].password").doesNotExist())
                .andExpect(jsonPath("$.users[0].role.id").doesNotExist())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.totalItems").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.size").value(2));
    }

    @Test
    void profileAndUpdateEndpointsReturnUserResponseDtos() throws Exception {
        UserDetailsImpl principal = new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        when(userService.findByUsername("ada")).thenReturn(Optional.of(user));
        when(userService.findById(7L)).thenReturn(Optional.of(user));

        ResponseEntity<UserResponse> profileResponse = controller.getMyProfile();
        ResponseEntity<?> profileUpdateResponse = controller.updateProfile(new ProfileUpdateRequest());
        ResponseEntity<?> adminUpdateResponse = controller.updateUser(7L, new ProfileUpdateRequest());

        assertThat(profileResponse.getBody()).isExactlyInstanceOf(UserResponse.class);
        assertThat(profileUpdateResponse.getBody()).isExactlyInstanceOf(UserResponse.class);
        assertThat(adminUpdateResponse.getBody()).isExactlyInstanceOf(UserResponse.class);
        verify(userService, org.mockito.Mockito.times(2)).save(user);
    }

    private User representativeUser() {
        Date birthDate = new Date(0L);
        Date createdAt = new Date(1_700_000_000_000L);
        Date lastLogin = new Date(1_700_003_600_000L);
        Role role = new Role(ERole.ROLE_ADMIN);
        ReflectionTestUtils.setField(role, "id", 11);

        User value = new User();
        value.setId(7L);
        value.setFirstName("Ada");
        value.setLastName("Lovelace");
        value.setBirthDate(birthDate);
        value.setCity("London");
        value.setCountry("United Kingdom");
        value.setAvatar("https://example.test/ada.png");
        value.setCompany("Analytical Engines");
        value.setJobPosition("Programmer");
        value.setMobile("+44 20 0000 0000");
        value.setUsername("ada");
        value.setEmail("ada@example.test");
        value.setPassword("encoded-password-placeholder");
        value.setRole(role);
        value.setEnabled(true);
        value.setCreatedAt(createdAt);
        value.setLastLogin(lastLogin);
        return value;
    }
}
