package com.badereddine.demo.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityPolicyPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void defaultsBothPoliciesToDisabled() {
        contextRunner.run(context -> {
            SecurityPolicyProperties properties = context.getBean(SecurityPolicyProperties.class);

            assertThat(properties.registrationEnabled()).isFalse();
            assertThat(properties.swaggerEnabled()).isFalse();
        });
    }

    @Test
    void bindsEnabledPoliciesFromConfiguration() {
        contextRunner
                .withPropertyValues(
                        "demo.security.registration-enabled=true",
                        "demo.security.swagger-enabled=true")
                .run(context -> {
                    SecurityPolicyProperties properties = context.getBean(SecurityPolicyProperties.class);

                    assertThat(properties.registrationEnabled()).isTrue();
                    assertThat(properties.swaggerEnabled()).isTrue();
                });
    }

    @Test
    void committedProfilesUseSafeDefaultsAndExplicitDevelopmentDefaults() throws IOException {
        Properties defaults = PropertiesLoaderUtils.loadProperties(
                new ClassPathResource("application.properties"));
        Properties development = PropertiesLoaderUtils.loadProperties(
                new ClassPathResource("application-dev.properties"));

        assertThat(defaults.getProperty("demo.security.registration-enabled"))
                .isEqualTo("${DEMO_REGISTRATION_ENABLED:false}");
        assertThat(defaults.getProperty("demo.security.swagger-enabled"))
                .isEqualTo("${DEMO_SWAGGER_ENABLED:false}");
        assertThat(development.getProperty("demo.security.registration-enabled"))
                .isEqualTo("${DEMO_REGISTRATION_ENABLED:true}");
        assertThat(development.getProperty("demo.security.swagger-enabled"))
                .isEqualTo("${DEMO_SWAGGER_ENABLED:true}");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SecurityPolicyProperties.class)
    static class PropertiesConfiguration {
    }
}
