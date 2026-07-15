package com.badereddine.demo.controller;

import com.badereddine.demo.exception.InvalidPasswordException;
import com.badereddine.demo.exception.UserRestExceptionHandler;
import com.badereddine.demo.model.ERole;
import com.badereddine.demo.payload.request.PasswordChangeRequest;
import com.badereddine.demo.payload.request.ProfileUpdateRequest;
import com.badereddine.demo.payload.response.RoleResponse;
import com.badereddine.demo.payload.response.UserResponse;
import com.badereddine.demo.security.services.UserDetailsImpl;
import com.badereddine.demo.service.AdminUserService;
import com.badereddine.demo.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private ProfileService profileService;

    @Mock
    private AdminUserService adminUserService;

    private MockMvc mockMvc;
    private UserResponse profile;

    @BeforeEach
    void setUp() {
        ProfileController controller = new ProfileController(profileService, adminUserService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new UserRestExceptionHandler())
                .build();
        profile = representativeProfile();
    }

    @Test
    void getCurrentProfilePreservesPathAndRedactedResponse() throws Exception {
        when(profileService.getCurrentProfile()).thenReturn(profile);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.username").value("ada"))
                .andExpect(jsonPath("$.email").value("ada@example.test"))
                .andExpect(jsonPath("$.role.name").value("ROLE_USER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(profileService).getCurrentProfile();
    }

    @Test
    void getUsernameProfilePreservesAuthenticatedLookupContract() throws Exception {
        UserDetailsImpl userDetails = new UserDetailsImpl(
                7L,
                "ada",
                "ada@example.test",
                "encoded-password-placeholder",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        when(adminUserService.getUserProfile("ada")).thenReturn(userDetails);

        mockMvc.perform(get("/api/users/ada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.username").value("ada"))
                .andExpect(jsonPath("$.email").value("ada@example.test"))
                .andExpect(jsonPath("$.authorities[0].authority").value("ROLE_USER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(adminUserService).getUserProfile("ada");
    }

    @Test
    void updateCurrentProfilePreservesPutRequestAndSuccessfulResponse() throws Exception {
        when(profileService.updateProfile(any(ProfileUpdateRequest.class)))
                .thenReturn(new ProfileService.ProfileUpdateResult(true, profile, null));

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Augusta",
                                  "email": "updated@example.test",
                                  "city": "Paris"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ada"))
                .andExpect(jsonPath("$.password").doesNotExist());

        ArgumentCaptor<ProfileUpdateRequest> requestCaptor =
                ArgumentCaptor.forClass(ProfileUpdateRequest.class);
        verify(profileService).updateProfile(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getFirstName()).isEqualTo("Augusta");
        assertThat(requestCaptor.getValue().getEmail()).isEqualTo("updated@example.test");
        assertThat(requestCaptor.getValue().getCity()).isEqualTo("Paris");
    }

    @Test
    void updateCurrentProfilePreservesDuplicateEmailResponse() throws Exception {
        when(profileService.updateProfile(any(ProfileUpdateRequest.class)))
                .thenReturn(new ProfileService.ProfileUpdateResult(
                        false,
                        null,
                        ProfileService.EMAIL_IN_USE_MESSAGE
                ));

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "existing@example.test"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ProfileService.EMAIL_IN_USE_MESSAGE));
    }

    @Test
    void changePasswordPreservesPutRequestAndSuccessfulResponse() throws Exception {
        mockMvc.perform(put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "current-password",
                                  "newPassword": "replacement-password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(ProfileService.PASSWORD_CHANGED_MESSAGE));

        ArgumentCaptor<PasswordChangeRequest> requestCaptor =
                ArgumentCaptor.forClass(PasswordChangeRequest.class);
        verify(profileService).changePassword(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getCurrentPassword()).isEqualTo("current-password");
        assertThat(requestCaptor.getValue().getNewPassword()).isEqualTo("replacement-password");
    }

    @Test
    void changePasswordPreservesStableInvalidPasswordResponse() throws Exception {
        doThrow(new InvalidPasswordException("sensitive detail placeholder"))
                .when(profileService).changePassword(any(PasswordChangeRequest.class));

        mockMvc.perform(put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "incorrect-password",
                                  "newPassword": "replacement-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication failed"))
                .andExpect(jsonPath("$.timeStamp").isNumber());
    }

    private UserResponse representativeProfile() {
        return new UserResponse(
                7L,
                "Ada",
                "Lovelace",
                new Date(0L),
                "London",
                "United Kingdom",
                "https://example.test/ada.png",
                "Analytical Engines",
                "Programmer",
                "+44 20 0000 0000",
                "ada",
                "ada@example.test",
                new RoleResponse(ERole.ROLE_USER),
                true,
                new Date(1_700_000_000_000L),
                new Date(1_700_003_600_000L)
        );
    }
}
