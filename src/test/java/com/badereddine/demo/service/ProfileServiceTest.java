package com.badereddine.demo.service;

import com.badereddine.demo.exception.InvalidPasswordException;
import com.badereddine.demo.exception.UserNotFoundException;
import com.badereddine.demo.model.ERole;
import com.badereddine.demo.model.Role;
import com.badereddine.demo.model.User;
import com.badereddine.demo.payload.request.PasswordChangeRequest;
import com.badereddine.demo.payload.request.ProfileUpdateRequest;
import com.badereddine.demo.payload.response.UserResponse;
import com.badereddine.demo.payload.response.UserResponseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private ProfileService profileService;
    private User user;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(userService, passwordEncoder, new UserResponseMapper());
        user = representativeUser();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ada", null)
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getsCurrentProfileFromAuthenticatedUsernameAsRedactedDto() throws Exception {
        when(userService.findByUsername("ada")).thenReturn(Optional.of(user));

        UserResponse response = profileService.getCurrentProfile();

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.username()).isEqualTo("ada");
        assertThat(response.email()).isEqualTo("ada@example.test");
        assertThat(response.role().name()).isEqualTo(ERole.ROLE_USER);
        assertTransactional("getCurrentProfile", true);
        verify(userService).findByUsername("ada");
    }

    @Test
    void currentProfileRejectsMissingAuthenticatedUser() {
        when(userService.findByUsername("ada")).thenReturn(Optional.empty());

        assertThatThrownBy(profileService::getCurrentProfile)
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void changesPasswordOnlyAfterServerSideVerificationAndEncoding() throws Exception {
        PasswordChangeRequest request = passwordChangeRequest();
        when(userService.findByUsername("ada")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-password", "stored-password-placeholder")).thenReturn(true);
        when(passwordEncoder.encode("replacement-password")).thenReturn("replacement-hash-placeholder");

        profileService.changePassword(request);

        assertThat(user.getPassword()).isEqualTo("replacement-hash-placeholder");
        verify(passwordEncoder).matches("current-password", "stored-password-placeholder");
        verify(passwordEncoder).encode("replacement-password");
        verify(userService).save(user);
        assertTransactional("changePassword", false, PasswordChangeRequest.class);
    }

    @Test
    void rejectsIncorrectCurrentPasswordWithoutEncodingOrPersisting() {
        PasswordChangeRequest request = passwordChangeRequest();
        when(userService.findByUsername("ada")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-password", "stored-password-placeholder")).thenReturn(false);

        assertThatThrownBy(() -> profileService.changePassword(request))
                .isInstanceOf(InvalidPasswordException.class);

        verify(passwordEncoder, never()).encode(any());
        verify(userService, never()).save(any());
        assertThat(user.getPassword()).isEqualTo("stored-password-placeholder");
    }

    @Test
    void updatesEveryProvidedProfileFieldAndReturnsCompatibleDto() throws Exception {
        Date updatedBirthDate = new Date(946_684_800_000L);
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setEmail("updated@example.test");
        request.setFirstName("Augusta");
        request.setLastName("King");
        request.setBirthDate(updatedBirthDate);
        request.setCity("Paris");
        request.setCountry("France");
        request.setCompany("Engines Ltd");
        request.setJobPosition("Lead Programmer");
        request.setMobile("+33 100000000");
        request.setAvatar("https://example.test/updated.png");
        when(userService.findByUsername("ada")).thenReturn(Optional.of(user));
        when(userService.existsByEmail("updated@example.test")).thenReturn(false);

        ProfileService.ProfileUpdateResult result = profileService.updateProfile(request);

        assertThat(result.successful()).isTrue();
        assertThat(result.message()).isNull();
        assertThat(result.profile().email()).isEqualTo("updated@example.test");
        assertThat(result.profile().firstName()).isEqualTo("Augusta");
        assertThat(result.profile().lastName()).isEqualTo("King");
        assertThat(user.getBirthDate()).isEqualTo(updatedBirthDate);
        assertThat(user.getCity()).isEqualTo("Paris");
        assertThat(user.getCountry()).isEqualTo("France");
        assertThat(user.getCompany()).isEqualTo("Engines Ltd");
        assertThat(user.getJobPosition()).isEqualTo("Lead Programmer");
        assertThat(user.getMobile()).isEqualTo("+33 100000000");
        assertThat(user.getAvatar()).isEqualTo("https://example.test/updated.png");
        verify(userService).save(user);
        assertTransactional("updateProfile", false, ProfileUpdateRequest.class);
    }

    @Test
    void rejectsDuplicateEmailBeforeApplyingAnyProfileChanges() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setEmail("existing@example.test");
        request.setFirstName("Should not be applied");
        when(userService.findByUsername("ada")).thenReturn(Optional.of(user));
        when(userService.existsByEmail("existing@example.test")).thenReturn(true);

        ProfileService.ProfileUpdateResult result = profileService.updateProfile(request);

        assertThat(result.successful()).isFalse();
        assertThat(result.profile()).isNull();
        assertThat(result.message()).isEqualTo(ProfileService.EMAIL_IN_USE_MESSAGE);
        assertThat(user.getEmail()).isEqualTo("ada@example.test");
        assertThat(user.getFirstName()).isEqualTo("Ada");
        verify(userService, never()).save(any());
    }

    @Test
    void sameEmailDoesNotRunDuplicateCheck() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setEmail("ada@example.test");
        request.setCity("Oxford");
        when(userService.findByUsername("ada")).thenReturn(Optional.of(user));

        ProfileService.ProfileUpdateResult result = profileService.updateProfile(request);

        assertThat(result.successful()).isTrue();
        assertThat(result.profile().city()).isEqualTo("Oxford");
        verify(userService, never()).existsByEmail(any());
        verify(userService).save(user);
        verifyNoInteractions(passwordEncoder);
    }

    private void assertTransactional(String methodName, boolean readOnly, Class<?>... parameterTypes)
            throws Exception {
        Method method = ProfileService.class.getMethod(methodName, parameterTypes);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isEqualTo(readOnly);
    }

    private PasswordChangeRequest passwordChangeRequest() {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword("current-password");
        request.setNewPassword("replacement-password");
        return request;
    }

    private User representativeUser() {
        User value = new User();
        value.setId(7L);
        value.setFirstName("Ada");
        value.setLastName("Lovelace");
        value.setBirthDate(new Date(0L));
        value.setCity("London");
        value.setCountry("United Kingdom");
        value.setAvatar("https://example.test/ada.png");
        value.setCompany("Analytical Engines");
        value.setJobPosition("Programmer");
        value.setMobile("+44 20 0000 0000");
        value.setUsername("ada");
        value.setEmail("ada@example.test");
        value.setPassword("stored-password-placeholder");
        value.setRole(new Role(ERole.ROLE_USER));
        value.setEnabled(true);
        value.setCreatedAt(new Date(1_700_000_000_000L));
        value.setLastLogin(new Date(1_700_003_600_000L));
        return value;
    }
}
