package com.badereddine.demo.service;

import com.badereddine.demo.exception.UserNotFoundException;
import com.badereddine.demo.model.ERole;
import com.badereddine.demo.model.Role;
import com.badereddine.demo.model.User;
import com.badereddine.demo.payload.request.LoginRequest;
import com.badereddine.demo.payload.request.SignupRequest;
import com.badereddine.demo.payload.response.JwtResponse;
import com.badereddine.demo.security.jwt.JwtUtils;
import com.badereddine.demo.security.services.UserDetailsImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Date;
import java.util.List;

@Service
public class AuthenticationService {
    public static final String USERNAME_TAKEN_MESSAGE = "Error: Username is already taken!";
    public static final String EMAIL_IN_USE_MESSAGE = "Error: Email is already in use!";
    public static final String REGISTRATION_SUCCESS_MESSAGE = "User registered successfully!";

    private final UserService userService;
    private final RoleService roleService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AuthenticationService(
            UserService userService,
            RoleService roleService,
            AuthenticationManager authenticationManager,
            JwtUtils jwtUtils,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        this.userService = userService;
        this.roleService = roleService;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public JwtResponse authenticate(LoginRequest loginRequest) {
        User user = userService.findByUsernameOrEmail(loginRequest.getUsername(), loginRequest.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found!"));

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        user.setLastLogin(Date.from(clock.instant()));
        userService.save(user);

        String jwt = jwtUtils.generateJwtToken(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList();

        return new JwtResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles
        );
    }

    @Transactional
    public RegistrationResult register(SignupRequest signUpRequest) {
        if (userService.existsByUsername(signUpRequest.getUsername())) {
            return RegistrationResult.rejected(USERNAME_TAKEN_MESSAGE);
        }
        if (userService.existsByEmail(signUpRequest.getEmail())) {
            return RegistrationResult.rejected(EMAIL_IN_USE_MESSAGE);
        }

        Role userRole = roleService.findByName(ERole.ROLE_USER)
                .orElseGet(() -> roleService.save(new Role(ERole.ROLE_USER)));

        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setFirstName(signUpRequest.getFirstName());
        user.setLastName(signUpRequest.getLastName());
        user.setRole(userRole);
        user.setBirthDate(Date.from(clock.instant()));
        user.setCity("Not specified");
        user.setCountry("Not specified");
        user.setCompany("Not specified");
        user.setJobPosition("Not specified");
        user.setMobile("+212 000000000");
        user.setAvatar("https://ui-avatars.com/api/?name=" + signUpRequest.getFirstName() + "+"
                + signUpRequest.getLastName() + "&background=6366F1&color=fff");

        userService.save(user);
        return RegistrationResult.registered();
    }

    public record RegistrationResult(boolean successful, String message) {
        private static RegistrationResult registered() {
            return new RegistrationResult(true, REGISTRATION_SUCCESS_MESSAGE);
        }

        private static RegistrationResult rejected(String message) {
            return new RegistrationResult(false, message);
        }
    }
}
