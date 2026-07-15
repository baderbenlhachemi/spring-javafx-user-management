package com.badereddine.demo.controller;

import com.badereddine.demo.exception.UserNotFoundException;
import com.badereddine.demo.payload.request.ProfileUpdateRequest;
import com.badereddine.demo.payload.response.MessageResponse;
import com.badereddine.demo.payload.response.UserResponse;
import com.badereddine.demo.service.AdminUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "User Administration")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "username") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search
    ) {
        try {
            return ResponseEntity.ok(adminUserService.getAllUsers(page, size, sortBy, sortDir, search));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(exception.getMessage()));
        }
    }

    @GetMapping("/users/id/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) throws UserNotFoundException {
        return ResponseEntity.ok(adminUserService.getUserById(id));
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) throws UserNotFoundException {
        String username = adminUserService.deleteUser(id);
        return ResponseEntity.ok(new MessageResponse("User '" + username + "' deleted successfully"));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestBody ProfileUpdateRequest request
    ) throws UserNotFoundException {
        AdminUserService.UserUpdateResult result = adminUserService.updateUser(id, request);
        return result.successful()
                ? ResponseEntity.ok(result.user())
                : ResponseEntity.badRequest().body(new MessageResponse(result.message()));
    }

    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> changeUserRole(
            @PathVariable Long id,
            @RequestParam String role
    ) throws UserNotFoundException {
        try {
            String newRole = adminUserService.changeUserRole(id, role).name();
            return ResponseEntity.ok(new MessageResponse("User role updated to " + newRole));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(exception.getMessage()));
        }
    }

    @PatchMapping("/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleUserStatus(
            @PathVariable Long id,
            @RequestParam boolean enabled
    ) throws UserNotFoundException {
        String username = adminUserService.setUserEnabled(id, enabled);
        String status = enabled ? "enabled" : "disabled";
        return ResponseEntity.ok(new MessageResponse("User '" + username + "' has been " + status));
    }
}
