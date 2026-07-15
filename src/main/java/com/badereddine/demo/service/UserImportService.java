package com.badereddine.demo.service;

import com.badereddine.demo.exception.UserImportException;
import com.badereddine.demo.model.ERole;
import com.badereddine.demo.model.Role;
import com.badereddine.demo.model.User;
import com.badereddine.demo.payload.request.UserImportRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UserImportService {
    public static final long MAX_FILE_SIZE_BYTES = 1_048_576L;
    public static final int MIN_RECORD_COUNT = 1;
    public static final int MAX_RECORD_COUNT = 1_000;

    public static final String FILE_REQUIRED_MESSAGE = "Import file is required";
    public static final String FILE_EMPTY_MESSAGE = "Import file must not be empty";
    public static final String FILE_TOO_LARGE_MESSAGE = "Import file must not exceed 1 MiB";
    public static final String FILE_READ_MESSAGE = "Import file could not be read";
    public static final String MALFORMED_JSON_MESSAGE = "Import file must contain a valid JSON array";
    public static final String UNSUPPORTED_FIELDS_MESSAGE = "Import file contains unsupported fields";
    public static final String RECORD_COUNT_MESSAGE = "Import file must contain between 1 and 1000 records";
    public static final String INVALID_FIELDS_MESSAGE = "Record %d has invalid or missing fields";
    public static final String INVALID_ROLE_MESSAGE = "Record %d has an unsupported role";

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserService userService;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectReader importReader;

    public UserImportService(
            UserService userService,
            RoleService roleService,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper
    ) {
        this.userService = userService;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.importReader = objectMapper.readerFor(UserImportRequest[].class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * Imports a JSON array containing 1 to 1,000 records from a file no larger
     * than 1 MiB. Invalid input is rejected before any user is persisted.
     */
    @Transactional
    public UserImportResult importUsers(MultipartFile file) {
        byte[] contents = readBoundedContents(file);
        UserImportRequest[] requests = parseRequests(contents);
        validateRequests(requests);

        Role adminRole = resolveRole(ERole.ROLE_ADMIN);
        Role userRole = resolveRole(ERole.ROLE_USER);
        Set<String> importedUsernames = new HashSet<>();
        Set<String> importedEmails = new HashSet<>();
        int successfulImports = 0;
        int failedImports = 0;

        for (UserImportRequest request : requests) {
            boolean newUsername = importedUsernames.add(request.username());
            boolean newEmail = importedEmails.add(request.email());
            boolean duplicateInFile = !newUsername || !newEmail;
            boolean duplicateInDatabase = !duplicateInFile
                    && (Boolean.TRUE.equals(userService.existsByUsername(request.username()))
                    || Boolean.TRUE.equals(userService.existsByEmail(request.email())));

            if (duplicateInFile || duplicateInDatabase) {
                failedImports++;
                continue;
            }

            ERole roleName = ERole.valueOf(request.role().name());
            User user = toEntity(request, roleName == ERole.ROLE_ADMIN ? adminRole : userRole);
            userService.save(user);
            successfulImports++;
        }

        return new UserImportResult(requests.length, successfulImports, failedImports);
    }

    private byte[] readBoundedContents(MultipartFile file) {
        if (file == null) {
            throw new UserImportException(FILE_REQUIRED_MESSAGE);
        }
        if (file.isEmpty()) {
            throw new UserImportException(FILE_EMPTY_MESSAGE);
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new UserImportException(FILE_TOO_LARGE_MESSAGE);
        }

        try (InputStream input = file.getInputStream()) {
            byte[] contents = input.readNBytes((int) MAX_FILE_SIZE_BYTES + 1);
            if (contents.length == 0) {
                throw new UserImportException(FILE_EMPTY_MESSAGE);
            }
            if (contents.length > MAX_FILE_SIZE_BYTES) {
                throw new UserImportException(FILE_TOO_LARGE_MESSAGE);
            }
            return contents;
        } catch (IOException exception) {
            throw new UserImportException(FILE_READ_MESSAGE);
        }
    }

    private UserImportRequest[] parseRequests(byte[] contents) {
        try {
            UserImportRequest[] requests = importReader.readValue(contents);
            if (requests == null) {
                throw new UserImportException(MALFORMED_JSON_MESSAGE);
            }
            return requests;
        } catch (UnrecognizedPropertyException exception) {
            throw new UserImportException(UNSUPPORTED_FIELDS_MESSAGE);
        } catch (JsonProcessingException exception) {
            throw new UserImportException(MALFORMED_JSON_MESSAGE);
        } catch (IOException exception) {
            throw new UserImportException(FILE_READ_MESSAGE);
        }
    }

    private void validateRequests(UserImportRequest[] requests) {
        if (requests.length < MIN_RECORD_COUNT || requests.length > MAX_RECORD_COUNT) {
            throw new UserImportException(RECORD_COUNT_MESSAGE);
        }

        for (int index = 0; index < requests.length; index++) {
            UserImportRequest request = requests[index];
            int recordNumber = index + 1;
            if (!hasValidRequiredFields(request)) {
                throw new UserImportException(INVALID_FIELDS_MESSAGE.formatted(recordNumber));
            }
            if (!isAllowedRole(request.role().name())) {
                throw new UserImportException(INVALID_ROLE_MESSAGE.formatted(recordNumber));
            }
        }
    }

    private boolean hasValidRequiredFields(UserImportRequest request) {
        return request != null
                && hasText(request.firstName())
                && request.firstName().length() <= 20
                && hasText(request.lastName())
                && request.lastName().length() <= 20
                && request.birthDate() != null
                && hasText(request.city())
                && hasText(request.country())
                && hasText(request.avatar())
                && hasText(request.company())
                && hasText(request.jobPosition())
                && hasText(request.mobile())
                && hasText(request.username())
                && hasText(request.email())
                && request.email().length() <= 50
                && EMAIL_PATTERN.matcher(request.email()).matches()
                && request.role() != null
                && hasText(request.role().name());
    }

    private boolean isAllowedRole(String roleName) {
        return ERole.ROLE_USER.name().equals(roleName) || ERole.ROLE_ADMIN.name().equals(roleName);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Role resolveRole(ERole roleName) {
        return roleService.findByName(roleName)
                .orElseGet(() -> roleService.save(new Role(roleName)));
    }

    private User toEntity(UserImportRequest request, Role role) {
        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setBirthDate(request.birthDate());
        user.setCity(request.city());
        user.setCountry(request.country());
        user.setAvatar(request.avatar());
        user.setCompany(request.company());
        user.setJobPosition(request.jobPosition());
        user.setMobile(request.mobile());
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRole(role);
        user.setEnabled(false);
        return user;
    }

    public record UserImportResult(int totalRecords, int successfulImports, int failedImports) {
    }
}
