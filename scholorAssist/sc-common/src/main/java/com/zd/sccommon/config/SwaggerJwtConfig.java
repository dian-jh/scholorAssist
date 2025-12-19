package com.zd.sccommon.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger JWT配置类
 * 用于Swagger测试环境的JWT配置
 * 
 * @author System
 * @since 2024-01-21
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "swagger.jwt")
public class SwaggerJwtConfig {

    /**
     * JWT密钥（与网关保持一致）
     */
    private String secret = "mySecretKey123456789012345678901234567890";

    /**
     * 是否启用Swagger JWT验证
     */
    private Boolean enabled = true;

    /**
     * 是否启用调试模式
     */
    private Boolean debug = false;

    /**
     * Swagger相关路径
     */
    private List<String> swaggerPaths = List.of(
        "/swagger-ui",
        "/v3/api-docs",
        "/swagger-resources",
        "/webjars",
        "/doc.html",
        "/favicon.ico"
    );

    /**
     * 不需要token验证的路径
     */
    private List<String> excludedPaths = List.of(
        "/api/users/register",
        "/api/users/login",
        "/api/users/check-username",
        "/api/users/check-email",
        "/actuator",
        "/error"
    );
}