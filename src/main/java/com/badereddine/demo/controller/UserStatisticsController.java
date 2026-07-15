package com.badereddine.demo.controller;

import com.badereddine.demo.service.UserStatisticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "User Statistics")
public class UserStatisticsController {

    private final UserStatisticsService userStatisticsService;

    public UserStatisticsController(UserStatisticsService userStatisticsService) {
        this.userStatisticsService = userStatisticsService;
    }

    @GetMapping("/stats/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserStats() {
        UserStatisticsService.UserStatistics userStatistics = userStatisticsService.getUserStatistics();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userStatistics.totalUsers());
        stats.put("totalAdmins", userStatistics.totalAdmins());
        stats.put("totalRegularUsers", userStatistics.totalRegularUsers());
        stats.put("newUsersToday", userStatistics.newUsersToday());
        return ResponseEntity.ok(stats);
    }
}
