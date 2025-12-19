package com.zd.scgateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zd.sccommon.model.UserContext;
import com.zd.scgateway.config.JwtConfig;
import com.zd.scgateway.exception.JwtAuthenticationException;
import com.zd.scgateway.utils.ReactiveJwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Gateway JWT认证过滤器
 * 基于Spring Cloud Gateway的响应式JWT认证实现
 * * @author system
 * @since 2024-01-21
 */
@Slf4j
@Component
public class GatewayJwtAuthenticationFilter extends AbstractGatewayFilterFactory<GatewayJwtAuthenticationFilter.Config> {

    private final ReactiveJwtUtil reactiveJwtUtil;
    private final JwtConfig jwtConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 用户信息在Exchange中的属性键
     */
    public static final String USER_INFO_ATTRIBUTE = "USER_INFO";

    public GatewayJwtAuthenticationFilter(ReactiveJwtUtil reactiveJwtUtil, JwtConfig jwtConfig) {
        super(Config.class);
        this.reactiveJwtUtil = reactiveJwtUtil;
        this.jwtConfig = jwtConfig;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            String method = request.getMethod().name();

            // 【*** BUG 修复 ***】
            // 1. 立即处理OPTIONS预检请求
            // 必须在任何认证逻辑之前，让Spring Cloud Gateway的globalcors配置来处理
            if ("OPTIONS".equals(method)) {
                if (jwtConfig.getDebug()) {
                    log.debug("OPTIONS预检请求，放行: {} {}", method, path);
                }
                return chain.filter(exchange);
            }
            // 【*** 修复结束 ***】


            if (jwtConfig.getDebug()) {
                log.debug("处理请求: {} {}", method, path);
            }

            // 2. 检查JWT功能是否启用
            if (!jwtConfig.getEnabled()) {
                if (jwtConfig.getDebug()) {
                    log.debug("JWT认证已禁用，跳过验证");
                }
                return chain.filter(exchange);
            }

            // 3. 检查是否为不需要认证的路径
            if (isExcludedPath(path)) {
                if (jwtConfig.getDebug()) {
                    log.debug("路径无需认证: {}", path);
                }
                return chain.filter(exchange);
            }

            // 4. 提取并验证JWT token
            return extractAndValidateToken(request)
                    .flatMap(token -> reactiveJwtUtil.validateAndExtractUserInfo(token))
                    .flatMap(userInfo -> {
                        if (jwtConfig.getDebug()) {
                            log.debug("用户认证成功，userId: {}, username: {}, role: {}, status: {}",
                                    userInfo.userId(), userInfo.username(), userInfo.role(), userInfo.status());
                        }

                        try {
                            // 创建UserContext对象
                            UserContext userContext = UserContext.builder()
                                    .userId(userInfo.userId())
                                    .username(userInfo.username())
                                    .name(userInfo.name())
                                    .role(userInfo.role())
                                    .avatar(userInfo.avatar())
                                    .email(userInfo.email())
                                    .status(userInfo.status())
                                    .token(userInfo.token())
                                    .build();

                            // 将用户信息序列化为JSON字符串
                            String userInfoJson = objectMapper.writeValueAsString(userContext);

                            // 将用户信息存储到Exchange属性中并添加到请求头
                            ServerWebExchange modifiedExchange = exchange.mutate()
                                    .request(request.mutate()
                                            .header("USER_INFO", userInfoJson)
                                            .build())
                                    .build();

                            modifiedExchange.getAttributes().put(USER_INFO_ATTRIBUTE, userInfo);

                            return chain.filter(modifiedExchange);
                        } catch (JsonProcessingException e) {
                            log.error("序列化用户信息失败: {}", e.getMessage(), e);
                            return sendErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR.value(), "用户信息处理异常");
                        }
                    })
                    .onErrorResume(JwtAuthenticationException.class, e -> {
                        log.warn("JWT认证失败: {} - {}", e.getCode(), e.getMessage());
                        return sendErrorResponse(exchange, e.getCode(), e.getMessage());
                    })
                    .onErrorResume(Exception.class, e -> {
                        log.error("JWT认证过滤器异常: {}", e.getMessage(), e);
                        return sendErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR.value(), "认证服务异常");
                    });
        };
    }

    /**
     * 检查路径是否不需要认证
     * * @param requestPath 请求路径
     * @return 是否不需要认证
     */
    private boolean isExcludedPath(String requestPath) {
        return jwtConfig.getExcludedPaths().stream()
                .anyMatch(excludedPath -> requestPath.startsWith(excludedPath));
    }

    // --- [修改点 4] ---
    // 删除了 handleOptionsRequest 方法
    // --- [修改点 4 结束] ---

    /**
     * 提取并验证JWT token
     * * @param request ServerHttpRequest
     * @return JWT token的Mono
     */
    private Mono<String> extractAndValidateToken(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        return reactiveJwtUtil.extractTokenFromHeader(authorization);
    }

    /**
     * 发送错误响应
     * * @param exchange ServerWebExchange
     * @param code 错误码
     * @param message 错误消息
     * @return 响应的Mono
     */
    private Mono<Void> sendErrorResponse(ServerWebExchange exchange, int code, String message) {
        ServerHttpResponse response = exchange.getResponse();

        // 设置响应状态和头
        response.setStatusCode(HttpStatus.valueOf(code));
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        // 【CORS 修复】确保错误响应也包含CORS头，动态镜像请求来源
        try {
            ServerHttpRequest req = exchange.getRequest();
            String origin = req.getHeaders().getFirst(HttpHeaders.ORIGIN);
            if (origin != null && !origin.isEmpty()) {
                response.getHeaders().set("Access-Control-Allow-Origin", origin);
                response.getHeaders().set("Vary", "Origin");
            }
            response.getHeaders().set("Access-Control-Allow-Credentials", "true");
            response.getHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            response.getHeaders().set("Access-Control-Allow-Headers", "Authorization,Content-Type,Accept,Origin,X-Requested-With,X-User-Id,X-Username,X-User-Role,Range");
        } catch (Exception ignored) {}
        // 【CORS 修复结束】


        // 构建错误响应体
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", code);
        errorResponse.put("msg", message);
        errorResponse.put("data", null);
        errorResponse.put("timestamp", System.currentTimeMillis());

        try {
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("序列化错误响应失败: {}", e.getMessage(), e);
            return response.setComplete();
        }
    }

    /**
     * 从Exchange中获取当前用户信息
     * * @param exchange ServerWebExchange
     * @return 用户信息，如果未认证返回null
     */
    public static ReactiveJwtUtil.UserInfo getCurrentUserInfo(ServerWebExchange exchange) {
        return exchange.getAttribute(USER_INFO_ATTRIBUTE);
    }

    /**
     * 从Exchange中获取当前用户ID
     * * @param exchange ServerWebExchange
     * @return 用户ID，如果未认证返回null
     */
    public static String getCurrentUserId(ServerWebExchange exchange) {
        ReactiveJwtUtil.UserInfo userInfo = getCurrentUserInfo(exchange);
        return userInfo != null ? userInfo.userId() : null;
    }

    /**
     * 从Exchange中获取当前用户名
     * * @param exchange ServerWebExchange
     * @return 用户名，如果未认证返回null
     */
    public static String getCurrentUsername(ServerWebExchange exchange) {
        ReactiveJwtUtil.UserInfo userInfo = getCurrentUserInfo(exchange);
        return userInfo != null ? userInfo.username() : null;
    }

    /**
     * 从Exchange中获取当前用户角色
     * * @param exchange ServerWebExchange
     * @return 用户角色，如果未认证返回null
     */
    public static String getCurrentUserRole(ServerWebExchange exchange) {
        ReactiveJwtUtil.UserInfo userInfo = getCurrentUserInfo(exchange);
        return userInfo != null ? userInfo.role() : null;
    }

    /**
     * 检查当前用户是否已登录
     * * @param exchange ServerWebExchange
     * @return 是否已登录
     */
    public static boolean isUserLoggedIn(ServerWebExchange exchange) {
        return getCurrentUserInfo(exchange) != null;
    }

    /**
     * 检查当前用户是否具有指定角色
     * * @param exchange ServerWebExchange
     * @param role 角色
     * @return 是否具有角色
     */
    public static boolean hasRole(ServerWebExchange exchange, String role) {
        String currentRole = getCurrentUserRole(exchange);
        if (currentRole == null || role == null) {
            return false;
        }

        // 超级管理员拥有所有角色权限
        if ("super_admin".equals(currentRole)) {
            return true;
        }

        // 管理员拥有用户角色权限
        if ("admin".equals(currentRole) && "user".equals(role)) {
            return true;
        }

        return currentRole.equals(role);
    }

    /**
     * 配置类
     */
    public static class Config {
        // 可以在这里添加过滤器特定的配置参数
    }
}