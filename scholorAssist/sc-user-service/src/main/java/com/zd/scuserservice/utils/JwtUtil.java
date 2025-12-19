package com.zd.scuserservice.utils;

// 1. 修改了这里的导入
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 提供JWT token的生成、解析、验证等功能
 * * @author system
 * @since 2024-01-21
 */
@Slf4j
@Component
public class JwtUtil {

    /**
     * JWT密钥
     */
    @Value("${jwt.secret:mySecretKey123456789012345678901234567890}")
    private String secret;

    /**
     * JWT过期时间（小时）
     */
    @Value("${jwt.expiration:24}")
    private int expiration;

    /**
     * 记住我功能的过期时间（小时）
     */
    @Value("${jwt.remember-me-expiration:168}")
    private int rememberMeExpiration;

    /**
     * 生成JWT token
     * * @param userId 用户ID
     * @param username 用户名
     * @param role 用户角色
     * @param rememberMe 是否记住登录
     * @return JWT token
     */
    public String generateToken(String userId, String username, String role, boolean rememberMe) {
        log.debug("生成JWT token，userId: {}, username: {}, role: {}, rememberMe: {}",
                userId, username, role, rememberMe);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);

        int tokenExpiration = rememberMe ? rememberMeExpiration : expiration;
        Date expirationDate = Date.from(
                LocalDateTime.now().plusHours(tokenExpiration)
                        .atZone(ZoneId.systemDefault()).toInstant()
        );

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(expirationDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        log.debug("JWT token生成成功，过期时间: {}", expirationDate);
        return token;
    }

    /**
     * 从token中提取用户ID
     * * @param token JWT token
     * @return 用户ID
     */
    public String extractUserId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            // 3. 修改了这里
            String userId = (String) claims.get("userId");
            log.debug("从token中提取用户ID: {}", userId);
            return userId;
        } catch (Exception e) {
            log.error("提取用户ID失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从token中提取用户名
     * * @param token JWT token
     * @return 用户名
     */
    public String extractUsername(String token) {
        try {
            Claims claims = extractAllClaims(token);
            // 3. 修改了这里
            String username = (String) claims.get("username");
            log.debug("从token中提取用户名: {}", username);
            return username;
        } catch (Exception e) {
            log.error("提取用户名失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从token中提取用户角色
     * * @param token JWT token
     * @return 用户角色
     */
    public String extractRole(String token) {
        try {
            Claims claims = extractAllClaims(token);
            // 3. 修改了这里
            String role = (String) claims.get("role");
            log.debug("从token中提取用户角色: {}", role);
            return role;
        } catch (Exception e) {
            log.error("提取用户角色失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从token中提取过期时间
     * * @param token JWT token
     * @return 过期时间
     */
    public Date extractExpiration(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration(); // 这个方法是正确的
        } catch (Exception e) {
            log.error("提取过期时间失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证token是否有效
     * * @param token JWT token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            // 检查是否能解析，并且是否过期
            boolean isExpired = isTokenExpired(token);
            if (isExpired) {
                log.warn("Token验证失败: 已过期");
                return false;
            }
            log.debug("Token验证结果: 有效");
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.error("Token验证失败 (无效签名或格式错误): {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Token验证异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查token是否过期
     * * @param token JWT token
     * @return 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            if (expiration == null) {
                log.warn("无法从Token中提取过期时间");
                return true; // 无法提取视为无效/过期
            }
            boolean expired = expiration.before(new Date());
            log.debug("Token过期检查结果: {}", expired);
            return expired;
        } catch (Exception e) {
            // extractExpiration 内部已经记录了错误
            log.error("检查token过期状态时发生异常: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 从Authorization头中提取token
     * * @param authorization Authorization头值
     * @return JWT token，如果格式不正确返回null
     */
    public String extractTokenFromHeader(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            log.debug("从Authorization头中提取token成功");
            return token;
        }
        log.warn("Authorization头格式不正确: {}", authorization);
        return null;
    }

    /**
     * 提取token中的所有声明
     * * @param token JWT token
     * @return Claims对象
     */
    private Claims extractAllClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        // 2. 移除了不必要的 (Claims) 强转
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 刷新token（生成新的token）
     * * @param oldToken 旧token
     * @return 新token，如果旧token无效返回null
     */
    public String refreshToken(String oldToken) {
        try {
            // 注意：validateToken 会检查是否过期，
            // 通常刷新逻辑允许已过期的token（在一定宽限期内）
            // 这里的实现是：如果token格式正确但已过期，validateToken返回false，导致刷新失败。
            // 您可能需要调整 validateToken 的逻辑，或者在刷新时单独处理 ExpiredJwtException

            // 简单的实现：我们只解析，不验证过期
            Claims claims = extractAllClaims(oldToken);

            String userId = (String) claims.get("userId");
            String username = (String) claims.get("username");
            String role = (String) claims.get("role");

            if (userId == null || username == null || role == null) {
                log.error("从旧token中提取信息失败，无法刷新");
                return null;
            }

            // 刷新时，通常不使用 "rememberMe"
            String newToken = generateToken(userId, username, role, false);
            log.info("Token刷新成功，userId: {}", userId);
            return newToken;
        } catch (ExpiredJwtException e) {
            // 如果token只是过期了，我们允许刷新
            log.warn("Token已过期，尝试刷新: {}", e.getMessage());
            String userId = (String) e.getClaims().get("userId");
            String username = (String) e.getClaims().get("username");
            String role = (String) e.getClaims().get("role");

            if (userId == null || username == null || role == null) {
                log.error("从过期的token中提取信息失败，无法刷新");
                return null;
            }
            return generateToken(userId, username, role, false);

        } catch (Exception e) {
            log.error("刷新token失败（Token无效或已损坏）: {}", e.getMessage());
            return null;
        }
    }
}