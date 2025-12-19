package com.zd.sccommon.interceptor;

import com.zd.sccommon.config.SwaggerJwtConfig;
import com.zd.sccommon.model.UserContext;
import com.zd.sccommon.utils.SwaggerJwtUtil;
import com.zd.sccommon.utils.UserContextHolder;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Swagger Token拦截器
 * 专为Swagger3测试环境设计，支持在不经过网关的情况下直接解析token
 * 
 * <p>主要功能：</p>
 * <ul>
 *   <li>识别Swagger UI发起的请求</li>
 *   <li>从请求头中提取并验证JWT token</li>
 *   <li>将解析后的用户信息注入到请求上下文中</li>
 *   <li>与网关token解析逻辑保持一致</li>
 * </ul>
 * 
 * @author System
 * @since 2024-01-21
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SwaggerTokenInterceptor implements HandlerInterceptor {

    private final SwaggerJwtUtil swaggerJwtUtil;
    private final SwaggerJwtConfig swaggerJwtConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            String path = request.getRequestURI();
            String method = request.getMethod();

            if (swaggerJwtConfig.getDebug()) {
                log.debug("SwaggerTokenInterceptor处理请求: {} {}", method, path);
            }

            // 检查是否启用Swagger JWT验证
            if (!swaggerJwtConfig.getEnabled()) {
                if (swaggerJwtConfig.getDebug()) {
                    log.debug("Swagger JWT验证已禁用，跳过处理");
                }
                return true;
            }

            // 检查是否为Swagger相关路径（直接放行）
            if (isSwaggerPath(path)) {
                if (swaggerJwtConfig.getDebug()) {
                    log.debug("Swagger相关路径，直接放行: {}", path);
                }
                return true;
            }

            // 检查是否为不需要认证的路径
            if (isExcludedPath(path)) {
                if (swaggerJwtConfig.getDebug()) {
                    log.debug("排除路径，无需认证: {}", path);
                }
                return true;
            }

            // 提取并验证JWT token
            String authorization = request.getHeader("Authorization");
            if (authorization == null || authorization.trim().isEmpty()) {
                if (swaggerJwtConfig.getDebug()) {
                    log.debug("未找到Authorization头，跳过token验证");
                }
                return true;
            }

            try {
                // 提取token
                String token = swaggerJwtUtil.extractTokenFromHeader(authorization);
                
                // 验证token并提取用户信息
                UserContext userContext = swaggerJwtUtil.validateAndExtractUserInfo(token);
                
                // 将用户信息存储到ThreadLocal
                UserContextHolder.setContext(userContext);
                
                if (swaggerJwtConfig.getDebug()) {
                    log.debug("Swagger Token验证成功，用户: {}, 角色: {}", 
                            userContext.getUsername(), userContext.getRole());
                }
                
            } catch (JwtException e) {
                // JWT验证失败，记录警告但不阻止请求（Swagger测试环境）
                log.warn("Swagger Token验证失败: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                if (swaggerJwtConfig.getDebug()) {
                    log.debug("Token验证失败详情", e);
                }
                // 清空上下文
                UserContextHolder.clear();
            } catch (Exception e) {
                // 其他异常，记录错误但不阻止请求
                log.error("Swagger Token处理异常: {}", e.getMessage(), e);
                UserContextHolder.clear();
            }

            return true;
            
        } catch (Exception e) {
            log.error("SwaggerTokenInterceptor处理异常: {}", e.getMessage(), e);
            // 确保清空上下文
            UserContextHolder.clear();
            return true; // 不阻止请求继续
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            // 请求完成后清理ThreadLocal，防止内存泄漏
            UserContextHolder.clear();
            if (swaggerJwtConfig.getDebug()) {
                log.debug("SwaggerTokenInterceptor清理用户上下文");
            }
        } catch (Exception e) {
            log.error("清理用户上下文时发生异常", e);
        }
    }

    /**
     * 检查是否为Swagger相关路径
     * 
     * @param requestPath 请求路径
     * @return 是否为Swagger路径
     */
    private boolean isSwaggerPath(String requestPath) {
        return swaggerJwtConfig.getSwaggerPaths().stream()
                .anyMatch(swaggerPath -> requestPath.contains(swaggerPath));
    }

    /**
     * 检查路径是否不需要认证
     * 
     * @param requestPath 请求路径
     * @return 是否不需要认证
     */
    private boolean isExcludedPath(String requestPath) {
        return swaggerJwtConfig.getExcludedPaths().stream()
                .anyMatch(excludedPath -> requestPath.startsWith(excludedPath));
    }
}
