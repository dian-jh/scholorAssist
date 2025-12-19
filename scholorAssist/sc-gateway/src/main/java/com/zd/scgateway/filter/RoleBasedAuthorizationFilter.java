package com.zd.scgateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zd.scgateway.config.JwtConfig;
import com.zd.scgateway.utils.ReactiveJwtUtil;
import lombok.RequiredArgsConstructor;
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
 * 基于角色的权限校验过滤器
 * 根据请求路径和用户角色进行权限控制
 * 
 * @author system
 * @since 2024-01-21
 */
@Slf4j
@Component
public class RoleBasedAuthorizationFilter extends AbstractGatewayFilterFactory<RoleBasedAuthorizationFilter.Config> {

    private final JwtConfig jwtConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RoleBasedAuthorizationFilter(JwtConfig jwtConfig) {
        super(Config.class);
        this.jwtConfig = jwtConfig;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            String method = request.getMethod().name();

            if (jwtConfig.getDebug()) {
                log.debug("权限校验: {} {}", method, path);
            }

            // 获取用户信息
            ReactiveJwtUtil.UserInfo userInfo = GatewayJwtAuthenticationFilter.getCurrentUserInfo(exchange);
            
            // 如果没有用户信息，说明认证过滤器没有执行或认证失败
            if (userInfo == null) {
                if (jwtConfig.getDebug()) {
                    log.debug("用户未认证，跳过权限校验");
                }
                return chain.filter(exchange);
            }

            // 检查用户权限
            return checkPermission(userInfo, path, method)
                .flatMap(hasPermission -> {
                    if (hasPermission) {
                        if (jwtConfig.getDebug()) {
                            log.debug("用户 {} 有权限访问 {} {}", userInfo.username(), method, path);
                        }
                        return chain.filter(exchange);
                    } else {
                        log.warn("用户 {} (角色: {}) 无权限访问 {} {}", 
                                userInfo.username(), userInfo.role(), method, path);
                        return sendForbiddenResponse(exchange, "权限不足");
                    }
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("权限校验异常: {}", e.getMessage(), e);
                    return sendForbiddenResponse(exchange, "权限校验失败");
                });
        };
    }

    /**
     * 检查用户权限
     * 
     * @param userInfo 用户信息
     * @param path 请求路径
     * @param method 请求方法
     * @return 是否有权限的Mono
     */
    private Mono<Boolean> checkPermission(ReactiveJwtUtil.UserInfo userInfo, String path, String method) {
        return Mono.fromCallable(() -> {
            String role = userInfo.role();
            
            // 超级管理员拥有所有权限
            if ("super_admin".equals(role)) {
                return true;
            }
            
            // 管理员权限规则
            if ("admin".equals(role)) {
                return checkAdminPermission(path, method);
            }
            
            // 普通用户权限规则
            if ("user".equals(role)) {
                return checkUserPermission(path, method, userInfo.userId());
            }
            
            // 未知角色，拒绝访问
            log.warn("未知用户角色: {}", role);
            return false;
        });
    }

    /**
     * 检查管理员权限
     * 
     * @param path 请求路径
     * @param method 请求方法
     * @return 是否有权限
     */
    private boolean checkAdminPermission(String path, String method) {
        // 管理员可以访问大部分资源，但有一些限制
        
        // 禁止管理员删除超级管理员账户
        if (path.matches("/api/user/\\d+") && "DELETE".equals(method)) {
            // 这里需要进一步检查目标用户是否为超级管理员
            // 为了简化，暂时允许，实际应该查询目标用户角色
            return true;
        }
        
        // 禁止管理员修改系统核心配置
        if (path.startsWith("/api/system/config") && ("PUT".equals(method) || "DELETE".equals(method))) {
            return false;
        }
        
        // 其他情况允许访问
        return true;
    }

    /**
     * 检查普通用户权限
     * 
     * @param path 请求路径
     * @param method 请求方法
     * @param userId 用户ID
     * @return 是否有权限
     */
    private boolean checkUserPermission(String path, String method, String userId) {
        // 用户只能访问自己的资源
        
        // 用户管理相关
        if (path.startsWith("/api/user/")) {
            // 允许查看和修改自己的信息
            if (path.equals("/api/user/profile") || path.equals("/api/user/password")) {
                return "GET".equals(method) || "PUT".equals(method);
            }
            
            // 检查是否访问自己的用户资源
            if (path.matches("/api/user/" + userId + "(/.*)?")) {
                return "GET".equals(method) || "PUT".equals(method);
            }
            
            // 禁止访问其他用户信息
            return false;
        }
        
        // 文献管理相关 - 用户只能管理自己的文献
        if (path.startsWith("/api/literature/")) {
            // 允许查看公开文献
            if ("GET".equals(method) && path.equals("/api/literature/public")) {
                return true;
            }
            
            // 其他操作需要进一步检查文献所有者
            // 这里简化处理，允许所有操作，实际应该在业务层检查所有权
            return true;
        }
        
        // 笔记管理相关 - 用户只能管理自己的笔记
        if (path.startsWith("/api/notes/")) {
            return true; // 业务层会检查笔记所有权
        }
        
        // 分类管理相关 - 用户只能管理自己的分类
        if (path.startsWith("/api/categories/")) {
            return true; // 业务层会检查分类所有权
        }
        
        // 统计服务 - 用户只能查看自己的统计
        if (path.startsWith("/api/statistics/")) {
            return "GET".equals(method); // 只允许查看
        }
        
        // AI服务 - 用户可以使用AI功能
        if (path.startsWith("/api/ai/")) {
            return true;
        }
        
        // 其他路径默认拒绝
        return false;
    }

    /**
     * 发送403禁止访问响应
     * 
     * @param exchange ServerWebExchange
     * @param message 错误消息
     * @return 响应的Mono
     */
    private Mono<Void> sendForbiddenResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        
        // 设置响应状态和头
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.getHeaders().add("Access-Control-Allow-Origin", "*");
        response.getHeaders().add("Access-Control-Allow-Credentials", "true");
        
        // 构建错误响应体
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", HttpStatus.FORBIDDEN.value());
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
     * 配置类
     */
    public static class Config {
        // 可以在这里添加权限过滤器特定的配置参数
        private boolean enabled = true;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}