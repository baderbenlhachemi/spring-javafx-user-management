package com.badereddine.demo.service;

import com.badereddine.demo.exception.AccessDeniedException;
import com.badereddine.demo.exception.UserNotFoundException;
import com.badereddine.demo.model.ERole;
import com.badereddine.demo.model.Role;
import com.badereddine.demo.model.User;
import com.badereddine.demo.payload.request.ProfileUpdateRequest;
import com.badereddine.demo.payload.response.UserListResponse;
import com.badereddine.demo.payload.response.UserResponse;
import com.badereddine.demo.payload.response.UserResponseMapper;
import com.badereddine.demo.security.services.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserService userService;

    private UserResponseMapper userResponseMapper;
    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        userResponseMapper = new UserResponseMapper();
        adminUserService = new AdminUserService(
                userService,
                new UserPaginationPolicy(),
                userResponseMapper
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listsMappedUsersWithCompatibleDefaultsAndPaginationMetadata() {
        User user = user(7L, "ada", ERole.ROLE_ADMIN);
        when(userService.findAll(any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(user), invocation.getArgument(0), 12));

        UserListResponse response = adminUserService.getAllUsers(0, 10, "username", "asc", null);

        assertThat(response.users()).containsExactly(userResponseMapper.toResponse(user));
        assertThat(response.currentPage()).isZero();
        assertThat(response.totalItems()).isEqualTo(12);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(10);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userService).findAll(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("username").isAscending()).isTrue();
        verify(userService, never()).searchUsers(any(), any());
    }

    @Test
    void trimsSearchAndUsesRequestedPaginationAndSorting() {
        when(userService.searchUsers(any(), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(1), 0));

        adminUserService.getAllUsers(2, 25, "lastLogin", "desc", "  ada  ");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userService).searchUsers(org.mockito.ArgumentMatchers.eq("ada"), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(25);
        assertThat(pageable.getSort().getOrderFor("lastLogin").isDescending()).isTrue();
    }

    @Test
    void rejectsInvalidPaginationBeforeQueryingUsers() {
        assertThatThrownBy(() -> adminUserService.getAllUsers(-1, 10, "username", "asc", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(UserPaginationPolicy.INVALID_PAGE_MESSAGE);

        verifyNoInteractions(userService);
    }

    @Test
    void allowsUsersToReadTheirOwnProfile() {
        User user = user(7L, "ada", ERole.ROLE_USER);
        authenticate(user);
        when(userService.findByUsername("ada")).thenReturn(Optional.of(user));

        UserDetailsImpl result = adminUserService.getUserProfile("ada");

        assertThat(result.getUsername()).isEqualTo("ada");
        assertThat(result.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    void allowsAdministratorsToReadAnotherProfile() {
        User administrator = user(1L, "admin", ERole.ROLE_ADMIN);
        User target = user(7L, "ada", ERole.ROLE_USER);
        authenticate(administrator);
        when(userService.findByUsername("ada")).thenReturn(Optional.of(target));

        assertThat(adminUserService.getUserProfile("ada").getUsername()).isEqualTo("ada");
    }

    @Test
    void rejectsMembersReadingAnotherProfile() {
        User member = user(1L, "member", ERole.ROLE_USER);
        User target = user(7L, "ada", ERole.ROLE_USER);
        authenticate(member);
        when(userService.findByUsername("ada")).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> adminUserService.getUserProfile("ada"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You are not allowed to access this profile!");
    }

    @Test
    void mapsIdLookupAndPreservesNotFoundMessage() {
        User user = user(7L, "ada", ERole.ROLE_ADMIN);
        when(userService.findById(7L)).thenReturn(Optional.of(user));
        when(userService.findById(8L)).thenReturn(Optional.empty());

        assertThat(adminUserService.getUserById(7L)).isEqualTo(userResponseMapper.toResponse(user));
        assertThatThrownBy(() -> adminUserService.getUserById(8L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with id: 8");
    }

    @Test
    void updatesEveryProvidedAdministrativeProfileField() {
        User user = user(7L, "ada", ERole.ROLE_USER);
        Date birthDate = new Date(1234L);
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setEmail("new@example.test");
        request.setFirstName("Augusta");
        request.setLastName("King");
        request.setBirthDate(birthDate);
        request.setCity("Paris");
        request.setCountry("France");
        request.setCompany("Engines");
        request.setJobPosition("Analyst");
        request.setMobile("12345");
        request.setAvatar("avatar.png");
        when(userService.findById(7L)).thenReturn(Optional.of(user));
        when(userService.existsByEmail("new@example.test")).thenReturn(false);

        AdminUserService.UserUpdateResult result = adminUserService.updateUser(7L, request);

        assertThat(result.successful()).isTrue();
        assertThat(result.user()).isEqualTo(userResponseMapper.toResponse(user));
        assertThat(user.getEmail()).isEqualTo("new@example.test");
        assertThat(user.getFirstName()).isEqualTo("Augusta");
        assertThat(user.getLastName()).isEqualTo("King");
        assertThat(user.getBirthDate()).isEqualTo(birthDate);
        assertThat(user.getCity()).isEqualTo("Paris");
        assertThat(user.getCountry()).isEqualTo("France");
        assertThat(user.getCompany()).isEqualTo("Engines");
        assertThat(user.getJobPosition()).isEqualTo("Analyst");
        assertThat(user.getMobile()).isEqualTo("12345");
        assertThat(user.getAvatar()).isEqualTo("avatar.png");
        verify(userService).save(user);
    }

    @Test
    void rejectsDuplicateAdministrativeEmailWithoutSaving() {
        User user = user(7L, "ada", ERole.ROLE_USER);
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setEmail("used@example.test");
        when(userService.findById(7L)).thenReturn(Optional.of(user));
        when(userService.existsByEmail("used@example.test")).thenReturn(true);

        AdminUserService.UserUpdateResult result = adminUserService.updateUser(7L, request);

        assertThat(result.successful()).isFalse();
        assertThat(result.user()).isNull();
        assertThat(result.message()).isEqualTo(AdminUserService.EMAIL_IN_USE_MESSAGE);
        verify(userService, never()).save(any());
    }

    @Test
    void delegatesDeleteRoleAndStatusMutations() {
        User user = user(7L, "ada", ERole.ROLE_ADMIN);
        when(userService.deleteUser(7L)).thenReturn(user);
        when(userService.findById(7L)).thenReturn(Optional.of(user));
        when(userService.setEnabled(7L, false)).thenReturn(user);

        assertThat(adminUserService.deleteUser(7L)).isEqualTo("ada");
        assertThat(adminUserService.changeUserRole(7L, "role_user")).isEqualTo(ERole.ROLE_USER);
        assertThat(adminUserService.setUserEnabled(7L, false)).isEqualTo("ada");

        verify(userService).deleteUser(7L);
        verify(userService).changeRole(7L, ERole.ROLE_USER);
        verify(userService).setEnabled(7L, false);
    }

    @Test
    void checksUserExistenceBeforeReturningInvalidRoleResponse() {
        User user = user(7L, "ada", ERole.ROLE_ADMIN);
        when(userService.findById(7L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> adminUserService.changeUserRole(7L, "owner"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(AdminUserService.INVALID_ROLE_MESSAGE);
        verify(userService, never()).changeRole(any(), any());

        when(userService.findById(8L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> adminUserService.changeUserRole(8L, "owner"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with id: 8");
    }

    @Test
    void declaresReadAndMutationTransactionBoundaries() throws Exception {
        assertReadOnlyTransaction("getUserProfile", String.class);
        assertReadOnlyTransaction("getAllUsers", int.class, int.class, String.class, String.class, String.class);
        assertReadOnlyTransaction("getUserById", Long.class);
        assertMutationTransaction("deleteUser", Long.class);
        assertMutationTransaction("updateUser", Long.class, ProfileUpdateRequest.class);
        assertMutationTransaction("changeUserRole", Long.class, String.class);
        assertMutationTransaction("setUserEnabled", Long.class, boolean.class);
    }

    private void authenticate(User user) {
        UserDetailsImpl principal = UserDetailsImpl.build(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private User user(Long id, String username, ERole roleName) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.test");
        user.setPassword("encoded-password");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(new Role(roleName));
        user.setEnabled(true);
        return user;
    }

    private void assertReadOnlyTransaction(String methodName, Class<?>... parameterTypes) throws Exception {
        Transactional transactional = transaction(methodName, parameterTypes);
        assertThat(transactional.readOnly()).isTrue();
    }

    private void assertMutationTransaction(String methodName, Class<?>... parameterTypes) throws Exception {
        Transactional transactional = transaction(methodName, parameterTypes);
        assertThat(transactional.readOnly()).isFalse();
    }

    private Transactional transaction(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = AdminUserService.class.getMethod(methodName, parameterTypes);
        assertThat(method.isAnnotationPresent(Transactional.class)).isTrue();
        return method.getAnnotation(Transactional.class);
    }
}
