package com.badereddine.demo.service;

import com.badereddine.demo.exception.InvalidPasswordException;
import com.badereddine.demo.exception.UserNotFoundException;
import com.badereddine.demo.model.User;
import com.badereddine.demo.payload.request.PasswordChangeRequest;
import com.badereddine.demo.payload.request.ProfileUpdateRequest;
import com.badereddine.demo.payload.response.UserResponse;
import com.badereddine.demo.payload.response.UserResponseMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
    public static final String EMAIL_IN_USE_MESSAGE = "Error: Email is already in use!";
    public static final String PASSWORD_CHANGED_MESSAGE = "Password changed successfully!";

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserResponseMapper userResponseMapper;

    public ProfileService(
            UserService userService,
            PasswordEncoder passwordEncoder,
            UserResponseMapper userResponseMapper
    ) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.userResponseMapper = userResponseMapper;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentProfile() {
        return userResponseMapper.toResponse(currentUser());
    }

    @Transactional
    public void changePassword(PasswordChangeRequest request) {
        User user = currentUser();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Current password is incorrect!");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userService.save(user);
    }

    @Transactional
    public ProfileUpdateResult updateProfile(ProfileUpdateRequest request) {
        User user = currentUser();

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userService.existsByEmail(request.getEmail())) {
                return ProfileUpdateResult.rejected(EMAIL_IN_USE_MESSAGE);
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
        return ProfileUpdateResult.updated(userResponseMapper.toResponse(user));
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found!"));
    }

    public record ProfileUpdateResult(boolean successful, UserResponse profile, String message) {
        private static ProfileUpdateResult updated(UserResponse profile) {
            return new ProfileUpdateResult(true, profile, null);
        }

        private static ProfileUpdateResult rejected(String message) {
            return new ProfileUpdateResult(false, null, message);
        }
    }
}
