package com.badereddine.demo.service;

import com.badereddine.demo.model.ERole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserStatisticsServiceTest {

    @Mock
    private UserService userService;

    @Test
    void returnsCompatibleUserStatisticsFromUserService() {
        when(userService.count()).thenReturn(21L);
        when(userService.countByRole(ERole.ROLE_ADMIN)).thenReturn(3L);
        when(userService.countByRole(ERole.ROLE_USER)).thenReturn(18L);
        when(userService.countNewUsersToday()).thenReturn(4L);
        UserStatisticsService service = new UserStatisticsService(userService);

        UserStatisticsService.UserStatistics statistics = service.getUserStatistics();

        assertThat(statistics.totalUsers()).isEqualTo(21L);
        assertThat(statistics.totalAdmins()).isEqualTo(3L);
        assertThat(statistics.totalRegularUsers()).isEqualTo(18L);
        assertThat(statistics.newUsersToday()).isEqualTo(4L);
        InOrder inOrder = inOrder(userService);
        inOrder.verify(userService).count();
        inOrder.verify(userService).countByRole(ERole.ROLE_ADMIN);
        inOrder.verify(userService).countByRole(ERole.ROLE_USER);
        inOrder.verify(userService).countNewUsersToday();
    }

    @Test
    void declaresReadOnlyTransactionBoundary() throws Exception {
        Method method = UserStatisticsService.class.getMethod("getUserStatistics");
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();
    }
}
