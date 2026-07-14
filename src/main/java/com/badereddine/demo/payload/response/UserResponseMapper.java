package com.badereddine.demo.payload.response;

import com.badereddine.demo.model.Role;
import com.badereddine.demo.model.User;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class UserResponseMapper {

    public UserResponse toResponse(User user) {
        Role role = user.getRole();
        RoleResponse roleResponse = role == null ? null : new RoleResponse(role.getName());

        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                copy(user.getBirthDate()),
                user.getCity(),
                user.getCountry(),
                user.getAvatar(),
                user.getCompany(),
                user.getJobPosition(),
                user.getMobile(),
                user.getUsername(),
                user.getEmail(),
                roleResponse,
                user.isEnabled(),
                copy(user.getCreatedAt()),
                copy(user.getLastLogin())
        );
    }

    private Date copy(Date value) {
        return value == null ? null : new Date(value.getTime());
    }
}
