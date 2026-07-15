package com.badereddine.demo.controller;

import com.badereddine.demo.service.UserStatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserStatsControllerTest {

    @Mock
    private UserStatisticsService userStatisticsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserController controller = UserControllerTestFactory.builder()
                .userStatisticsService(userStatisticsService)
                .build();
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void delegatesAndPreservesStatisticsResponseFields() throws Exception {
        when(userStatisticsService.getUserStatistics())
                .thenReturn(new UserStatisticsService.UserStatistics(21L, 3L, 18L, 4L));

        mockMvc.perform(get("/api/stats/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(21))
                .andExpect(jsonPath("$.totalAdmins").value(3))
                .andExpect(jsonPath("$.totalRegularUsers").value(18))
                .andExpect(jsonPath("$.newUsersToday").value(4));

        verify(userStatisticsService).getUserStatistics();
    }
}
