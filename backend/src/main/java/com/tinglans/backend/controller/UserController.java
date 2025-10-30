package com.tinglans.backend.controller;

import com.tinglans.backend.common.ApiResponse;
import com.tinglans.backend.domain.User;
import com.tinglans.backend.service.UserService;
import com.tinglans.backend.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
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
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@RequestBody RegisterRequest request) throws Exception {
        String userId = userService.register(request.getUsername(), request.getPassword());
        RegisterResponse response = new RegisterResponse();
        response.setUserId(userId);
        response.setUsername(request.getUsername());
        return ResponseEntity.ok(ApiResponse.success("注册成功", response));
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) throws Exception {
        String token = userService.login(request.getUsername(), request.getPassword());
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        return ResponseEntity.ok(ApiResponse.success("登录成功", response));
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<User>> getUser(@PathVariable String userId) throws Exception {
        User user = userService.validateAndGetUser(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * 获取当前用户偏好
     */
    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<String>> getPreferences(HttpServletRequest httpRequest) throws Exception {
        String userId = AuthUtil.getCurrentUserId(httpRequest);
        String preferences = userService.getPreferences(userId);
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }

    /**
     * 更新当前用户偏好
     */
    @PutMapping("/preferences")
    public ResponseEntity<ApiResponse<Void>> updatePreferences(
            @RequestBody UpdatePreferencesRequest request,
            HttpServletRequest httpRequest) throws Exception {
        String userId = AuthUtil.getCurrentUserId(httpRequest);
        userService.updatePreferences(userId, request.getPreferences());
        return ResponseEntity.ok(ApiResponse.success("偏好更新成功", null));
    }

    @Data
    public static class RegisterRequest {
        private String username;
        private String password;
    }

    @Data
    public static class RegisterResponse {
        private String userId;
        private String username;
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class LoginResponse {
        private String token;
    }

    @Data
    public static class UpdatePreferencesRequest {
        private String preferences;
    }
}
