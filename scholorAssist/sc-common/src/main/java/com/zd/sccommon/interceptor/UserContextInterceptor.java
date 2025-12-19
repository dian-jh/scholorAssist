package com.zd.sccommon.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zd.sccommon.model.UserContext;
import com.zd.sccommon.utils.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;



/**
 * 用户上下文拦截器
 * 
 * <p>从请求头中提取用户信息并存储到ThreadLocal中</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class UserContextInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    /**
     * 用户信息请求头名称（新格式：JSON字符串）
     */
    private static final String HEADER_USER_INFO = "USER_INFO";

    /**
     * 用户ID请求头名称（兼容旧格式）
     */
    private static final String HEADER_USER_ID = "X-User-Id";

    /**
     * 用户名请求头名称（兼容旧格式）
     */
    private static final String HEADER_USERNAME = "X-Username";

    /**
     * 用户角色请求头名称（兼容旧格式）
     */
    private static final String HEADER_USER_ROLE = "X-User-Role";

    /**
     * JWT Token请求头名称
     */
    private static final String HEADER_AUTHORIZATION = "Authorization";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            UserContext userContext = null;

            // 优先尝试从新格式的USER_INFO请求头中解析用户信息
            String userInfoJson = request.getHeader(HEADER_USER_INFO);
            if (userInfoJson != null && !userInfoJson.trim().isEmpty()) {
                try {
                    userContext = objectMapper.readValue(userInfoJson, UserContext.class);
                    log.debug("从USER_INFO请求头解析用户上下文成功，userId: {}, username: {}, role: {}", 
                            userContext.getUserId(), userContext.getUsername(), userContext.getRole());
                } catch (Exception e) {
                    log.warn("解析USER_INFO请求头失败，尝试使用旧格式: {}", e.getMessage());
                }
            }

            // 如果新格式解析失败，回退到旧格式（兼容性处理）
            if (userContext == null) {
                String userId = request.getHeader(HEADER_USER_ID);
                String username = request.getHeader(HEADER_USERNAME);
                String role = request.getHeader(HEADER_USER_ROLE);
                String authorization = request.getHeader(HEADER_AUTHORIZATION);

                // 提取JWT token（去掉Bearer前缀）
                String token = null;
                if (authorization != null && authorization.startsWith("Bearer ")) {
                    token = authorization.substring(7);
                }

                // 如果有用户信息，则创建用户上下文（旧格式）
                if (userId != null && !userId.trim().isEmpty()) {
                    userContext = UserContext.builder()
                            .userId(userId)
                            .username(username)
                            .role(role)
                            .token(token)
                            .build();

                    log.debug("从旧格式请求头创建用户上下文，userId: {}, username: {}, role: {}", userId, username, role);
                }
            }

            // 如果有用户上下文，则存储到ThreadLocal
            if (userContext != null) {
                UserContextHolder.setContext(userContext);
                log.debug("用户上下文已设置，userId: {}, username: {}, role: {}, status: {}", 
                        userContext.getUserId(), userContext.getUsername(), userContext.getRole(), userContext.getStatus());
            } else {
                log.debug("请求头中未找到用户信息，跳过用户上下文设置");
            }

            return true;
        } catch (Exception e) {
            log.error("设置用户上下文时发生异常", e);
            // 即使出现异常也继续处理请求，但清空上下文
            UserContextHolder.clear();
            return true;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            // 请求完成后清理ThreadLocal，防止内存泄漏
            UserContextHolder.clear();
            log.debug("用户上下文已清理");
        } catch (Exception e) {
            log.error("清理用户上下文时发生异常", e);
        }
    }
}