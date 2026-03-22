package com.badereddine.client.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Data class representing the JWT authentication response from the server
 */
public class AuthResponse {
    // Backend returns "accessToken" not "token"
    @SerializedName("accessToken")
    private String token;

    // Backend returns "tokenType" not "type"
    @SerializedName("tokenType")
    private String type = "Bearer";

    private Long id;
    private String username;
    private String email;
    private List<String> roles;

    public AuthResponse() {
    }

    public AuthResponse(String token, String type, Long id, String username, String email, List<String> roles) {
        this.token = token;
        this.type = type != null ? type : "Bearer";
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type != null ? type : "Bearer";
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public boolean isAdmin() {
        return roles != null && roles.contains("ROLE_ADMIN");
    }

    public String getAuthorizationHeader() {
        return type + " " + token;
    }
}
