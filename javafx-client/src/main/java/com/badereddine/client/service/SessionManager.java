package com.badereddine.client.service;

import com.badereddine.client.model.AuthResponse;
import com.badereddine.client.model.User;

/**
 * Service to manage the current user session
 */
public class SessionManager {

    private static SessionManager instance;

    private AuthResponse authResponse;
    private User currentUser;

    public SessionManager() {
    }

    public SessionManager(AuthResponse authResponse) {
        this.authResponse = authResponse;
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setAuthResponse(AuthResponse authResponse) {
        this.authResponse = authResponse;
    }

    public AuthResponse getAuthResponse() {
        return authResponse;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public String getAuthorizationHeader() {
        if (authResponse != null) {
            return authResponse.getAuthorizationHeader();
        }
        return null;
    }

    public boolean isAuthenticated() {
        return authResponse != null && authResponse.getToken() != null;
    }

    public boolean isAdmin() {
        return authResponse != null && authResponse.isAdmin();
    }

    public String getUsername() {
        return authResponse != null ? authResponse.getUsername() : null;
    }

    public void logout() {
        this.authResponse = null;
        this.currentUser = null;
    }
}
