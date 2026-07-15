package com.badereddine.demo.controller;

import com.badereddine.demo.exception.InvalidPasswordException;
import com.badereddine.demo.exception.UserNotFoundException;
import com.badereddine.demo.payload.request.LoginRequest;
import com.badereddine.demo.payload.request.SignupRequest;
import com.badereddine.demo.payload.response.JwtResponse;
import com.badereddine.demo.payload.response.MessageResponse;
import com.badereddine.demo.service.AuthenticationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Authentication")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/auth")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest)
            throws UserNotFoundException, InvalidPasswordException {
        return ResponseEntity.ok(authenticationService.authenticate(loginRequest));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<MessageResponse> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        AuthenticationService.RegistrationResult result = authenticationService.register(signUpRequest);
        MessageResponse response = new MessageResponse(result.message());
        return result.successful()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }
}
