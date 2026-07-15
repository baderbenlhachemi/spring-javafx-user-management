package com.badereddine.demo.service;

import com.badereddine.demo.model.ERole;
import com.badereddine.demo.model.Role;
import com.badereddine.demo.model.User;
import com.badereddine.demo.repository.RoleRepository;
import com.badereddine.demo.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(UserServiceTest.TestDependencies.class)
class UserServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-15T12:00:00Z"),
            ZoneOffset.UTC
    );

    @TestConfiguration(proxyBeanMethods = false)
    static class TestDependencies {

        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgreSQLContainer() {
            return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("team_access_hub_user_service_test")
                    .withUsername("test_user")
                    .withPassword("test_password");
        }

        @Bean
        @Primary
        Clock fixedClock() {
            return FIXED_CLOCK;
        }
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void reportsExistingUsernameAndRejectsDuplicateUsername() {
        saveUser("unique-username", "first@example.test", userRole, true);

        assertThat(userService.existsByUsername("unique-username")).isTrue();
        assertThat(userService.existsByUsername("missing-username")).isFalse();
        assertThatThrownBy(() -> saveUser(
                "unique-username",
                "second@example.test",
                userRole,
                true
        )).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void reportsExistingEmailAndRejectsDuplicateEmail() {
        saveUser("first-email-user", "unique-email@example.test", userRole, true);

        assertThat(userService.existsByEmail("unique-email@example.test")).isTrue();
        assertThat(userService.existsByEmail("missing@example.test")).isFalse();
        assertThatThrownBy(() -> saveUser(
                "second-email-user",
                "unique-email@example.test",
                userRole,
                true
        )).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void changesRoleAndPersistsTheNewAssignment() {
        User member = saveUser("role-change", "role-change@example.test", userRole, true);

        User changed = userService.changeRole(member.getId(), ERole.ROLE_ADMIN);

        assertThat(changed.getRole().getName()).isEqualTo(ERole.ROLE_ADMIN);
        assertThat(userRepository.countByRoleName(ERole.ROLE_ADMIN)).isEqualTo(1);
        assertThat(userRepository.countByRoleName(ERole.ROLE_USER)).isZero();
    }

    @Test
    void disablesAndReenablesRegularUser() {
        User member = saveUser("status-change", "status-change@example.test", userRole, true);

        User disabled = userService.setEnabled(member.getId(), false);
        assertThat(disabled.isEnabled()).isFalse();
        assertThat(userRepository.findById(member.getId()))
                .get()
                .extracting(User::isEnabled)
                .isEqualTo(false);

        User reenabled = userService.setEnabled(member.getId(), true);
        assertThat(reenabled.isEnabled()).isTrue();
        assertThat(userRepository.findById(member.getId()))
                .get()
                .extracting(User::isEnabled)
                .isEqualTo(true);
    }

    @Test
    void countsUsersCreatedSinceStartOfTodayFromInjectedClock() {
        Instant startOfToday = LocalDate.now(FIXED_CLOCK)
                .atStartOfDay(FIXED_CLOCK.getZone())
                .toInstant();
        User beforeToday = saveUser("before-today", "before@example.test", userRole, true);
        User atStartOfToday = saveUser("at-start", "at-start@example.test", userRole, true);
        User duringToday = saveUser("during-today", "during@example.test", adminRole, true);
        setCreatedAt(beforeToday, startOfToday.minusSeconds(1));
        setCreatedAt(atStartOfToday, startOfToday);
        setCreatedAt(duringToday, startOfToday.plusSeconds(60));

        assertThat(userService.countNewUsersToday()).isEqualTo(2);
    }

    private User saveUser(String username, String email, Role role, boolean enabled) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setBirthDate(new Date(0L));
        user.setCity("Test City");
        user.setCountry("Test Country");
        user.setAvatar("https://example.test/avatar.png");
        user.setCompany("Test Company");
        user.setJobPosition("Test Position");
        user.setMobile("+212 000000000");
        user.setRole(role);
        user.setEnabled(enabled);
        return userService.save(user);
    }

    private void setCreatedAt(User user, Instant createdAt) {
        jdbcTemplate.update(
                "UPDATE users SET created_at = ? WHERE id = ?",
                Timestamp.from(createdAt),
                user.getId()
        );
    }
}
