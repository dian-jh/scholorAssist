package com.zd.sccommon.config;

import com.zd.sccommon.interceptor.SwaggerTokenInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Swagger拦截器配置类
 * 
 * <p>注册SwaggerTokenInterceptor到Spring容器</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SwaggerInterceptorConfig implements WebMvcConfigurer {

    private final SwaggerTokenInterceptor swaggerTokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(swaggerTokenInterceptor)
                .addPathPatterns("/**")  // 拦截所有请求
                .excludePathPatterns(
                        "/error",           // 排除错误页面
                        "/favicon.ico",     // 排除图标请求
                        "/actuator/**"      // 排除监控端点
                )
                .order(0);  // 设置拦截器优先级（比UserContextInterceptor优先级高）
    }
}