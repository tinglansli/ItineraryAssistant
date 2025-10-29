package com.tinglans.backend.service;

import com.tinglans.backend.domain.User;
import com.tinglans.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
    private static final String PREFERENCE_DELIMITER = ";";

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
        log.debug("查看用户偏好: userId={}", userId);

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("用户不存在: " + userId);
        }

        User user = userOpt.get();
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

        return Arrays.stream(preferencesStr.split(PREFERENCE_DELIMITER))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
}
