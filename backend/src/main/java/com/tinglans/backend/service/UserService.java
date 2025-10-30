package com.tinglans.backend.service;

import com.tinglans.backend.common.BusinessException;
import com.tinglans.backend.common.ResponseCode;
import com.tinglans.backend.domain.User;
import com.tinglans.backend.repository.UserRepository;
import com.tinglans.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * 用户业务逻辑层
 * 负责：用户偏好管理等业务流程
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    private static final String PREFERENCE_DELIMITER = ";";

    // ==================== 校验方法 ====================

    /**
     * 校验用户ID
     */
    public void validateUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException(ResponseCode.INVALID_PARAM, "用户ID不能为空");
        }
    }

    /**
     * 校验并获取用户
     */
    public User validateAndGetUser(String userId) throws ExecutionException, InterruptedException {
        Optional<User> userOpt = getUserById(userId);
        if (userOpt.isEmpty()) {
            throw new BusinessException(ResponseCode.USER_NOT_FOUND);
        }
        return userOpt.get();
    }

    // ==================== 业务方法 ====================

    /**
     * 根据ID获取用户
     *
     * @param userId 用户ID
     * @return 用户对象
     */
    public Optional<User> getUserById(String userId) throws ExecutionException, InterruptedException {
        log.debug("获取用户: userId={}", userId);
        return userRepository.findById(userId);
    }

    /**
     * 更新用户偏好
     *
     * @param userId           用户ID
     * @param preferencesStr   偏好字符串（用 ";" 分隔，如："美食;历史文化;寺庙"）
     */
    public void updatePreferences(String userId, String preferencesStr) 
            throws ExecutionException, InterruptedException {
        log.info("更新用户偏好: userId={}, input={}", userId, preferencesStr);

        // 解析偏好字符串
        List<String> preferences = parsePreferencesString(preferencesStr);

        // 更新到数据库
        userRepository.updatePreferences(userId, preferences);

        log.info("偏好更新成功: userId={}, 偏好数量={}", userId, preferences.size());
    }

    /**
     * 查看用户偏好
     *
     * @param userId 用户ID
     * @return 偏好字符串（用 ";" 分隔）
     */
    public String getPreferences(String userId) throws ExecutionException, InterruptedException {
        validateUserId(userId);
        
        log.debug("查看用户偏好: userId={}", userId);

        User user = validateAndGetUser(userId);
        List<String> preferences = user.getPreferences();

        if (preferences == null || preferences.isEmpty()) {
            return "";
        }

        String result = String.join(PREFERENCE_DELIMITER, preferences);
        log.debug("用户偏好: userId={}, preferences={}", userId, result);
        return result;
    }

    /**
     * 获取用户偏好列表
     */
    public List<String> getPreferencesList(String userId) throws ExecutionException, InterruptedException {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return new ArrayList<>();
        }

        User user = userOpt.get();
        return user.getPreferences() != null ? user.getPreferences() : new ArrayList<>();
    }

    /**
     * 解析偏好字符串为列表
     */
    private List<String> parsePreferencesString(String preferencesStr) {
        if (preferencesStr == null || preferencesStr.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // 支持英文分号 ";" 和中文分号 "；" 两种分隔符
        // 使用正则表达式进行分割
        return Arrays.stream(preferencesStr.split("[;；]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    // ==================== 认证方法 ====================

    /**
     * 用户注册
     *
     * @param username 用户名
     * @param password 密码
     * @return 新创建的用户ID
     */
    public String register(String username, String password) throws ExecutionException, InterruptedException {
        log.info("用户注册: username={}", username);

        // 参数校验
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BusinessException(ResponseCode.INVALID_PARAM, "用户名和密码不能为空");
        }

        if (username.length() < 3 || username.length() > 20) {
            throw new BusinessException(ResponseCode.INVALID_PARAM, "用户名长度必须在3-20个字符之间");
        }

        if (password.length() < 6) {
            throw new BusinessException(ResponseCode.INVALID_PARAM, "密码长度至少6个字符");
        }

        // 检查用户名是否已存在
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ResponseCode.USER_ALREADY_EXISTS);
        }

        // 创建新用户
        String userId = "user-" + UUID.randomUUID().toString();
        String passwordHash = passwordEncoder.encode(password);

        User newUser = User.builder()
                .id(userId)
                .username(username)
                .passwordHash(passwordHash)
                .preferences(new ArrayList<>())
                .createdAt(Instant.now())
                .build();

        userRepository.save(newUser);

        log.info("用户注册成功: userId={}, username={}", userId, username);
        return userId;
    }

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return JWT Token
     */
    public String login(String username, String password) throws ExecutionException, InterruptedException {
        log.info("用户登录: username={}", username);

        // 参数校验
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BusinessException(ResponseCode.INVALID_PARAM, "用户名和密码不能为空");
        }

        // 查找用户
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new BusinessException(ResponseCode.INVALID_CREDENTIALS);
        }

        User user = userOpt.get();

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ResponseCode.INVALID_CREDENTIALS);
        }

        // 更新最后登录时间
        userRepository.updateLastLoginAt(user.getId());

        // 生成Token
        String token = jwtUtil.generateToken(user.getId());

        log.info("用户登录成功: userId={}, username={}", user.getId(), username);
        return token;
    }
}
