package com.badereddine.demo.config;

import com.badereddine.demo.model.Role;
import com.badereddine.demo.model.User;
import com.badereddine.demo.repository.RoleRepository;
import com.badereddine.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class DataInitializerTest {

    @Test
    void defaultProfileInitializesRolesWithoutCreatingAdministrator() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        configureRoleRepository(roleRepository);

        contextRunner(userRepository, roleRepository, passwordEncoder)
                .run(context -> {
                    context.getBean(DataInitializer.class).run();

                    verify(userRepository, never()).existsByUsername(any());
                    verify(userRepository, never()).save(any(User.class));
                    verify(passwordEncoder, never()).encode(any());
                    verify(roleRepository, times(2)).save(any(Role.class));
                });
    }

    @Test
    void devProfileCreatesConfiguredAdministratorWithoutPrintingCredentials(CapturedOutput output) {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        configureRoleRepository(roleRepository);

        String username = UUID.randomUUID().toString();
        String password = UUID.randomUUID().toString();
        String email = UUID.randomUUID() + "@example.invalid";
        String encodedPassword = UUID.randomUUID().toString();
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);

        contextRunner(userRepository, roleRepository, passwordEncoder)
                .withPropertyValues(
                        "spring.profiles.active=dev",
                        "demo.initializer.admin.username=" + username,
                        "demo.initializer.admin.password=" + password,
                        "demo.initializer.admin.email=" + email)
                .run(context -> {
                    context.getBean(DataInitializer.class).run();

                    org.mockito.ArgumentCaptor<User> userCaptor =
                            org.mockito.ArgumentCaptor.forClass(User.class);
                    verify(userRepository).save(userCaptor.capture());
                    User savedUser = userCaptor.getValue();

                    assertThat(savedUser.getUsername()).isEqualTo(username);
                    assertThat(savedUser.getEmail()).isEqualTo(email);
                    assertThat(savedUser.getPassword()).isEqualTo(encodedPassword);
                    verify(passwordEncoder).encode(password);
                });

        assertThat(output.getAll())
                .doesNotContain(username)
                .doesNotContain(password)
                .doesNotContain(encodedPassword)
                .doesNotContain(email);
    }

    @Test
    void devProfileMapsAdministratorSettingsFromEnvironment() throws IOException {
        Properties properties = PropertiesLoaderUtils.loadProperties(
                new ClassPathResource("application-dev.properties"));

        assertThat(properties)
                .containsEntry("demo.initializer.admin.username", "${DEMO_ADMIN_USERNAME}")
                .containsEntry("demo.initializer.admin.password", "${DEMO_ADMIN_PASSWORD}")
                .containsEntry("demo.initializer.admin.email", "${DEMO_ADMIN_EMAIL}");
    }

    private ApplicationContextRunner contextRunner(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        return new ApplicationContextRunner()
                .withUserConfiguration(TestConfiguration.class)
                .withBean(UserRepository.class, () -> userRepository)
                .withBean(RoleRepository.class, () -> roleRepository)
                .withBean(PasswordEncoder.class, () -> passwordEncoder);
    }

    private void configureRoleRepository(RoleRepository roleRepository) {
        when(roleRepository.findByName(any())).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Configuration(proxyBeanMethods = false)
    @Import(DataInitializer.class)
    static class TestConfiguration {
    }
}
