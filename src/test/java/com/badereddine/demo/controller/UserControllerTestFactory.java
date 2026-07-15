package com.badereddine.demo.controller;

import com.badereddine.demo.payload.response.UserResponseMapper;
import com.badereddine.demo.security.jwt.JwtUtils;
import com.badereddine.demo.service.CsvExportService;
import com.badereddine.demo.service.FakeDataService;
import com.badereddine.demo.service.RoleService;
import com.badereddine.demo.service.UserImportService;
import com.badereddine.demo.service.UserPaginationPolicy;
import com.badereddine.demo.service.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.Mockito.mock;

final class UserControllerTestFactory {

    private UserControllerTestFactory() {
    }

    static Builder builder() {
        return new Builder();
    }

    static final class Builder {
        private UserService userService = mock(UserService.class);
        private RoleService roleService = mock(RoleService.class);
        private AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        private JwtUtils jwtUtils = mock(JwtUtils.class);
        private FakeDataService fakeDataService = new FakeDataService();
        private PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        private UserResponseMapper userResponseMapper = new UserResponseMapper();
        private UserImportService userImportService = mock(UserImportService.class);
        private UserPaginationPolicy userPaginationPolicy = new UserPaginationPolicy();
        private CsvExportService csvExportService = new CsvExportService();

        Builder userService(UserService value) {
            userService = value;
            return this;
        }

        Builder fakeDataService(FakeDataService value) {
            fakeDataService = value;
            return this;
        }

        Builder userImportService(UserImportService value) {
            userImportService = value;
            return this;
        }

        UserController build() {
            return new UserController(
                    userService,
                    roleService,
                    authenticationManager,
                    jwtUtils,
                    fakeDataService,
                    passwordEncoder,
                    userResponseMapper,
                    userImportService,
                    userPaginationPolicy,
                    csvExportService
            );
        }
    }
}
