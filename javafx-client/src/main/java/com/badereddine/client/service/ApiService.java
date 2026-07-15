package com.badereddine.client.service;

import com.badereddine.client.api.ApiClient;
import com.badereddine.client.config.ClientConfiguration;
import com.badereddine.client.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service class for making API calls to the JWT User Management backend
 */
public class ApiService {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final ApiClient apiClient;
    private final SessionManager sessionManager;

    private static ApiService instance;

    private ApiService() {
        this(createDefaultApiClient(), SessionManager.getInstance());
    }

    public ApiService(ApiClient apiClient, SessionManager sessionManager) {
        this.apiClient = java.util.Objects.requireNonNull(apiClient, "apiClient must not be null");
        this.sessionManager = java.util.Objects.requireNonNull(sessionManager, "sessionManager must not be null");
    }

    private static ApiClient createDefaultApiClient() {
        ClientConfiguration configuration = ClientConfiguration.load();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                .create();
        return new ApiClient(client, configuration.apiBaseUrl(), gson);
    }

    public static synchronized ApiService getInstance() {
        if (instance == null) {
            instance = new ApiService();
        }
        return instance;
    }

    /**
     * Authenticate a user and get JWT token
     */
    public CompletableFuture<ApiResult<AuthResponse>> authenticate(String username, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonBody = apiClient.toJson(new LoginRequest(username, null, password));
                RequestBody body = RequestBody.create(jsonBody, JSON);

                Request request = new Request.Builder()
                        .url(apiClient.url("/auth"))
                        .post(body)
                        .build();

                try (Response response = apiClient.execute(request)) {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (response.isSuccessful()) {
                        AuthResponse authResponse = apiClient.fromJson(responseBody, AuthResponse.class);
                        return ApiResult.success(authResponse);
                    } else {
                        return ApiResult.error("Authentication failed: " + getErrorMessage(responseBody, response.code()));
                    }
                }
            } catch (IOException e) {
                return ApiResult.error("Connection error: " + e.getMessage());
            }
        });
    }

    /**
     * Generate fake users and download the JSON file (Admin only)
     */
    public CompletableFuture<ApiResult<byte[]>> generateUsers(int count, int adminCount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = apiClient.url("/users/generate/" + count + "?adminCount=" + adminCount);
                Request request = new Request.Builder()
                        .url(url)
                        .header("Authorization", authorizationHeader())
                        .get()
                        .build();

                try (Response response = apiClient.execute(request)) {
                    if (response.isSuccessful() && response.body() != null) {
                        byte[] bytes = response.body().bytes();
                        return ApiResult.success(bytes);
                    } else {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        return ApiResult.error("Failed to generate users: " + getErrorMessage(responseBody, response.code()));
                    }
                }
            } catch (IOException e) {
                return ApiResult.error("Connection error: " + e.getMessage());
            }
        });
    }

    /**
     * Export users to CSV (Admin only)
     */
    public CompletableFuture<ApiResult<byte[]>> exportUsersToCsv(String search) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                StringBuilder urlBuilder = new StringBuilder(apiClient.url("/users/export/csv"));
                if (search != null && !search.trim().isEmpty()) {
                    try {
                        urlBuilder.append("?search=").append(java.net.URLEncoder.encode(search.trim(), "UTF-8"));
                    } catch (Exception e) {
                        urlBuilder.append("?search=").append(search.trim());
                    }
                }

                Request request = new Request.Builder()
                        .url(urlBuilder.toString())
                        .header("Authorization", authorizationHeader())
                        .get()
                        .build();

                try (Response response = apiClient.execute(request)) {
                    if (response.isSuccessful() && response.body() != null) {
                        byte[] bytes = response.body().bytes();
                        return ApiResult.success(bytes);
                    } else {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        return ApiResult.error("Failed to export users: " + getErrorMessage(responseBody, response.code()));
                    }
                }
            } catch (IOException e) {
                return ApiResult.error("Connection error: " + e.getMessage());
            }
        });
    }

    /**
     * Upload a JSON file to batch import users (Admin only)
     */
    public CompletableFuture<ApiResult<BatchImportResult>> batchImportUsers(File file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                RequestBody fileBody = RequestBody.create(file, MediaType.get("application/json"));
                MultipartBody body = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", file.getName(), fileBody)
                        .build();

                Request request = new Request.Builder()
                        .url(apiClient.url("/users/batch"))
                        .header("Authorization", authorizationHeader())
                        .post(body)
                        .build();

                try (Response response = apiClient.execute(request)) {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (response.isSuccessful()) {
                        BatchImportResult result = apiClient.fromJson(responseBody, BatchImportResult.class);
                        return ApiResult.success(result);
                    } else {
                        return ApiResult.error("Failed to import users: " + getErrorMessage(responseBody, response.code()));
                    }
                }
            } catch (IOException e) {
                return ApiResult.error("Connection error: " + e.getMessage());
            }
        });
    }

    /**
     * Get current user's profile
     */
    public CompletableFuture<ApiResult<User>> getMyProfile() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url(apiClient.url("/users/me"))
                        .header("Authorization", authorizationHeader())
                        .get()
                        .build();

                try (Response response = apiClient.execute(request)) {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (response.isSuccessful()) {
                        User user = apiClient.fromJson(responseBody, User.class);
                        return ApiResult.success(user);
                    } else {
                        return ApiResult.error("Failed to get profile: " + getErrorMessage(responseBody, response.code()));
                    }
                }
            } catch (IOException e) {
                return ApiResult.error("Connection error: " + e.getMessage());
            }
        });
    }

    /**
     * Get a user's profile by username (admin only)
     */
    public CompletableFuture<ApiResult<User>> getUserProfile(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url(apiClient.url("/users/" + username))
                        .header("Authorization", authorizationHeader())
                        .get()
                        .build();

                try (Response response = apiClient.execute(request)) {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (response.isSuccessful()) {
                        User user = apiClient.fromJson(responseBody, User.class);
                        return ApiResult.success(user);
                    } else {
                        return ApiResult.error("Failed to get user profile: " + getErrorMessage(responseBody, response.code()));
                    }
                }
            } catch (IOException e) {
                return ApiResult.error("Connection error: " + e.getMessage());
            }
        });
    }

    private String getErrorMessage(String responseBody, int code) {
        try {
            ErrorResponse error = apiClient.fromJson(responseBody, ErrorResponse.class);
            if (error != null && error.getMessage() != null) {
                return error.getMessage();
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return "HTTP " + code;
    }

    // Inner classes for request/response handling
    private static class LoginRequest {
        private String username;
        private String email;
        private String password;

        public LoginRequest(String username, String email, String password) {
            this.username = username;
            this.email = email;
            this.password = password;
        }
    }

    private static class ErrorResponse {
        private String message;
        private int status;

        public String getMessage() {
            return message;
        }
    }

    // ==================== NEW API METHODS ====================

    /**
     * Register a new user
     */
    public CompletableFuture<ApiResult<String>> register(SignupRequest signupRequest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonBody = apiClient.toJson(signupRequest);
                RequestBody body = RequestBody.create(jsonBody, JSON);

                Request request = new Request.Builder()
                        .url(apiClient.url("/auth/register"))
                        .post(body)
                        .build();

                try (Response response = apiClient.execute(request)) {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (response.isSuccessful()) {
                        return ApiResult.success("Registration successful!");
                    } else {
                        return ApiResult.error("Registration failed: " + getErrorMessage(responseBody, response.code()));
                    }
                }
            } catch (IOException e) {
                return ApiResult.error("Connection error: " + e.getMessage());
            }
        });
    }

    /**
     * Change password for current user
     */
    public CompletableFuture<ApiResult<String>> changePassword(PasswordChangeRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonBody = apiClient.toJson(request);
                RequestBody body = RequestBody.create(jsonBody, JSON);

                Request httpRequest = new Request.Builder()
                        .url(apiClient.url("/users/me/password"))
                        .header("Authorization", authorizationHeader())
                        .put(body)
                        .build();

                try (Response response = apiClient.execute(httpRequest)) {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (response.isSuccessful()) {
                        return ApiResult.success("Password changed successfully!");
                    } else {
                        return ApiResult.error("Failed to change password: " + getErrorMessage(responseBody, response.code()));
                    }
                }
            } catch (IOException e) {
                return ApiResult.error("Connection error: " + e.getMessage());
            }
        });
    }

    /**
     * Update profile for current user
     */
    public CompletableFuture<ApiResult<User>> updateProfile(User updatedUser) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonBody = apiClient.toJson(updatedUser);
                RequestBody body = RequestBody.create(jsonBody, JSON);

                Request request = new Request.Builder()
                        .url(apiClient.url("/users/me"))
                        .header("Authorization", authorizationHeader())
                        .put(body)
                        .build();

                try (Response response = apiClient.execute(request)) {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (response.isSuccessful()) {
                        User user = apiClient.fromJson(responseBody, User.class);
                        return ApiResult.success(user);
                    } else {
                        return ApiResult.error("Failed to update profile: " + getErrorMessage(responseBody, response.code()));
                    }
                }
            } catch (IOException e) {
                return ApiResult.error("Connection error: " + e.getMessage());
            }
        });
    }

    /**
     * Get all users (Admin only) with pagination and search
     */
    public CompletableFuture<ApiResult<UserListResponse>> getAllUsers(int page, int size, String sortBy, String sortDir, String search) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                StringBuilder urlBuilder = new StringBuilder();
                urlBuilder.append(apiClient.url(String.format("/users?page=%d&size=%d&sortBy=%s&sortDir=%s",
                        page, size, sortBy, sortDir)));

                if (search != null && !search.trim().isEmpty()) {
                    try {
                        urlBuilder.append("&search=").append(java.net.URLEncoder.encode(search.trim(), "UTF-8"));
                    } catch (Exception e) {
                        urlBuilder.append("&search=").append(search.trim());
                    }
                }

                Request request = new Request.Builder()
                        .url(urlBuilder.toString())
                        .header("Authorization", authorizationHeader())
                        .get()
                        .build();

                try (Response response = apiClient.execute(request)) {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (response.isSuccessful()) {
                        UserListResponse userList = apiClient.fromJson(responseBody, UserListResponse.class);
                        return ApiResult.success(userList);
                    } else {
                        return ApiResult.error("Failed to get users: " + getErrorMessage(responseBody, response.code()));
                    }
                }
            } catch (IOException e) {
                return ApiResult.error("Connection error: " + e.getMessage());
            }
        });
    }

    /**
     * Get user by ID (Admin only)
     */
    public CompletableFuture<ApiResult<User>> getUserById(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url(apiClient.url("/users/id/" + userId))
                        .header("Authorization", authorizationHeader())
                        .get()
                        .build();

                try (Response response = apiClient.execute(request)) {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (response.isSuccessful()) {
                        User user = apiClient.fromJson(responseBody, User.class);
                        return ApiResult.success(user);
                    } else {
                        return ApiResult.error("Failed to get user: " + getErrorMessage(responseBody, response.code()));
                    }
                }
            } catch (IOException e) {
                return ApiResult.error("Connection error: " + e.getMessage());
            }
        });
    }

    /**
     * Delete user (Admin only)
     */
    public CompletableFuture<ApiResult<String>> deleteUser(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url(apiClient.url("/users/" + userId))
                        .header("Authorization", authorizationHeader())
                        .delete()
                        .build();

                try (Response response = apiClient.execute(request)) {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (response.isSuccessful()) {
                        return ApiResult.success("User deleted successfully");
                    } else {
                        return ApiResult.error("Failed to delete user: " + getErrorMessage(responseBody, response.code()));
                    }
                }
            } catch (IOException e) {
                return ApiResult.error("Connection error: " + e.getMessage());
            }
        });
    }

    /**
     * Update user (Admin only - can edit any user)
     */
    public CompletableFuture<ApiResult<User>> updateUserById(Long userId, User updatedUser) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonBody = apiClient.toJson(updatedUser);
                RequestBody body = RequestBody.create(jsonBody, JSON);

                Request request = new Request.Builder()
                        .url(apiClient.url("/users/" + userId))
                        .header("Authorization", authorizationHeader())
                        .put(body)
                        .build();

                try (Response response = apiClient.execute(request)) {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (response.isSuccessful()) {
                        User user = apiClient.fromJson(responseBody, User.class);
                        return ApiResult.success(user);
                    } else {
                        return ApiResult.error("Failed to update user: " + getErrorMessage(responseBody, response.code()));
                    }
                }
            } catch (IOException e) {
                return ApiResult.error("Connection error: " + e.getMessage());
            }
        });
    }

    /**
     * Change user role (Admin only)
     */
    public CompletableFuture<ApiResult<String>> changeUserRole(Long userId, String newRole) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url(apiClient.url("/users/" + userId + "/role?role=" + newRole))
                        .header("Authorization", authorizationHeader())
                        .patch(RequestBody.create("", JSON))
                        .build();

                try (Response response = apiClient.execute(request)) {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (response.isSuccessful()) {
                        return ApiResult.success("Role updated successfully");
                    } else {
                        return ApiResult.error("Failed to change role: " + getErrorMessage(responseBody, response.code()));
                    }
                }
            } catch (IOException e) {
                return ApiResult.error("Connection error: " + e.getMessage());
            }
        });
    }

    /**
     * Toggle user status (enable/disable) - Admin only
     */
    public CompletableFuture<ApiResult<String>> toggleUserStatus(Long userId, boolean enabled) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url(apiClient.url("/users/" + userId + "/status?enabled=" + enabled))
                        .header("Authorization", authorizationHeader())
                        .patch(RequestBody.create("", JSON))
                        .build();

                try (Response response = apiClient.execute(request)) {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (response.isSuccessful()) {
                        return ApiResult.success(enabled ? "User enabled" : "User disabled");
                    } else {
                        return ApiResult.error("Failed to update status: " + getErrorMessage(responseBody, response.code()));
                    }
                }
            } catch (IOException e) {
                return ApiResult.error("Connection error: " + e.getMessage());
            }
        });
    }

    /**
     * Get user stats (Admin only)
     */
    public CompletableFuture<ApiResult<UserStats>> getUserStats() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url(apiClient.url("/stats/users"))
                        .header("Authorization", authorizationHeader())
                        .get()
                        .build();

                try (Response response = apiClient.execute(request)) {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (response.isSuccessful()) {
                        UserStats stats = apiClient.fromJson(responseBody, UserStats.class);
                        return ApiResult.success(stats);
                    } else {
                        return ApiResult.error("Failed to get stats: " + getErrorMessage(responseBody, response.code()));
                    }
                }
            } catch (IOException e) {
                return ApiResult.error("Connection error: " + e.getMessage());
            }
        });
    }

    private String authorizationHeader() {
        return sessionManager.getAuthorizationHeader();
    }

    /**
     * Inner class to hold API result (success or error)
     */
    public static class ApiResult<T> {
        private final T data;
        private final String error;
        private final boolean success;

        private ApiResult(T data, String error, boolean success) {
            this.data = data;
            this.error = error;
            this.success = success;
        }

        public static <T> ApiResult<T> success(T data) {
            return new ApiResult<>(data, null, true);
        }

        public static <T> ApiResult<T> error(String error) {
            return new ApiResult<>(null, error, false);
        }

        public T getData() {
            return data;
        }

        public String getError() {
            return error;
        }

        public boolean isSuccess() {
            return success;
        }
    }
}
