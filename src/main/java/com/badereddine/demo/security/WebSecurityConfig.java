package com.badereddine.demo.security;

import com.badereddine.demo.security.jwt.AuthEntryPointJwt;
import com.badereddine.demo.security.jwt.AuthTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Autowired
    private SecurityPolicyProperties securityPolicyProperties;

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

    // Custom filter to handle JWT authentication
    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(PUBLIC_AUTH_ENDPOINTS).permitAll();

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
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        // Return the http object
        return http.build();
    }

}
