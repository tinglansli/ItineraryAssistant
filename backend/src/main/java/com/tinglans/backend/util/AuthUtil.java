package com.tinglans.backend.util;

import com.tinglans.backend.common.BusinessException;
import com.tinglans.backend.common.ResponseCode;
import com.tinglans.backend.interceptor.JwtInterceptor;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 认证工具类
 * 用于从HttpServletRequest中获取当前登录用户信息
 */
public class AuthUtil {

    /**
     * 从HttpServletRequest中获取当前登录用户的ID
     * 该ID由JwtInterceptor在验证token后放入request attributes
     *
     * @param request HttpServletRequest
     * @return 当前登录用户的ID
     * @throws BusinessException 如果无法获取用户ID
     */
    public static String getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(JwtInterceptor.USER_ID_ATTRIBUTE);
        
        if (userId == null) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "无法获取用户信息");
        }
        
        return userId.toString();
    }
}
