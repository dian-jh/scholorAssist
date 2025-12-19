package com.zd.scgateway.exception;

/**
 * JWT认证异常
 * 用于JWT认证过程中的异常处理
 * 
 * @author system
 * @since 2024-01-21
 */
public class JwtAuthenticationException extends RuntimeException {

    private final int code;

    public JwtAuthenticationException(int code, String message) {
        super(message);
        this.code = code;
    }

    public JwtAuthenticationException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * Token缺失异常
     */
    public static JwtAuthenticationException missingToken() {
        return new JwtAuthenticationException(401, "缺少Authorization头");
    }

    /**
     * Token格式错误异常
     */
    public static JwtAuthenticationException invalidTokenFormat() {
        return new JwtAuthenticationException(401, "Authorization头格式不正确");
    }

    /**
     * Token无效异常
     */
    public static JwtAuthenticationException invalidToken() {
        return new JwtAuthenticationException(401, "Token无效或已过期");
    }

    /**
     * Token过期异常
     */
    public static JwtAuthenticationException expiredToken() {
        return new JwtAuthenticationException(401, "Token已过期");
    }

    /**
     * Token信息不完整异常
     */
    public static JwtAuthenticationException incompleteToken() {
        return new JwtAuthenticationException(401, "Token信息不完整");
    }

    /**
     * 权限不足异常
     */
    public static JwtAuthenticationException insufficientPermissions() {
        return new JwtAuthenticationException(403, "权限不足");
    }

    /**
     * 认证异常
     */
    public static JwtAuthenticationException authenticationError(String message) {
        return new JwtAuthenticationException(401, message);
    }

    /**
     * 认证异常（带原因）
     */
    public static JwtAuthenticationException authenticationError(String message, Throwable cause) {
        return new JwtAuthenticationException(401, message, cause);
    }
}