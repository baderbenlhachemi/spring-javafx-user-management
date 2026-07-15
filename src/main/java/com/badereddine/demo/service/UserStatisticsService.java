package com.badereddine.demo.service;

import com.badereddine.demo.model.ERole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserStatisticsService {

    private final UserService userService;

    public UserStatisticsService(UserService userService) {
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public UserStatistics getUserStatistics() {
        return new UserStatistics(
                userService.count(),
                userService.countByRole(ERole.ROLE_ADMIN),
                userService.countByRole(ERole.ROLE_USER),
                userService.countNewUsersToday()
        );
    }

    public record UserStatistics(
            long totalUsers,
            long totalAdmins,
            long totalRegularUsers,
            long newUsersToday
    ) {
    }
}
