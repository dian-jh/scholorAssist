package com.zd.scgateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * 安全配置类
 * 统一管理网关安全相关配置
 * 
 * @author system
 * @since 2024-01-21
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "security")
public class SecurityConfig {

    /**
     * 是否启用安全功能
     */
    private Boolean enabled = true;

    /**
     * 是否启用权限校验
     */
    private Boolean authorizationEnabled = true;

    /**
     * 是否启用请求限流
     */
    private Boolean rateLimitEnabled = true;

    /**
     * 默认请求限流配置
     */
    private RateLimit defaultRateLimit = new RateLimit();

    /**
     * 特定路径的请求限流配置
     */
    private Map<String, RateLimit> pathRateLimits = Map.of(
        "/api/user/login", new RateLimit(5, 60), // 登录接口：每分钟5次
        "/api/user/register", new RateLimit(3, 300), // 注册接口：每5分钟3次
        "/api/ai/**", new RateLimit(10, 60) // AI接口：每分钟10次
    );

    /**
     * 敏感操作路径
     */
    private List<String> sensitivePaths = List.of(
        "/api/user/password",
        "/api/user/delete",
        "/api/system/**"
    );

    /**
     * 需要额外安全验证的路径
     */
    private List<String> highSecurityPaths = List.of(
        "/api/user/delete",
        "/api/system/config/**"
    );

    /**
     * CORS配置
     */
    private Cors cors = new Cors();

    /**
     * 安全头配置
     */
    private SecurityHeaders securityHeaders = new SecurityHeaders();

    /**
     * 请求限流配置
     */
    @Data
    public static class RateLimit {
        /**
         * 每个时间窗口允许的请求数
         */
        private Integer requests = 100;

        /**
         * 时间窗口大小（秒）
         */
        private Integer windowSeconds = 60;

        public RateLimit() {}

        public RateLimit(Integer requests, Integer windowSeconds) {
            this.requests = requests;
            this.windowSeconds = windowSeconds;
        }
    }

    /**
     * CORS配置
     */
    @Data
    public static class Cors {
        /**
         * 是否启用CORS
         */
        private Boolean enabled = true;

        /**
         * 允许的源
         */
        private List<String> allowedOrigins = List.of(
            "http://localhost:3000",
            "http://localhost:8080",
            "https://scholar-assist.com"
        );

        /**
         * 允许的方法
         */
        private List<String> allowedMethods = List.of(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        );

        /**
         * 允许的头
         */
        private List<String> allowedHeaders = List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With",
            "X-User-Id",
            "X-Username",
            "X-User-Role"
        );

        /**
         * 是否允许凭证
         */
        private Boolean allowCredentials = true;

        /**
         * 预检请求缓存时间（秒）
         */
        private Integer maxAge = 3600;
    }

    /**
     * 安全头配置
     */
    @Data
    public static class SecurityHeaders {
        /**
         * 是否启用安全头
         */
        private Boolean enabled = true;

        /**
         * X-Content-Type-Options
         */
        private String contentTypeOptions = "nosniff";

        /**
         * X-Frame-Options
         */
        private String frameOptions = "DENY";

        /**
         * X-XSS-Protection
         */
        private String xssProtection = "1; mode=block";

        /**
         * Strict-Transport-Security
         */
        private String strictTransportSecurity = "max-age=31536000; includeSubDomains";

        /**
         * Content-Security-Policy
         */
        private String contentSecurityPolicy = "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'";

        /**
         * Referrer-Policy
         */
        private String referrerPolicy = "strict-origin-when-cross-origin";
    }
}