package com.badereddine.demo.config;

import com.badereddine.demo.model.ERole;
import com.badereddine.demo.model.Role;
import com.badereddine.demo.model.User;
import com.badereddine.demo.repository.RoleRepository;
import com.badereddine.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Data initializer that creates default admin user and roles on application startup.
 * This ensures users can login immediately without needing to make API calls first.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        System.out.println("=== Initializing default data ===");

        // Create roles if they don't exist
        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                .orElseGet(() -> {
                    System.out.println("Creating ROLE_ADMIN...");
                    return roleRepository.save(new Role(ERole.ROLE_ADMIN));
                });

        // Ensure ROLE_USER exists as well
        if (roleRepository.findByName(ERole.ROLE_USER).isEmpty()) {
            System.out.println("Creating ROLE_USER...");
            roleRepository.save(new Role(ERole.ROLE_USER));
        }

        // Create default admin user if it doesn't exist
        if (!userRepository.existsByUsername("admin")) {
            System.out.println("Creating default admin user...");

            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@localhost.com");
            admin.setPassword(passwordEncoder.encode("admin"));
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
            System.out.println("✓ Default admin user created successfully!");
            System.out.println("  Username: admin");
            System.out.println("  Password: admin");
        } else {
            System.out.println("Default admin user already exists.");
        }

        System.out.println("=== Data initialization complete ===");
    }
}
