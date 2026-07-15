package com.badereddine.demo.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserPaginationPolicy {

    public static final int MAX_PAGE_SIZE = 100;
    public static final String INVALID_PAGE_MESSAGE = "page must be a non-negative integer";
    public static final String INVALID_SIZE_MESSAGE = "size must be an integer between 1 and 100";
    public static final String INVALID_SORT_FIELD_MESSAGE =
            "sortBy must be one of: username, email, firstName, company, enabled, lastLogin";
    public static final String INVALID_SORT_DIRECTION_MESSAGE = "sortDir must be either asc or desc";

    private static final List<String> ALLOWED_SORT_FIELDS = List.of(
            "username",
            "email",
            "firstName",
            "company",
            "enabled",
            "lastLogin"
    );

    public Pageable create(int page, int size, String sortBy, String sortDir) {
        if (page < 0) {
            throw new IllegalArgumentException(INVALID_PAGE_MESSAGE);
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(INVALID_SIZE_MESSAGE);
        }
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException(INVALID_SORT_FIELD_MESSAGE);
        }

        Sort.Direction direction;
        if ("asc".equalsIgnoreCase(sortDir)) {
            direction = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(sortDir)) {
            direction = Sort.Direction.DESC;
        } else {
            throw new IllegalArgumentException(INVALID_SORT_DIRECTION_MESSAGE);
        }

        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
}
