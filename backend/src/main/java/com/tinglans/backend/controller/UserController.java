package com.tinglans.backend.controller;

import com.tinglans.backend.common.ApiResponse;
import com.tinglans.backend.domain.User;
import com.tinglans.backend.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * 职责：路由分发、参数绑定
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    /**
     * 获取用户信息
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<User>> getUser(@PathVariable String userId) throws Exception {
        User user = userService.validateAndGetUser(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * 获取用户偏好
     */
    @GetMapping("/{userId}/preferences")
    public ResponseEntity<ApiResponse<String>> getPreferences(@PathVariable String userId) throws Exception {
        String preferences = userService.getPreferences(userId);
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }

    /**
     * 更新用户偏好
     */
    @PutMapping("/{userId}/preferences")
    public ResponseEntity<ApiResponse<Void>> updatePreferences(
            @PathVariable String userId,
            @RequestBody UpdatePreferencesRequest request) throws Exception {
        userService.updatePreferences(userId, request.getPreferences());
        return ResponseEntity.ok(ApiResponse.success("偏好更新成功", null));
    }

    @Data
    public static class UpdatePreferencesRequest {
        private String preferences;
    }
}
