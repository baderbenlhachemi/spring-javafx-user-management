package com.badereddine.demo.controller;

import com.badereddine.demo.exception.InvalidPasswordException;
import com.badereddine.demo.exception.UserNotFoundException;
import com.badereddine.demo.payload.request.PasswordChangeRequest;
import com.badereddine.demo.payload.request.ProfileUpdateRequest;
import com.badereddine.demo.payload.response.MessageResponse;
import com.badereddine.demo.payload.response.UserResponse;
import com.badereddine.demo.service.AdminUserService;
import com.badereddine.demo.service.ProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Profiles")
public class ProfileController {

    private final ProfileService profileService;
    private final AdminUserService adminUserService;

    public ProfileController(ProfileService profileService, AdminUserService adminUserService) {
        this.profileService = profileService;
        this.adminUserService = adminUserService;
    }

    @GetMapping("/users/me")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getMyProfile() throws UserNotFoundException {
        return ResponseEntity.ok(profileService.getCurrentProfile());
    }

    @GetMapping("/users/{username}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserProfile(@PathVariable String username) {
        return ResponseEntity.ok(adminUserService.getUserProfile(username));
    }

    @PutMapping("/users/me/password")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest request)
            throws UserNotFoundException, InvalidPasswordException {
        profileService.changePassword(request);
        return ResponseEntity.ok(new MessageResponse(ProfileService.PASSWORD_CHANGED_MESSAGE));
    }

    @PutMapping("/users/me")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody ProfileUpdateRequest request)
            throws UserNotFoundException {
        ProfileService.ProfileUpdateResult result = profileService.updateProfile(request);
        return result.successful()
                ? ResponseEntity.ok(result.profile())
                : ResponseEntity.badRequest().body(new MessageResponse(result.message()));
    }
}
