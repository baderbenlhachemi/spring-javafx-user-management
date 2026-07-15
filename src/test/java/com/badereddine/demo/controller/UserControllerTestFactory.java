package com.badereddine.demo.controller;

import com.badereddine.demo.service.AdminUserService;
import com.badereddine.demo.service.AuthenticationService;
import com.badereddine.demo.service.CsvExportService;
import com.badereddine.demo.service.FakeDataService;
import com.badereddine.demo.service.ProfileService;
import com.badereddine.demo.service.UserImportService;
import com.badereddine.demo.service.UserService;
import com.badereddine.demo.service.UserStatisticsService;

import static org.mockito.Mockito.mock;

final class UserControllerTestFactory {

    private UserControllerTestFactory() {
    }

    static Builder builder() {
        return new Builder();
    }

    static final class Builder {
        private UserService userService = mock(UserService.class);
        private AuthenticationService authenticationService = mock(AuthenticationService.class);
        private ProfileService profileService = mock(ProfileService.class);
        private AdminUserService adminUserService = mock(AdminUserService.class);
        private UserStatisticsService userStatisticsService = mock(UserStatisticsService.class);
        private FakeDataService fakeDataService = new FakeDataService();
        private UserImportService userImportService = mock(UserImportService.class);
        private CsvExportService csvExportService = new CsvExportService();

        Builder userService(UserService value) {
            userService = value;
            return this;
        }

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
                    authenticationService,
                    profileService,
                    adminUserService,
                    userStatisticsService,
                    fakeDataService,
                    userImportService,
                    csvExportService
            );
        }
    }
}
