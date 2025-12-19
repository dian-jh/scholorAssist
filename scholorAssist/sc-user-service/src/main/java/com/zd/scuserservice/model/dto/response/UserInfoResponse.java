package com.zd.scuserservice.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户信息响应DTO
 * 
 * @author system
 * @since 2024-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户信息响应")
public class UserInfoResponse {

    /**
     * 用户唯一标识
     */
    @Schema(description = "用户ID", example = "user_123456789")
    private String userId;

    /**
     * 用户名
     */
    @Schema(description = "用户名", example = "john_doe")
    private String username;

    /**
     * 邮箱地址
     */
    @Schema(description = "邮箱地址", example = "john@example.com")
    private String email;

    /**
     * 真实姓名
     */
    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    /**
     * 用户角色
     */
    @Schema(description = "用户角色", example = "user", allowableValues = {"user", "admin", "super_admin"})
    private String role;

    /**
     * 用户状态
     */
    @Schema(description = "用户状态", example = "active", allowableValues = {"pending_verification", "active", "suspended"})
    private String status;

    /**
     * 头像URL
     */
    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatarUrl;

    /**
     * 最后登录时间
     */
    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginAt;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}