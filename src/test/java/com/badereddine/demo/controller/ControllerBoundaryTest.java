package com.badereddine.demo.controller;

import com.badereddine.demo.payload.request.LoginRequest;
import com.badereddine.demo.payload.request.PasswordChangeRequest;
import com.badereddine.demo.payload.request.ProfileUpdateRequest;
import com.badereddine.demo.payload.request.SignupRequest;
import com.badereddine.demo.service.AdminUserService;
import com.badereddine.demo.service.AuthenticationService;
import com.badereddine.demo.service.ProfileService;
import com.badereddine.demo.service.UserStatisticsService;
import com.badereddine.demo.service.UserTransferService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ControllerBoundaryTest {

    private static final String ADMIN_POLICY = "hasRole('ADMIN')";
    private static final String PROFILE_POLICY = "hasRole('USER') or hasRole('ADMIN')";

    @Test
    void replacesTheLegacyControllerWithFiveFocusedOpenApiGroups() {
        assertBoundary(
                AuthenticationController.class,
                "Authentication",
                2,
                AuthenticationService.class
        );
        assertBoundary(
                ProfileController.class,
                "Profiles",
                4,
                ProfileService.class,
                AdminUserService.class
        );
        assertBoundary(
                AdminUserController.class,
                "User Administration",
                6,
                AdminUserService.class
        );
        assertBoundary(
                UserTransferController.class,
                "User Transfers",
                3,
                UserTransferService.class
        );
        assertBoundary(
                UserStatisticsController.class,
                "User Statistics",
                1,
                UserStatisticsService.class
        );

        assertThatThrownBy(() -> Class.forName("com.badereddine.demo.controller.UserController"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void preservesEveryMethodSecurityPolicyAfterTheSplit() throws Exception {
        assertNoPolicy(AuthenticationController.class, "authenticateUser", LoginRequest.class);
        assertNoPolicy(AuthenticationController.class, "registerUser", SignupRequest.class);

        assertPolicy(ProfileController.class, "getMyProfile", PROFILE_POLICY);
        assertPolicy(ProfileController.class, "getUserProfile", "isAuthenticated()", String.class);
        assertPolicy(ProfileController.class, "changePassword", PROFILE_POLICY, PasswordChangeRequest.class);
        assertPolicy(ProfileController.class, "updateProfile", PROFILE_POLICY, ProfileUpdateRequest.class);

        Arrays.stream(AdminUserController.class.getDeclaredMethods())
                .filter(this::isMappedEndpoint)
                .forEach(method -> assertThat(method.getAnnotation(PreAuthorize.class).value())
                        .as(method.getName())
                        .isEqualTo(ADMIN_POLICY));
        Arrays.stream(UserTransferController.class.getDeclaredMethods())
                .filter(this::isMappedEndpoint)
                .forEach(method -> assertThat(method.getAnnotation(PreAuthorize.class).value())
                        .as(method.getName())
                        .isEqualTo(ADMIN_POLICY));
        assertPolicy(UserStatisticsController.class, "getUserStats", ADMIN_POLICY);
    }

    @Test
    void preservesAllSixteenHttpOperationMappings() throws Exception {
        assertPost(AuthenticationController.class, "authenticateUser", "/auth", LoginRequest.class);
        assertPost(AuthenticationController.class, "registerUser", "/auth/register", SignupRequest.class);

        assertGet(ProfileController.class, "getMyProfile", "/users/me");
        assertGet(ProfileController.class, "getUserProfile", "/users/{username}", String.class);
        assertPut(ProfileController.class, "changePassword", "/users/me/password", PasswordChangeRequest.class);
        assertPut(ProfileController.class, "updateProfile", "/users/me", ProfileUpdateRequest.class);

        assertGet(
                AdminUserController.class,
                "getAllUsers",
                "/users",
                int.class,
                int.class,
                String.class,
                String.class,
                String.class
        );
        assertGet(AdminUserController.class, "getUserById", "/users/id/{id}", Long.class);
        assertDelete(AdminUserController.class, "deleteUser", "/users/{id}", Long.class);
        assertPut(
                AdminUserController.class,
                "updateUser",
                "/users/{id}",
                Long.class,
                ProfileUpdateRequest.class
        );
        assertPatch(
                AdminUserController.class,
                "changeUserRole",
                "/users/{id}/role",
                Long.class,
                String.class
        );
        assertPatch(
                AdminUserController.class,
                "toggleUserStatus",
                "/users/{id}/status",
                Long.class,
                boolean.class
        );

        assertGet(UserTransferController.class, "generateUsers", "/users/generate/{count}", int.class, int.class);
        assertPost(UserTransferController.class, "batchUsers", "/users/batch", MultipartFile.class);
        assertGet(UserTransferController.class, "exportUsersToCsv", "/users/export/csv", String.class);

        assertGet(UserStatisticsController.class, "getUserStats", "/stats/users");
    }

    private void assertBoundary(
            Class<?> controllerType,
            String tagName,
            int endpointCount,
            Class<?>... dependencies
    ) {
        RequestMapping requestMapping = controllerType.getAnnotation(RequestMapping.class);
        Tag tag = controllerType.getAnnotation(Tag.class);

        assertThat(requestMapping.value()).containsExactly("/api");
        assertThat(tag.name()).isEqualTo(tagName);
        assertThat(controllerType.getDeclaredConstructors()).hasSize(1);
        assertThat(controllerType.getDeclaredConstructors()[0].getParameterTypes())
                .containsExactly(dependencies);
        assertThat(Arrays.stream(controllerType.getDeclaredMethods()).filter(this::isMappedEndpoint))
                .hasSize(endpointCount);
    }

    private void assertNoPolicy(Class<?> controllerType, String methodName, Class<?>... parameterTypes)
            throws Exception {
        Method method = controllerType.getDeclaredMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(PreAuthorize.class)).isNull();
    }

    private void assertPolicy(
            Class<?> controllerType,
            String methodName,
            String policy,
            Class<?>... parameterTypes
    ) throws Exception {
        Method method = controllerType.getDeclaredMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(PreAuthorize.class).value()).isEqualTo(policy);
    }

    private void assertGet(
            Class<?> controllerType,
            String methodName,
            String path,
            Class<?>... parameterTypes
    ) throws Exception {
        assertThat(controllerType.getDeclaredMethod(methodName, parameterTypes)
                .getAnnotation(GetMapping.class).value()).containsExactly(path);
    }

    private void assertPost(
            Class<?> controllerType,
            String methodName,
            String path,
            Class<?>... parameterTypes
    ) throws Exception {
        assertThat(controllerType.getDeclaredMethod(methodName, parameterTypes)
                .getAnnotation(PostMapping.class).value()).containsExactly(path);
    }

    private void assertPut(
            Class<?> controllerType,
            String methodName,
            String path,
            Class<?>... parameterTypes
    ) throws Exception {
        assertThat(controllerType.getDeclaredMethod(methodName, parameterTypes)
                .getAnnotation(PutMapping.class).value()).containsExactly(path);
    }

    private void assertPatch(
            Class<?> controllerType,
            String methodName,
            String path,
            Class<?>... parameterTypes
    ) throws Exception {
        assertThat(controllerType.getDeclaredMethod(methodName, parameterTypes)
                .getAnnotation(PatchMapping.class).value()).containsExactly(path);
    }

    private void assertDelete(
            Class<?> controllerType,
            String methodName,
            String path,
            Class<?>... parameterTypes
    ) throws Exception {
        assertThat(controllerType.getDeclaredMethod(methodName, parameterTypes)
                .getAnnotation(DeleteMapping.class).value()).containsExactly(path);
    }

    private boolean isMappedEndpoint(Method method) {
        return method.isAnnotationPresent(GetMapping.class)
                || method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(PatchMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class);
    }
}
