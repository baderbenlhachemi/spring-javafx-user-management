package com.badereddine.demo.controller;

import com.badereddine.demo.payload.response.UserListResponse;
import com.badereddine.demo.service.AdminUserService;
import com.badereddine.demo.service.UserPaginationPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserPaginationControllerTest {

    @Mock
    private AdminUserService adminUserService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminUserController controller = new AdminUserController(adminUserService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void preservesDefaultsAndPaginationResponseFields() throws Exception {
        when(adminUserService.getAllUsers(0, 10, "username", "asc", null))
                .thenReturn(new UserListResponse(java.util.List.of(), 0, 0, 0, 10));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.size").value(10));

        verify(adminUserService).getAllUsers(0, 10, "username", "asc", null);
    }

    @Test
    void acceptsMaximumPageSizeAndDescendingClientSort() throws Exception {
        when(adminUserService.getAllUsers(
                2,
                UserPaginationPolicy.MAX_PAGE_SIZE,
                "lastLogin",
                "desc",
                null
        )).thenReturn(new UserListResponse(
                java.util.List.of(),
                2,
                0,
                0,
                UserPaginationPolicy.MAX_PAGE_SIZE
        ));

        mockMvc.perform(get("/api/users")
                        .param("page", "2")
                        .param("size", Integer.toString(UserPaginationPolicy.MAX_PAGE_SIZE))
                        .param("sortBy", "lastLogin")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk());

        verify(adminUserService).getAllUsers(
                2,
                UserPaginationPolicy.MAX_PAGE_SIZE,
                "lastLogin",
                "desc",
                null
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"username", "email", "firstName", "company", "enabled", "lastLogin"})
    void acceptsEveryClientSortField(String sortBy) throws Exception {
        when(adminUserService.getAllUsers(0, 10, sortBy, "asc", null))
                .thenReturn(new UserListResponse(java.util.List.of(), 0, 0, 0, 10));

        mockMvc.perform(get("/api/users").param("sortBy", sortBy))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -10})
    void rejectsInvalidPagesWithStableResponse(int page) throws Exception {
        when(adminUserService.getAllUsers(page, 10, "username", "asc", null))
                .thenThrow(new IllegalArgumentException(UserPaginationPolicy.INVALID_PAGE_MESSAGE));

        mockMvc.perform(get("/api/users").param("page", Integer.toString(page)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(UserPaginationPolicy.INVALID_PAGE_MESSAGE));

        verify(adminUserService).getAllUsers(page, 10, "username", "asc", null);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 101})
    void rejectsInvalidPageSizesWithStableResponse(int size) throws Exception {
        when(adminUserService.getAllUsers(0, size, "username", "asc", null))
                .thenThrow(new IllegalArgumentException(UserPaginationPolicy.INVALID_SIZE_MESSAGE));

        mockMvc.perform(get("/api/users").param("size", Integer.toString(size)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(UserPaginationPolicy.INVALID_SIZE_MESSAGE));

        verify(adminUserService).getAllUsers(0, size, "username", "asc", null);
    }

    @Test
    void rejectsNonAllowListedSortFieldWithStableResponse() throws Exception {
        when(adminUserService.getAllUsers(0, 10, "password", "asc", null))
                .thenThrow(new IllegalArgumentException(UserPaginationPolicy.INVALID_SORT_FIELD_MESSAGE));

        mockMvc.perform(get("/api/users").param("sortBy", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(UserPaginationPolicy.INVALID_SORT_FIELD_MESSAGE));

        verify(adminUserService).getAllUsers(0, 10, "password", "asc", null);
    }

    @Test
    void rejectsInvalidSortDirectionWithStableResponse() throws Exception {
        when(adminUserService.getAllUsers(0, 10, "username", "sideways", null))
                .thenThrow(new IllegalArgumentException(UserPaginationPolicy.INVALID_SORT_DIRECTION_MESSAGE));

        mockMvc.perform(get("/api/users").param("sortDir", "sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(UserPaginationPolicy.INVALID_SORT_DIRECTION_MESSAGE));

        verify(adminUserService).getAllUsers(0, 10, "username", "sideways", null);
    }
}
