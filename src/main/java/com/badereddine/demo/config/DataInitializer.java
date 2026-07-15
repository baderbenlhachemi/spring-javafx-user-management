package com.badereddine.demo.config;

import com.badereddine.demo.model.ERole;
import com.badereddine.demo.model.Role;
import com.badereddine.demo.model.User;
import com.badereddine.demo.repository.RoleRepository;
import com.badereddine.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Initializes required roles and optionally creates a development administrator.
 */
@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;
    private final String adminUsername;
    private final String adminPassword;
    private final String adminEmail;

    public DataInitializer(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            Environment environment,
            @Value("${demo.initializer.admin.username:}") String adminUsername,
            @Value("${demo.initializer.admin.password:}") String adminPassword,
            @Value("${demo.initializer.admin.email:}") String adminEmail
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.adminEmail = adminEmail;
    }

    @Override
    public void run(String... args) {
        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(ERole.ROLE_ADMIN)));

        if (roleRepository.findByName(ERole.ROLE_USER).isEmpty()) {
            roleRepository.save(new Role(ERole.ROLE_USER));
        }

        if (!environment.acceptsProfiles(Profiles.of("dev"))) {
            return;
        }

        if (!userRepository.existsByUsername(adminUsername)) {
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setBirthDate(new Date());
            admin.setCity("System");
            admin.setCountry("System");
            admin.setAvatar("https://ui-avatars.com/api/?name=Admin&background=6366F1&color=fff");
            admin.setCompany("System");
            admin.setJobPosition("Administrator");
            admin.setMobile("+212 000000000");
            admin.setRole(adminRole);

            userRepository.save(admin);
            logger.info("Development administrator initialized");
        }
    }
}
