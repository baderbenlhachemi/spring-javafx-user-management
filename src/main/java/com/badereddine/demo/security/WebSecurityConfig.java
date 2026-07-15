package com.badereddine.demo.security;

import com.badereddine.demo.security.jwt.AuthEntryPointJwt;
import com.badereddine.demo.security.jwt.AuthTokenFilter;
import com.badereddine.demo.security.jwt.JwtUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.badereddine.demo.security.services.UserDetailsServiceImpl;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityPolicyProperties.class)
public class WebSecurityConfig {
    private final UserDetailsServiceImpl userDetailsService;
    private final AuthEntryPointJwt unauthorizedHandler;
    private final SecurityPolicyProperties securityPolicyProperties;

    public WebSecurityConfig(
            UserDetailsServiceImpl userDetailsService,
            AuthEntryPointJwt unauthorizedHandler,
            SecurityPolicyProperties securityPolicyProperties
    ) {
        this.userDetailsService = userDetailsService;
        this.unauthorizedHandler = unauthorizedHandler;
        this.securityPolicyProperties = securityPolicyProperties;
    }

    private static final String[] PUBLIC_AUTH_ENDPOINTS = {
            "/api/auth",
            "/api/v1/auth/**",
    };

    private static final String[] API_DOCUMENTATION_ENDPOINTS = {
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/swagger-ui/**",
            "/swagger-ui.html",
    };

    private static final String[] PUBLIC_HEALTH_ENDPOINTS = {
            "/actuator/health/liveness",
            "/actuator/health/readiness",
    };

    // Custom filter to handle JWT authentication
    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter(JwtUtils jwtUtils) {
        return new AuthTokenFilter(jwtUtils, userDetailsService);
    }

    // Provider to authenticate users
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    // Responsible for authenticating users
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthTokenFilter authTokenFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(PUBLIC_AUTH_ENDPOINTS).permitAll();
                    auth.requestMatchers(PUBLIC_HEALTH_ENDPOINTS).permitAll();

                    if (securityPolicyProperties.registrationEnabled()) {
                        auth.requestMatchers("/api/auth/register").permitAll();
                    } else {
                        auth.requestMatchers("/api/auth/register").denyAll();
                    }

                    if (securityPolicyProperties.swaggerEnabled()) {
                        auth.requestMatchers(API_DOCUMENTATION_ENDPOINTS).permitAll();
                    } else {
                        auth.requestMatchers(API_DOCUMENTATION_ENDPOINTS).denyAll();
                    }

                    auth.anyRequest().authenticated();
                });

        // Add the authentication provider to the http object
        http.authenticationProvider(authenticationProvider());

        // Add the custom filter to the http object
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        // Return the http object
        return http.build();
    }

}
