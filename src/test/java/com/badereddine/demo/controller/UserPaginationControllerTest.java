package com.badereddine.demo.controller;

import com.badereddine.demo.service.UserPaginationPolicy;
import com.badereddine.demo.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserPaginationControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserController controller = UserControllerTestFactory.builder()
                .userService(userService)
                .build();
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void preservesDefaultsAndPaginationResponseFields() throws Exception {
        when(userService.findAll(any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(0), 0));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.size").value(10));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userService).findAll(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("username")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("username").isAscending()).isTrue();
    }

    @Test
    void acceptsMaximumPageSizeAndDescendingClientSort() throws Exception {
        when(userService.findAll(any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(0), 0));

        mockMvc.perform(get("/api/users")
                        .param("page", "2")
                        .param("size", Integer.toString(UserPaginationPolicy.MAX_PAGE_SIZE))
                        .param("sortBy", "lastLogin")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userService).findAll(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(UserPaginationPolicy.MAX_PAGE_SIZE);
        assertThat(pageable.getSort().getOrderFor("lastLogin")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("lastLogin").isDescending()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"username", "email", "firstName", "company", "enabled", "lastLogin"})
    void acceptsEveryClientSortField(String sortBy) throws Exception {
        when(userService.findAll(any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(0), 0));

        mockMvc.perform(get("/api/users").param("sortBy", sortBy))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -10})
    void rejectsInvalidPagesWithStableResponse(int page) throws Exception {
        mockMvc.perform(get("/api/users").param("page", Integer.toString(page)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(UserPaginationPolicy.INVALID_PAGE_MESSAGE));

        verifyNoInteractions(userService);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 101})
    void rejectsInvalidPageSizesWithStableResponse(int size) throws Exception {
        mockMvc.perform(get("/api/users").param("size", Integer.toString(size)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(UserPaginationPolicy.INVALID_SIZE_MESSAGE));

        verifyNoInteractions(userService);
    }

    @Test
    void rejectsNonAllowListedSortFieldWithStableResponse() throws Exception {
        mockMvc.perform(get("/api/users").param("sortBy", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(UserPaginationPolicy.INVALID_SORT_FIELD_MESSAGE));

        verifyNoInteractions(userService);
    }

    @Test
    void rejectsInvalidSortDirectionWithStableResponse() throws Exception {
        mockMvc.perform(get("/api/users").param("sortDir", "sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(UserPaginationPolicy.INVALID_SORT_DIRECTION_MESSAGE));

        verifyNoInteractions(userService);
    }
}
