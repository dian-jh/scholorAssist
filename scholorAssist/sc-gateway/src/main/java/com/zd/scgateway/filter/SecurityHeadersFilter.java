package com.zd.scgateway.filter;

import com.zd.scgateway.config.SecurityConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 安全头过滤器
 * 为所有响应添加安全相关的HTTP头
 * 
 * @author system
 * @since 2024-01-21
 */
@Slf4j
@Component
public class SecurityHeadersFilter extends AbstractGatewayFilterFactory<SecurityHeadersFilter.Config> {

    private final SecurityConfig securityConfig;

    public SecurityHeadersFilter(SecurityConfig securityConfig) {
        super(Config.class);
        this.securityConfig = securityConfig;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                if (securityConfig.getSecurityHeaders().getEnabled()) {
                    addSecurityHeaders(exchange);
                }
            }));
        };
    }

    /**
     * 添加安全头
     * 
     * @param exchange ServerWebExchange
     */
    private void addSecurityHeaders(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders headers = response.getHeaders();
        SecurityConfig.SecurityHeaders securityHeaders = securityConfig.getSecurityHeaders();

        try {
            // X-Content-Type-Options: 防止MIME类型嗅探攻击
            if (securityHeaders.getContentTypeOptions() != null) {
                headers.add("X-Content-Type-Options", securityHeaders.getContentTypeOptions());
            }

            // X-Frame-Options: 防止点击劫持攻击
            if (securityHeaders.getFrameOptions() != null) {
                headers.add("X-Frame-Options", securityHeaders.getFrameOptions());
            }

            // X-XSS-Protection: 启用XSS过滤
            if (securityHeaders.getXssProtection() != null) {
                headers.add("X-XSS-Protection", securityHeaders.getXssProtection());
            }

            // Strict-Transport-Security: 强制HTTPS
            if (securityHeaders.getStrictTransportSecurity() != null) {
                headers.add("Strict-Transport-Security", securityHeaders.getStrictTransportSecurity());
            }

            // Content-Security-Policy: 内容安全策略
            if (securityHeaders.getContentSecurityPolicy() != null) {
                headers.add("Content-Security-Policy", securityHeaders.getContentSecurityPolicy());
            }

            // Referrer-Policy: 控制Referer头的发送
            if (securityHeaders.getReferrerPolicy() != null) {
                headers.add("Referrer-Policy", securityHeaders.getReferrerPolicy());
            }

            // 添加自定义安全头
            headers.add("X-Powered-By", "Scholar-Assist-Gateway");
            headers.add("X-Request-ID", exchange.getRequest().getId());

            if (securityConfig.getEnabled()) {
                log.debug("已添加安全头到响应");
            }

        } catch (Exception e) {
            log.error("添加安全头失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 配置类
     */
    public static class Config {
        // 可以在这里添加安全头过滤器特定的配置参数
    }
}