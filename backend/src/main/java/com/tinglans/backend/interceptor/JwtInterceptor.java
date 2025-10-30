package com.tinglans.backend.interceptor;

import com.tinglans.backend.common.BusinessException;
import com.tinglans.backend.common.ResponseCode;
import com.tinglans.backend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT拦截器
 * 用于验证请求中的Token并提取用户ID
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String USER_ID_ATTRIBUTE = "userId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 获取Authorization header
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        if (!StringUtils.hasText(authHeader)) {
            log.warn("请求缺少Authorization header");
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "未提供认证凭证");
        }

        if (!authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Authorization header格式错误");
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "认证凭证格式错误");
        }

        // 提取Token
        String token = authHeader.substring(BEARER_PREFIX.length());
        
        if (!StringUtils.hasText(token)) {
            log.warn("Token为空");
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "认证凭证为空");
        }

        // 验证Token
        if (!jwtUtil.validateToken(token)) {
            log.warn("Token验证失败");
            if (jwtUtil.isTokenExpired(token)) {
                throw new BusinessException(ResponseCode.TOKEN_EXPIRED);
            }
            throw new BusinessException(ResponseCode.INVALID_TOKEN);
        }

        // 从Token中提取用户ID并存入request attributes
        String userId = jwtUtil.getUserIdFromToken(token);
        request.setAttribute(USER_ID_ATTRIBUTE, userId);
        
        log.debug("Token验证成功, userId={}", userId);
        return true;
    }
}
