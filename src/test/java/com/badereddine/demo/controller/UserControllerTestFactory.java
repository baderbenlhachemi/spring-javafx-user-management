package com.badereddine.demo.controller;

import com.badereddine.demo.service.AdminUserService;
import com.badereddine.demo.service.AuthenticationService;
import com.badereddine.demo.service.ProfileService;
import com.badereddine.demo.service.UserStatisticsService;
import com.badereddine.demo.service.UserTransferService;

import static org.mockito.Mockito.mock;

final class UserControllerTestFactory {

    private UserControllerTestFactory() {
    }

    static Builder builder() {
        return new Builder();
    }

    static final class Builder {
        private AuthenticationService authenticationService = mock(AuthenticationService.class);
        private ProfileService profileService = mock(ProfileService.class);
        private AdminUserService adminUserService = mock(AdminUserService.class);
        private UserStatisticsService userStatisticsService = mock(UserStatisticsService.class);
        private UserTransferService userTransferService = mock(UserTransferService.class);

        Builder profileService(ProfileService value) {
            profileService = value;
            return this;
        }

        Builder adminUserService(AdminUserService value) {
            adminUserService = value;
            return this;
        }

        Builder userStatisticsService(UserStatisticsService value) {
            userStatisticsService = value;
            return this;
        }

        Builder userTransferService(UserTransferService value) {
            userTransferService = value;
            return this;
        }

        UserController build() {
            return new UserController(
                    authenticationService,
                    profileService,
                    adminUserService,
                    userStatisticsService,
                    userTransferService
            );
        }
    }
}
