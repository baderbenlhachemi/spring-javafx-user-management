package com.badereddine.demo.payload.response;

import java.util.List;

public record UserListResponse(
        List<UserResponse> users,
        int currentPage,
        long totalItems,
        int totalPages,
        int size
) {
}
