package com.badereddine.demo.controller;

import com.badereddine.demo.exception.AccessDeniedException;
import com.badereddine.demo.exception.InvalidPasswordException;
import com.badereddine.demo.exception.UserImportException;
import com.badereddine.demo.exception.UserNotFoundException;
import com.badereddine.demo.model.ERole;
import com.badereddine.demo.model.User;
import com.badereddine.demo.payload.request.LoginRequest;
import com.badereddine.demo.payload.request.PasswordChangeRequest;
import com.badereddine.demo.payload.request.ProfileUpdateRequest;
import com.badereddine.demo.payload.request.SignupRequest;
import com.badereddine.demo.payload.response.GeneratedUserResponse;
import com.badereddine.demo.payload.response.JwtResponse;
import com.badereddine.demo.payload.response.MessageResponse;
import com.badereddine.demo.payload.response.UserListResponse;
import com.badereddine.demo.payload.response.UserResponse;
import com.badereddine.demo.payload.response.UserResponseMapper;
import com.badereddine.demo.security.services.UserDetailsImpl;
import com.badereddine.demo.service.AuthenticationService;
import com.badereddine.demo.service.CsvExportService;
import com.badereddine.demo.service.FakeDataService;
import com.badereddine.demo.service.ProfileService;
import com.badereddine.demo.service.UserImportService;
import com.badereddine.demo.service.UserPaginationPolicy;
import com.badereddine.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api")
public class UserController {
    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final ProfileService profileService;
    private final FakeDataService fakeDataService;
    private final UserResponseMapper userResponseMapper;
    private final UserImportService userImportService;
    private final UserPaginationPolicy userPaginationPolicy;
    private final CsvExportService csvExportService;

    public UserController(
            UserService userService,
            AuthenticationService authenticationService,
            ProfileService profileService,
            FakeDataService fakeDataService,
            UserResponseMapper userResponseMapper,
            UserImportService userImportService,
            UserPaginationPolicy userPaginationPolicy,
            CsvExportService csvExportService
    ) {
        this.userService = userService;
        this.authenticationService = authenticationService;
        this.profileService = profileService;
        this.fakeDataService = fakeDataService;
        this.userResponseMapper = userResponseMapper;
        this.userImportService = userImportService;
        this.userPaginationPolicy = userPaginationPolicy;
        this.csvExportService = csvExportService;
    }

    @GetMapping("/users/generate/{count}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> generateUsers(
            @PathVariable int count,
            @RequestParam(defaultValue = "0") int adminCount) throws IOException {
        try {
            List<GeneratedUserResponse> users = fakeDataService.generateFakeUsers(count, adminCount);
            byte[] json = new ObjectMapper().writeValueAsBytes(users);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.json")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(json);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(exception.getMessage()));
        }
    }

