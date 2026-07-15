package com.badereddine.demo.service;

import com.badereddine.demo.exception.AccessDeniedException;
import com.badereddine.demo.exception.UserNotFoundException;
import com.badereddine.demo.model.ERole;
import com.badereddine.demo.model.User;
import com.badereddine.demo.payload.request.ProfileUpdateRequest;
import com.badereddine.demo.payload.response.UserListResponse;
import com.badereddine.demo.payload.response.UserResponse;
import com.badereddine.demo.payload.response.UserResponseMapper;
import com.badereddine.demo.security.services.UserDetailsImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminUserService {
    public static final String EMAIL_IN_USE_MESSAGE = "Error: Email is already in use!";
    public static final String INVALID_ROLE_MESSAGE = "Error: Invalid role. Use ROLE_USER or ROLE_ADMIN";

    private final UserService userService;
    private final UserPaginationPolicy userPaginationPolicy;
    private final UserResponseMapper userResponseMapper;

    public AdminUserService(
            UserService userService,
            UserPaginationPolicy userPaginationPolicy,
            UserResponseMapper userResponseMapper
    ) {
        this.userService = userService;
        this.userPaginationPolicy = userPaginationPolicy;
        this.userResponseMapper = userResponseMapper;
    }

    @Transactional(readOnly = true)
    public UserDetailsImpl getUserProfile(String username) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl currentUser = (UserDetailsImpl) authentication.getPrincipal();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found!"));

        boolean isCurrentUser = currentUser.getUsername().equals(username);
        boolean isAdministrator = currentUser.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        if (!isCurrentUser && !isAdministrator) {
            throw new AccessDeniedException("You are not allowed to access this profile!");
        }

        return UserDetailsImpl.build(user);
    }

    @Transactional(readOnly = true)
    public UserListResponse getAllUsers(
            int page,
            int size,
            String sortBy,
            String sortDir,
            String search
    ) {
        Pageable pageable = userPaginationPolicy.create(page, size, sortBy, sortDir);
        Page<User> usersPage = hasSearchTerm(search)
                ? userService.searchUsers(search.trim(), pageable)
                : userService.findAll(pageable);
        List<UserResponse> users = usersPage.stream()
                .map(userResponseMapper::toResponse)
                .toList();

        return new UserListResponse(
                users,
                usersPage.getNumber(),
                usersPage.getTotalElements(),
                usersPage.getTotalPages(),
                usersPage.getSize()
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return userResponseMapper.toResponse(requireUser(id));
    }

    @Transactional
    public String deleteUser(Long id) {
        return userService.deleteUser(id).getUsername();
    }

    @Transactional
    public UserUpdateResult updateUser(Long id, ProfileUpdateRequest request) {
        User user = requireUser(id);

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userService.existsByEmail(request.getEmail())) {
                return UserUpdateResult.rejected(EMAIL_IN_USE_MESSAGE);
            }
            user.setEmail(request.getEmail());
        }

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getBirthDate() != null) user.setBirthDate(request.getBirthDate());
        if (request.getCity() != null) user.setCity(request.getCity());
        if (request.getCountry() != null) user.setCountry(request.getCountry());
        if (request.getCompany() != null) user.setCompany(request.getCompany());
        if (request.getJobPosition() != null) user.setJobPosition(request.getJobPosition());
        if (request.getMobile() != null) user.setMobile(request.getMobile());
        if (request.getAvatar() != null) user.setAvatar(request.getAvatar());

        userService.save(user);
        return UserUpdateResult.updated(userResponseMapper.toResponse(user));
    }

    @Transactional
    public ERole changeUserRole(Long id, String role) {
        requireUser(id);

        ERole newRole;
        try {
            newRole = ERole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(INVALID_ROLE_MESSAGE);
        }

        userService.changeRole(id, newRole);
        return newRole;
    }

    @Transactional
    public String setUserEnabled(Long id, boolean enabled) {
        return userService.setEnabled(id, enabled).getUsername();
    }

    private User requireUser(Long id) {
        return userService.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    private boolean hasSearchTerm(String search) {
        return search != null && !search.trim().isEmpty();
    }

    public record UserUpdateResult(boolean successful, UserResponse user, String message) {
        private static UserUpdateResult updated(UserResponse user) {
            return new UserUpdateResult(true, user, null);
        }

        private static UserUpdateResult rejected(String message) {
            return new UserUpdateResult(false, null, message);
        }
    }
}
