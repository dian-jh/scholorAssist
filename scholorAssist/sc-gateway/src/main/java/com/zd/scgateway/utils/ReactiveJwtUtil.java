package com.zd.scgateway.utils;

import com.zd.scgateway.config.JwtConfig;
import com.zd.scgateway.exception.JwtAuthenticationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * 响应式JWT工具类
 * 专为Spring Cloud Gateway设计的JWT验证工具
 * 
 * @author system
 * @since 2024-01-21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReactiveJwtUtil {

    private final JwtConfig jwtConfig;

    /**
     * 用户信息记录类
     */
    public record UserInfo(
        String userId,
        String username,
        String name,
        String role,
        String avatar,
        String email,
        String status,
        String token
    ) {}

    /**
     * 验证JWT token并提取用户信息
     * 
     * @param token JWT token
     * @return 用户信息的Mono
     */
    public Mono<UserInfo> validateAndExtractUserInfo(String token) {
        return Mono.fromCallable(() -> {
            try {
                // 验证token
                Claims claims = extractAllClaims(token);
                
                // 检查token是否过期
                if (isTokenExpired(claims)) {
                    throw JwtAuthenticationException.expiredToken();
                }
                
                // 检查必要的声明是否存在
                if (!hasRequiredClaims(claims)) {
                    throw JwtAuthenticationException.incompleteToken();
                }
                
                // 提取用户信息
                String userId = claims.get("userId", String.class);
                String username = claims.get("username", String.class);
                String name = claims.get("name", String.class);
                String role = claims.get("role", String.class);
                String avatar = claims.get("avatar", String.class);
                String email = claims.get("email", String.class);
                String status = claims.get("status", String.class);
                
                if (jwtConfig.getDebug()) {
                    log.debug("Token验证成功，用户: {}, 角色: {}, 状态: {}", username, role, status);
                }
                
                return new UserInfo(userId, username, name, role, avatar, email, status, token);
                
            } catch (ExpiredJwtException e) {
                log.warn("Token已过期: {}", e.getMessage());
                throw JwtAuthenticationException.expiredToken();
            } catch (UnsupportedJwtException e) {
                log.error("不支持的JWT格式: {}", e.getMessage());
                throw JwtAuthenticationException.invalidToken();
            } catch (MalformedJwtException e) {
                log.error("JWT格式错误: {}", e.getMessage());
                throw JwtAuthenticationException.invalidToken();
            } catch (SecurityException e) {
                log.error("JWT签名验证失败: {}", e.getMessage());
                throw JwtAuthenticationException.invalidToken();
            } catch (IllegalArgumentException e) {
                log.error("JWT参数错误: {}", e.getMessage());
                throw JwtAuthenticationException.invalidToken();
            } catch (JwtAuthenticationException e) {
                throw e;
            } catch (Exception e) {
                log.error("Token验证异常: {}", e.getMessage(), e);
                throw JwtAuthenticationException.authenticationError("Token验证失败", e);
            }
        })
        .onErrorMap(throwable -> {
            if (throwable instanceof JwtAuthenticationException) {
                return throwable;
            }
            return JwtAuthenticationException.authenticationError("Token验证异常", throwable);
        });
    }

    /**
     * 从Authorization头中提取token
     * 
     * @param authorization Authorization头值
     * @return JWT token的Mono
     */
    public Mono<String> extractTokenFromHeader(String authorization) {
        return Mono.fromCallable(() -> {
            if (authorization == null || authorization.trim().isEmpty()) {
                throw JwtAuthenticationException.missingToken();
            }
            
            if (!authorization.startsWith("Bearer ")) {
                log.warn("Authorization头格式不正确: {}", authorization);
                throw JwtAuthenticationException.invalidTokenFormat();
            }
            
            String token = authorization.substring(7).trim();
            if (token.isEmpty()) {
                throw JwtAuthenticationException.invalidTokenFormat();
            }
            
            if (jwtConfig.getDebug()) {
                log.debug("从Authorization头中提取token成功");
            }
            return token;
        });
    }

    /**
     * 验证用户权限
     * 
     * @param userInfo 用户信息
     * @param requiredRole 需要的角色
     * @return 是否有权限的Mono
     */
    public Mono<Boolean> hasPermission(UserInfo userInfo, String requiredRole) {
        return Mono.fromCallable(() -> {
            if (userInfo == null || requiredRole == null) {
                return false;
            }
            
            String userRole = userInfo.role();
            
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
        });
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
        return claims.get("userId") != null 
            && claims.get("username") != null 
            && claims.get("role") != null;
    }

    /**
     * 提取token中的所有声明
     * 
     * @param token JWT token
     * @return Claims对象
     * @throws JwtException 如果token无效
     */
    private Claims extractAllClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}