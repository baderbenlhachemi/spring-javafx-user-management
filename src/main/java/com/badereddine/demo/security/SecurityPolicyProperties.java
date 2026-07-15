package com.badereddine.demo.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.security")
public record SecurityPolicyProperties(
        boolean registrationEnabled,
        boolean swaggerEnabled
) {
}
