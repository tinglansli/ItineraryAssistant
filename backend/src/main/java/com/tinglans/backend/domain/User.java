package com.tinglans.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 用户实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    /**
     * 用户ID
     */
    private String id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 密码哈希
     */
    private String passwordHash;
    
    /**
     * 用户偏好（如：美食、动漫、亲子、历史文化等）
     */
    private List<String> preferences;
    
    /**
     * 创建时间
     */
    private Instant createdAt;
    
    /**
     * 最后登录时间
     */
    private Instant lastLoginAt;
}
