package com.badereddine.demo.payload.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Date;

@JsonIgnoreProperties(value = {
        "id", "password", "passwordHash", "password_hash", "enabled", "createdAt", "lastLogin"
})
public record UserImportRequest(
        String firstName,
        String lastName,
        Date birthDate,
        String city,
        String country,
        String avatar,
        String company,
        String jobPosition,
        String mobile,
        String username,
        String email,
        RoleImportRequest role
) {
    @JsonIgnoreProperties(value = {"id"})
    public record RoleImportRequest(String name) {
    }
}
