package com.zd.scgateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * JWT配置类
 * 统一管理JWT相关配置
 * 
 * @author system
 * @since 2024-01-21
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * JWT密钥
     */
    private String secret = "mySecretKey123456789012345678901234567890";

    /**
     * 默认过期时间（秒）
     */
    private Long defaultExpiration = 24 * 3600L; // 24小时

    /**
     * 记住登录过期时间（秒）
     */
    private Long rememberMeExpiration = 7 * 24 * 3600L; // 7天

    /**
     * 不需要认证的路径
     */
    private List<String> excludedPaths = List.of(
        "/api/users/register",
        "/api/users/login", 
        "/api/users/check-username",
        "/api/users/check-email",
        "/actuator",
        "/swagger-ui",
        "/v3/api-docs",
        "/favicon.ico",
        "/error"
    );

    /**
     * 允许的请求头
     */
    private List<String> allowedHeaders = List.of(
        "Authorization",
        "Content-Type",
        "Accept",
        "Origin",
        "X-Requested-With"
    );

    /**
     * 是否启用JWT校验
     */
    private Boolean enabled = true;

    /**
     * 是否启用调试模式
     */
    private Boolean debug = false;
}