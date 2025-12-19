package com.zd.sccommon.utils;

import com.zd.sccommon.config.SwaggerJwtConfig;
import com.zd.sccommon.model.UserContext;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Swagger JWT工具类
 * 专为Swagger测试环境设计的JWT验证工具
 * 与网关JWT解析逻辑保持一致
 * 
 * @author System
 * @since 2024-01-21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SwaggerJwtUtil {

    private final SwaggerJwtConfig swaggerJwtConfig;

    /**
     * 从Authorization头中提取token
     * 
     * @param authorization Authorization头值
     * @return JWT token
     * @throws IllegalArgumentException 如果token格式不正确
     */
    public String extractTokenFromHeader(String authorization) {
        if (authorization == null || authorization.trim().isEmpty()) {
            throw new IllegalArgumentException("Authorization头不能为空");
        }

        if (!authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization头必须以'Bearer '开头");
        }

        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            throw new IllegalArgumentException("JWT token不能为空");
        }

        return token;
    }

    /**
     * 验证JWT token并提取用户信息
     * 
     * @param token JWT token
     * @return 用户上下文信息
     * @throws JwtException 如果token无效
     */
    public UserContext validateAndExtractUserInfo(String token) {
        try {
            // 验证token
            Claims claims = extractAllClaims(token);
            
            // 检查token是否过期
            if (isTokenExpired(claims)) {
                throw new ExpiredJwtException(null, claims, "Token已过期");
            }
            
            // 检查必要的声明是否存在
            if (!hasRequiredClaims(claims)) {
                throw new MalformedJwtException("Token缺少必要的声明");
            }
            
            // 提取用户信息
            String userId = claims.get("userId", String.class);
            String username = claims.get("username", String.class);
            String name = claims.get("name", String.class);
            String role = claims.get("role", String.class);
            String avatar = claims.get("avatar", String.class);
            String email = claims.get("email", String.class);
            String status = claims.get("status", String.class);
            
            if (swaggerJwtConfig.getDebug()) {
                log.debug("Swagger Token验证成功，用户: {}, 角色: {}, 状态: {}", username, role, status);
            }
            
            return UserContext.builder()
                    .userId(userId)
                    .username(username)
                    .name(name)
                    .role(role)
                    .avatar(avatar)
                    .email(email)
                    .status(status)
                    .token(token)
                    .build();
                    
        } catch (ExpiredJwtException e) {
            log.warn("Swagger Token已过期: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.error("不支持的JWT格式: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.error("JWT格式错误: {}", e.getMessage());
            throw e;
        } catch (SecurityException e) {
            log.error("JWT签名验证失败: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("JWT参数错误: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Token验证异常: {}", e.getMessage(), e);
            throw new JwtException("Token验证失败", e);
        }
    }

    /**
     * 检查token是否过期
     * 
     * @param claims JWT声明
     * @return 是否过期
     */
    private boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration != null && expiration.before(new Date());
    }

    /**
     * 检查是否包含必要的声明
     * 
     * @param claims JWT声明
     * @return 是否包含必要声明
     */
    private boolean hasRequiredClaims(Claims claims) {
        return claims.get("userId") != null && 
               claims.get("username") != null && 
               claims.get("role") != null;
    }

    /**
     * 提取token中的所有声明
     * 
     * @param token JWT token
     * @return Claims对象
     * @throws JwtException 如果token无效
     */
    private Claims extractAllClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(swaggerJwtConfig.getSecret().getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 验证用户权限
     * 
     * @param userContext 用户上下文
     * @param requiredRole 需要的角色
     * @return 是否有权限
     */
    public boolean hasPermission(UserContext userContext, String requiredRole) {
        if (userContext == null || requiredRole == null) {
            return false;
        }
        
        String userRole = userContext.getRole();
        
        // 超级管理员拥有所有权限
        if ("super_admin".equals(userRole)) {
            return true;
        }
        
        // 管理员拥有用户权限
        if ("admin".equals(userRole) && "user".equals(requiredRole)) {
            return true;
        }
        
        // 角色完全匹配
        return userRole.equals(requiredRole);
    }
}