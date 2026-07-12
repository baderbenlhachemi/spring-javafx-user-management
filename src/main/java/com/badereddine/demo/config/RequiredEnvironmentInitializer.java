package com.badereddine.demo.config;

import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;

import java.util.List;

public class RequiredEnvironmentInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final List<String> REQUIRED_VARIABLES = List.of(
            "DB_URL",
            "DB_USERNAME",
            "DB_PASSWORD",
            "JWT_SECRET"
    );

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Environment environment = applicationContext.getEnvironment();
        if (environment.acceptsProfiles(Profiles.of("test"))) {
            return;
        }

        List<String> missingVariables = REQUIRED_VARIABLES.stream()
                .filter(variable -> !StringUtils.hasText(environment.getProperty(variable)))
                .toList();

        if (!missingVariables.isEmpty()) {
            throw new ApplicationContextException(
                    "Missing required environment variables: " + String.join(", ", missingVariables));
        }
    }
}
