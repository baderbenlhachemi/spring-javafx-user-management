package com.badereddine.demo.payload.response;

import java.util.Date;

public record UserResponse(
        Long id,
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
        RoleResponse role,
        boolean enabled,
        Date createdAt,
        Date lastLogin
) {
}
