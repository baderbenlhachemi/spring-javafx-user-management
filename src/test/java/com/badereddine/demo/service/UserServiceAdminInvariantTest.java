package com.badereddine.demo.service;

import com.badereddine.demo.exception.LastActiveAdminException;
import com.badereddine.demo.model.ERole;
import com.badereddine.demo.model.Role;
import com.badereddine.demo.model.User;
import com.badereddine.demo.repository.RoleRepository;
import com.badereddine.demo.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(UserServiceAdminInvariantTest.ContainerConfiguration.class)
class UserServiceAdminInvariantTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class ContainerConfiguration {
        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgreSQLContainer() {
            return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("team_access_hub_admin_invariant_test")
                    .withUsername("test_user")
                    .withPassword("test_password");
        }
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
        adminRole = roleRepository.save(new Role(ERole.ROLE_ADMIN));
        userRole = roleRepository.save(new Role(ERole.ROLE_USER));
    }

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    void rejectsDeletingTheLastActiveAdmin() {
        User admin = saveUser("admin-delete", adminRole, true);

        assertThatThrownBy(() -> userService.deleteUser(admin.getId()))
                .isInstanceOf(LastActiveAdminException.class)
                .hasMessage(LastActiveAdminException.MESSAGE);

        assertThat(userRepository.findById(admin.getId())).isPresent();
    }

    @Test
    void rejectsDisablingTheLastActiveAdmin() {
        User admin = saveUser("admin-disable", adminRole, true);

        assertThatThrownBy(() -> userService.setEnabled(admin.getId(), false))
                .isInstanceOf(LastActiveAdminException.class)
                .hasMessage(LastActiveAdminException.MESSAGE);

        assertThat(userRepository.findById(admin.getId())).get().extracting(User::isEnabled).isEqualTo(true);
    }

    @Test
    void rejectsDemotingTheLastActiveAdmin() {
        User admin = saveUser("admin-demote", adminRole, true);

        assertThatThrownBy(() -> userService.changeRole(admin.getId(), ERole.ROLE_USER))
                .isInstanceOf(LastActiveAdminException.class)
                .hasMessage(LastActiveAdminException.MESSAGE);

        assertThat(userRepository.countByRoleName(ERole.ROLE_ADMIN)).isEqualTo(1);
        assertThat(userRepository.countByRoleName(ERole.ROLE_USER)).isZero();
    }

    @ParameterizedTest
    @EnumSource(AdminRemoval.class)
    void allowsEquivalentActionWhenAnotherActiveAdminExists(AdminRemoval removal) {
        User target = saveUser("admin-target-" + removal.name().toLowerCase(), adminRole, true);
        saveUser("admin-backup-" + removal.name().toLowerCase(), adminRole, true);

        removal.apply(userService, target.getId());

        assertThat(activeAdminCount()).isEqualTo(1);
    }

    @Test
    void concurrentDisablesCannotRemoveBothActiveAdmins() throws Exception {
        User first = saveUser("admin-concurrent-one", adminRole, true);
        User second = saveUser("admin-concurrent-two", adminRole, true);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Callable<Boolean> disableFirst = disableWhenReleased(first.getId(), ready, start);
            Callable<Boolean> disableSecond = disableWhenReleased(second.getId(), ready, start);
            Future<Boolean> firstResult = executor.submit(disableFirst);
            Future<Boolean> secondResult = executor.submit(disableSecond);

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Boolean> outcomes = List.of(
                    firstResult.get(10, TimeUnit.SECONDS),
                    secondResult.get(10, TimeUnit.SECONDS));
            assertThat(outcomes).containsExactlyInAnyOrder(true, false);
            assertThat(activeAdminCount()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<Boolean> disableWhenReleased(Long id, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent test did not start in time");
            }
            try {
                userService.setEnabled(id, false);
                return true;
            } catch (LastActiveAdminException exception) {
                return false;
            }
        };
    }

    private long activeAdminCount() {
        return userRepository.countActiveByRoleName(ERole.ROLE_ADMIN);
    }

    private User saveUser(String username, Role role, boolean enabled) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.test");
        user.setPassword("encoded-password");
        user.setFirstName("Test");
        user.setLastName("Administrator");
        user.setBirthDate(new Date(0L));
        user.setCity("Test City");
        user.setCountry("Test Country");
        user.setAvatar("https://example.test/avatar.png");
        user.setCompany("Test Company");
        user.setJobPosition("Administrator");
        user.setMobile("+212 000000000");
        user.setRole(role);
        user.setEnabled(enabled);
        return userRepository.save(user);
    }

    private enum AdminRemoval {
        DELETE {
            @Override
            void apply(UserService service, Long id) {
                service.deleteUser(id);
            }
        },
        DISABLE {
            @Override
            void apply(UserService service, Long id) {
                service.setEnabled(id, false);
            }
        },
        DEMOTE {
            @Override
            void apply(UserService service, Long id) {
                service.changeRole(id, ERole.ROLE_USER);
            }
        };

        abstract void apply(UserService service, Long id);
    }
}