    @PostMapping("/users/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> batchUsers(@RequestParam("file") MultipartFile file) {
        try {
            UserImportService.UserImportResult result = userImportService.importUsers(file);
            Map<String, Integer> response = new HashMap<>();
            response.put("totalRecords", result.totalRecords());
            response.put("successfulImports", result.successfulImports());
            response.put("failedImports", result.failedImports());
            return ResponseEntity.ok(response);
        } catch (UserImportException exception) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(exception.getMessage()));
        }
    }

    @PostMapping("/auth")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest)
            throws UserNotFoundException, InvalidPasswordException {
        return ResponseEntity.ok(authenticationService.authenticate(loginRequest));
    }

    @GetMapping("/users/me")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getMyProfile() throws UserNotFoundException {
        return ResponseEntity.ok(profileService.getCurrentProfile());
    }

    @GetMapping("/users/{username}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserProfile(@PathVariable String username) throws UserNotFoundException, AccessDeniedException { Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        // Find the user in the database
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found!"));

        // Check if the authenticated user is allowed to access this profile
        if (!userDetails.getUsername().equals(username) && !userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            throw new AccessDeniedException("You are not allowed to access this profile!");
        }

        // Return the user details
        UserDetailsImpl userDetailsToReturn = UserDetailsImpl.build(user);

        return ResponseEntity.ok(userDetailsToReturn);
    }

    // ==================== NEW ENDPOINTS ====================

    /**
     * User Registration - Allow self-signup
     */
    @PostMapping("/auth/register")
    public ResponseEntity<MessageResponse> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        AuthenticationService.RegistrationResult result = authenticationService.register(signUpRequest);
        MessageResponse response = new MessageResponse(result.message());
        return result.successful()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    /**
     * Change Password - Authenticated users can change their password
     */
    @PutMapping("/users/me/password")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest request) throws UserNotFoundException, InvalidPasswordException {
        profileService.changePassword(request);
        return ResponseEntity.ok(new MessageResponse(ProfileService.PASSWORD_CHANGED_MESSAGE));
    }

    /**
     * Update Profile - Authenticated users can update their profile
     */
    @PutMapping("/users/me")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) throws UserNotFoundException {
        ProfileService.ProfileUpdateResult result = profileService.updateProfile(request);
        return result.successful()
                ? ResponseEntity.ok(result.profile())
                : ResponseEntity.badRequest().body(new MessageResponse(result.message()));
    }

    /**
     * List All Users - Admin only, with pagination and search
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "username") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search
    ) {
        Pageable pageable;
        try {
            pageable = userPaginationPolicy.create(page, size, sortBy, sortDir);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(exception.getMessage()));
        }

        Page<User> usersPage;

        if (search != null && !search.trim().isEmpty()) {
            usersPage = userService.searchUsers(search.trim(), pageable);
        } else {
            usersPage = userService.findAll(pageable);
        }

        List<UserResponse> users = usersPage.stream()
                .map(userResponseMapper::toResponse)
                .toList();

        return ResponseEntity.ok(new UserListResponse(
                users,
                usersPage.getNumber(),
                usersPage.getTotalElements(),
                usersPage.getTotalPages(),
                usersPage.getSize()
        ));
    }

    /**
     * Get User by ID - Admin only
     */
    @GetMapping("/users/id/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) throws UserNotFoundException {
        User user = userService.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        return ResponseEntity.ok(userResponseMapper.toResponse(user));
    }

    /**
     * Delete User - Admin only
     */
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) throws UserNotFoundException {
        User user = userService.deleteUser(id);
        return ResponseEntity.ok(new MessageResponse("User '" + user.getUsername() + "' deleted successfully"));
    }

    /**
     * Update User - Admin only (can edit any user)
     */
    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody ProfileUpdateRequest request) throws UserNotFoundException {
        User user = userService.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        // Check if new email is already in use by another user
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userService.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Email is already in use!"));
            }
            user.setEmail(request.getEmail());
        }

        // Update fields if provided
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
        return ResponseEntity.ok(userResponseMapper.toResponse(user));
    }

    /**
     * Change User Role - Admin only
     */
    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> changeUserRole(@PathVariable Long id, @RequestParam String role) throws UserNotFoundException {
        User user = userService.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        ERole newRole;
        try {
            newRole = ERole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Invalid role. Use ROLE_USER or ROLE_ADMIN"));
        }

        userService.changeRole(id, newRole);

        return ResponseEntity.ok(new MessageResponse("User role updated to " + newRole.name()));
    }

    /**
     * Toggle User Status (Enable/Disable) - Admin only
     */
    @PatchMapping("/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id, @RequestParam boolean enabled) throws UserNotFoundException {
        User user = userService.setEnabled(id, enabled);

        String status = enabled ? "enabled" : "disabled";
        return ResponseEntity.ok(new MessageResponse("User '" + user.getUsername() + "' has been " + status));
    }

    /**
     * Get User Stats - Admin only
     */
    @GetMapping("/stats/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userService.count());
        stats.put("totalAdmins", userService.countByRole(ERole.ROLE_ADMIN));
        stats.put("totalRegularUsers", userService.countByRole(ERole.ROLE_USER));
        stats.put("newUsersToday", userService.countNewUsersToday());

        return ResponseEntity.ok(stats);
    }

    /**
     * Export Users to CSV - Admin only
     */
    @GetMapping("/users/export/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportUsersToCsv(
            @RequestParam(required = false) String search,
            HttpServletResponse response) {

        Pageable exportPage = PageRequest.of(0, CsvExportService.MAX_EXPORT_ROWS);
        List<User> users;
        if (search != null && !search.trim().isEmpty()) {
            users = userService.searchUsers(search.trim(), exportPage).getContent();
        } else {
            users = userService.findAll(exportPage).getContent();
        }

        byte[] csvBytes = csvExportService.createCsv(users);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDispositionFormData("attachment", "users_export.csv");
        headers.setContentLength(csvBytes.length);

        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }
}
