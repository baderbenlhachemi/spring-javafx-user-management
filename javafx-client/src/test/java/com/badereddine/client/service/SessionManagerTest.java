package com.badereddine.client.service;

import com.badereddine.client.model.AuthResponse;
import com.badereddine.client.model.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionManagerTest {

    @Test
    void acceptsIndependentInMemoryAuthenticationState() {
        AuthResponse authResponse = new AuthResponse(
                "in-memory-token",
                "Bearer",
                1L,
                "alice",
                "alice@example.com",
                List.of("ROLE_ADMIN"));

        SessionManager sessionManager = new SessionManager(authResponse);

        assertSame(authResponse, sessionManager.getAuthResponse());
        assertEquals("Bearer in-memory-token", sessionManager.getAuthorizationHeader());
        assertEquals("alice", sessionManager.getUsername());
        assertTrue(sessionManager.isAuthenticated());
        assertTrue(sessionManager.isAdmin());
    }

    @Test
    void logoutClearsAllInMemorySessionState() {
        SessionManager sessionManager = new SessionManager(new AuthResponse(
                "in-memory-token",
                "Bearer",
                1L,
                "alice",
                "alice@example.com",
                List.of("ROLE_USER")));
        sessionManager.setCurrentUser(new User());

        sessionManager.logout();

        assertNull(sessionManager.getAuthResponse());
        assertNull(sessionManager.getCurrentUser());
        assertNull(sessionManager.getAuthorizationHeader());
        assertFalse(sessionManager.isAuthenticated());
    }
}
