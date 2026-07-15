package com.badereddine.demo.controller;

import com.badereddine.demo.exception.AccessDeniedException;
import com.badereddine.demo.exception.InvalidPasswordException;
import com.badereddine.demo.exception.UserImportException;
import com.badereddine.demo.exception.UserNotFoundException;
import com.badereddine.demo.model.ERole;
import com.badereddine.demo.model.Role;
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
import com.badereddine.demo.security.jwt.JwtUtils;
import com.badereddine.demo.security.services.UserDetailsImpl;
import com.badereddine.demo.service.FakeDataService;
import com.badereddine.demo.service.RoleService;
import com.badereddine.demo.service.UserImportService;
import com.badereddine.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private FakeDataService fakeDataService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserResponseMapper userResponseMapper;

    @Autowired
    private UserImportService userImportService;

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
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) throws UserNotFoundException, InvalidPasswordException {
        // Check if a user with the provided username or email exists
        User user = userService.findByUsernameOrEmail(loginRequest.getUsername(), loginRequest.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found!"));

        // Authenticate the user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), loginRequest.getPassword()));

        // Set the authentication object in the SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Update last login time
        user.setLastLogin(new Date());
        userService.save(user);

        // Generate a JWT token
        String jwt = jwtUtils.generateJwtToken(authentication);

        // Get the roles of the authenticated user
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        // Return the JWT token and user details
        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles));
    }

    @GetMapping("/users/me")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getMyProfile() throws UserNotFoundException {
        // Fetch the user details from the SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Get the UserDetailsImpl object from the authentication object
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Find the user in the database
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found!"));

        return ResponseEntity.ok(userResponseMapper.toResponse(user));
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
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        // Check if username is already taken
        if (userService.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        // Check if email is already in use
        if (userService.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Get the default USER role
        Role userRole = roleService.findByName(ERole.ROLE_USER)
                .orElseGet(() -> roleService.save(new Role(ERole.ROLE_USER)));

        // Create new user
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setFirstName(signUpRequest.getFirstName());
        user.setLastName(signUpRequest.getLastName());
        user.setRole(userRole);

        // Set default values for required fields
        user.setBirthDate(new Date());
        user.setCity("Not specified");
        user.setCountry("Not specified");
        user.setCompany("Not specified");
        user.setJobPosition("Not specified");
        user.setMobile("+212 000000000");
        user.setAvatar("https://ui-avatars.com/api/?name=" + signUpRequest.getFirstName() + "+" + signUpRequest.getLastName() + "&background=6366F1&color=fff");

        userService.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    /**
     * Change Password - Authenticated users can change their password
     */
    @PutMapping("/users/me/password")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest request) throws UserNotFoundException, InvalidPasswordException {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Find user in database
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found!"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Current password is incorrect!");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userService.save(user);

        return ResponseEntity.ok(new MessageResponse("Password changed successfully!"));
    }

    /**
     * Update Profile - Authenticated users can update their profile
     */
    @PutMapping("/users/me")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) throws UserNotFoundException {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Find user in database
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found!"));

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
     * List All Users - Admin only, with pagination and search
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserListResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "username") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
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
        User user = userService.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        userService.deleteById(id);
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

        Role roleEntity = roleService.findByName(newRole)
                .orElseGet(() -> roleService.save(new Role(newRole)));

        user.setRole(roleEntity);
        userService.save(user);

        return ResponseEntity.ok(new MessageResponse("User role updated to " + newRole.name()));
    }

    /**
     * Toggle User Status (Enable/Disable) - Admin only
     */
    @PatchMapping("/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id, @RequestParam boolean enabled) throws UserNotFoundException {
        User user = userService.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        user.setEnabled(enabled);
        userService.save(user);

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

        List<User> users;
        if (search != null && !search.trim().isEmpty()) {
            users = userService.searchUsers(search.trim(), PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        } else {
            users = userService.findAll(PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        }

        StringBuilder csv = new StringBuilder();

        // CSV Header
        csv.append("ID,Username,Email,First Name,Last Name,Company,Job Position,City,Country,Mobile,Role,Status,Created At,Last Login\n");

        // CSV Data
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (User user : users) {
            csv.append(escapeCsv(user.getId() != null ? user.getId().toString() : "")).append(",");
            csv.append(escapeCsv(user.getUsername())).append(",");
            csv.append(escapeCsv(user.getEmail())).append(",");
            csv.append(escapeCsv(user.getFirstName())).append(",");
            csv.append(escapeCsv(user.getLastName())).append(",");
            csv.append(escapeCsv(user.getCompany())).append(",");
            csv.append(escapeCsv(user.getJobPosition())).append(",");
            csv.append(escapeCsv(user.getCity())).append(",");
            csv.append(escapeCsv(user.getCountry())).append(",");
            csv.append(escapeCsv(user.getMobile())).append(",");
            csv.append(escapeCsv(user.getRole() != null ? user.getRole().getName().name() : "")).append(",");
            csv.append(user.isEnabled() ? "Active" : "Disabled").append(",");
            csv.append(user.getCreatedAt() != null ? dateFormat.format(user.getCreatedAt()) : "").append(",");
            csv.append(user.getLastLogin() != null ? dateFormat.format(user.getLastLogin()) : "Never").append("\n");
        }

        byte[] csvBytes = csv.toString().getBytes();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "users_export.csv");
        headers.setContentLength(csvBytes.length);

        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        // Escape quotes and wrap in quotes if contains comma, quote, or newline
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
