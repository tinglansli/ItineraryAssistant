package com.tinglans.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinglans.backend.common.BusinessException;
import com.tinglans.backend.common.ResponseCode;
import com.tinglans.backend.domain.User;
import com.tinglans.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 测试类
 */
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private User testUser;
    private String testUserId;

    @BeforeEach
    void setUp() {
        testUserId = "user-123";

        testUser = User.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@example.com")
                .preferences(Arrays.asList("喜欢历史文化", "预算中等"))
                .build();
    }

    @Test
    void testGetUser_success() throws Exception {
        // Given
        when(userService.validateAndGetUser(testUserId)).thenReturn(testUser);

        // When & Then
        mockMvc.perform(get("/api/users/{userId}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(testUserId))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));

        verify(userService, times(1)).validateAndGetUser(testUserId);
    }

    @Test
    void testGetUser_userNotFound() throws Exception {
        // Given
        when(userService.validateAndGetUser("non-existent-user"))
                .thenThrow(new BusinessException(ResponseCode.USER_NOT_FOUND));

        // When & Then
        mockMvc.perform(get("/api/users/{userId}", "non-existent-user"))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).validateAndGetUser("non-existent-user");
    }

    @Test
    void testGetPreferences_success() throws Exception {
        // Given
        String preferences = "喜欢历史文化,预算中等";
        when(userService.getPreferences(testUserId)).thenReturn(preferences);

        // When & Then
        mockMvc.perform(get("/api/users/{userId}/preferences", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(preferences));

        verify(userService, times(1)).getPreferences(testUserId);
    }

    @Test
    void testGetPreferences_userNotFound() throws Exception {
        // Given
        when(userService.getPreferences("non-existent-user"))
                .thenThrow(new BusinessException(ResponseCode.USER_NOT_FOUND));

        // When & Then
        mockMvc.perform(get("/api/users/{userId}/preferences", "non-existent-user"))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).getPreferences("non-existent-user");
    }

    @Test
    void testGetPreferences_emptyPreferences() throws Exception {
        // Given
        when(userService.getPreferences(testUserId)).thenReturn("");

        // When & Then
        mockMvc.perform(get("/api/users/{userId}/preferences", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(""));

        verify(userService, times(1)).getPreferences(testUserId);
    }

    @Test
    void testUpdatePreferences_success() throws Exception {
        // Given
        UserController.UpdatePreferencesRequest request = new UserController.UpdatePreferencesRequest();
        request.setPreferences("喜欢自然风光,预算较高");

        doNothing().when(userService).updatePreferences(eq(testUserId), eq("喜欢自然风光,预算较高"));

        // When & Then
        mockMvc.perform(put("/api/users/{userId}/preferences", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("偏好更新成功"));

        verify(userService, times(1)).updatePreferences(eq(testUserId), eq("喜欢自然风光,预算较高"));
    }

    @Test
    void testUpdatePreferences_userNotFound() throws Exception {
        // Given
        UserController.UpdatePreferencesRequest request = new UserController.UpdatePreferencesRequest();
        request.setPreferences("喜欢自然风光");

        doThrow(new BusinessException(ResponseCode.USER_NOT_FOUND))
                .when(userService).updatePreferences(eq("non-existent-user"), eq("喜欢自然风光"));

        // When & Then
        mockMvc.perform(put("/api/users/{userId}/preferences", "non-existent-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).updatePreferences(eq("non-existent-user"), eq("喜欢自然风光"));
    }

    @Test
    void testUpdatePreferences_emptyPreferences() throws Exception {
        // Given
        UserController.UpdatePreferencesRequest request = new UserController.UpdatePreferencesRequest();
        request.setPreferences("");

        doNothing().when(userService).updatePreferences(eq(testUserId), eq(""));

        // When & Then
        mockMvc.perform(put("/api/users/{userId}/preferences", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("偏好更新成功"));

        verify(userService, times(1)).updatePreferences(eq(testUserId), eq(""));
    }

    @Test
    void testUpdatePreferences_invalidParam() throws Exception {
        // Given
        UserController.UpdatePreferencesRequest request = new UserController.UpdatePreferencesRequest();
        request.setPreferences("invalid preferences format!!!!");

        doThrow(new BusinessException(ResponseCode.INVALID_PARAM, "偏好格式无效"))
                .when(userService).updatePreferences(eq(testUserId), eq("invalid preferences format!!!!"));

        // When & Then
        mockMvc.perform(put("/api/users/{userId}/preferences", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, times(1)).updatePreferences(eq(testUserId), eq("invalid preferences format!!!!"));
    }
}
