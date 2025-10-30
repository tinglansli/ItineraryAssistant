package com.tinglans.backend.config;

import com.tinglans.backend.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置
 * 配置拦截器和跨域等
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                // 需要鉴权的路径
                .addPathPatterns(
                        "/api/users/me",             // 获取当前用户信息
                        "/api/users/preferences",    // 用户偏好相关
                        "/api/trips/**",             // 所有行程相关接口
                        "/api/expenses/**",          // 所有开销相关接口
                        "/api/budgets/**",           // 所有预算相关接口
                        "/api/speech/**"             // 语音识别
                )
                // 不需要鉴权的路径
                .excludePathPatterns(
                        "/api/users/register",       // 注册
                        "/api/users/login"           // 登录
                );
    }
}
