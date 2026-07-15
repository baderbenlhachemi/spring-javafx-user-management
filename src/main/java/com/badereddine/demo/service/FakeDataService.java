package com.badereddine.demo.service;

import com.badereddine.demo.model.ERole;
import com.badereddine.demo.payload.response.GeneratedUserResponse;
import com.badereddine.demo.payload.response.RoleResponse;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FakeDataService {
    public static final int MIN_USER_COUNT = 1;
    public static final int MAX_USER_COUNT = 1_000;
    public static final int MIN_ADMIN_COUNT = 0;
    public static final int MAX_ADMIN_COUNT = MAX_USER_COUNT;
    public static final String INVALID_USER_COUNT_MESSAGE = "count must be between 1 and 1000";
    public static final String INVALID_ADMIN_COUNT_MESSAGE = "adminCount must be between 0 and count";

    /**
     * Generates 1 to 1,000 disabled users, with adminCount constrained to the
     * inclusive range from 0 to count.
     */
    public List<GeneratedUserResponse> generateFakeUsers(int count, int adminCount) {
        validateCounts(count, adminCount);

        Faker faker = new Faker();
        List<GeneratedUserResponse> users = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {
            ERole role = index < adminCount ? ERole.ROLE_ADMIN : ERole.ROLE_USER;
            users.add(generateFakeUser(faker, role));
        }

        return users;
    }

    private void validateCounts(int count, int adminCount) {
        if (count < MIN_USER_COUNT || count > MAX_USER_COUNT) {
            throw new IllegalArgumentException(INVALID_USER_COUNT_MESSAGE);
        }
        if (adminCount < MIN_ADMIN_COUNT || adminCount > MAX_ADMIN_COUNT || adminCount > count) {
            throw new IllegalArgumentException(INVALID_ADMIN_COUNT_MESSAGE);
        }
    }

    private GeneratedUserResponse generateFakeUser(Faker faker, ERole role) {
        return new GeneratedUserResponse(
                null,
                faker.name().firstName(),
                faker.name().lastName(),
                faker.date().birthday(),
                faker.address().city(),
                faker.address().country(),
                faker.internet().avatar(),
                faker.company().name(),
                faker.company().profession(),
                faker.phoneNumber().cellPhone(),
                faker.name().username(),
                faker.internet().emailAddress(),
                new RoleResponse(role),
                false,
                null,
                null
        );
    }
}
