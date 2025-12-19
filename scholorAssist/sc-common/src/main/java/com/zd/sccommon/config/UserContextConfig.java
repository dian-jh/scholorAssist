package com.zd.sccommon.config;

import com.zd.sccommon.interceptor.UserContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 用户上下文配置类
 * 
 * <p>注册用户上下文拦截器</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class UserContextConfig implements WebMvcConfigurer {

    private final UserContextInterceptor userContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userContextInterceptor)
                .addPathPatterns("/**")  // 拦截所有请求
                .excludePathPatterns(
                        "/error",           // 排除错误页面
                        "/favicon.ico",     // 排除图标请求
                        "/actuator/**",     // 排除监控端点
                        "/swagger-ui/**",   // 排除Swagger UI
                        "/v3/api-docs/**"   // 排除API文档
                )
                .order(1);  // 设置拦截器优先级
    }
}