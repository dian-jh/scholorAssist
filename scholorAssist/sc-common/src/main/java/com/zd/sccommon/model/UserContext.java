package com.zd.sccommon.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户上下文信息模型
 * 
 * <p>用于在微服务间传递用户身份信息</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户唯一标识
     */
    @JsonProperty("userId")
    private String userId;

    /**
     * 用户名（登录名）
     */
    @JsonProperty("username")
    private String username;

    /**
     * 用户显示名称（真实姓名或昵称）
     */
    @JsonProperty("name")
    private String name;

    /**
     * 用户角色
     */
    @JsonProperty("role")
    private String role;

    /**
     * 用户头像URL
     */
    @JsonProperty("avatar")
    private String avatar;

    /**
     * 用户邮箱
     */
    @JsonProperty("email")
    private String email;

    /**
     * 用户状态（active/suspended/pending_verification）
     */
    @JsonProperty("status")
    private String status;

    /**
     * JWT Token
     */
    @JsonProperty("token")
    private String token;

    /**
     * 检查用户是否已登录
     * 
     * @return 是否已登录
     */
    public boolean isLoggedIn() {
        return userId != null && !userId.trim().isEmpty();
    }

    /**
     * 检查用户是否具有指定角色
     * 
     * @param targetRole 目标角色
     * @return 是否具有指定角色
     */
    public boolean hasRole(String targetRole) {
        if (role == null || targetRole == null) {
            return false;
        }

        // 超级管理员拥有所有角色权限
        if ("super_admin".equals(role)) {
            return true;
        }

        // 管理员拥有用户角色权限
        if ("admin".equals(role) && "user".equals(targetRole)) {
            return true;
        }

        return role.equals(targetRole);
    }

    /**
     * 检查用户是否为管理员
     * 
     * @return 是否为管理员
     */
    public boolean isAdmin() {
        return hasRole("admin") || hasRole("super_admin");
    }

    /**
     * 检查用户是否为超级管理员
     * 
     * @return 是否为超级管理员
     */
    public boolean isSuperAdmin() {
        return hasRole("super_admin");
    }

    /**
     * 获取用户显示名称
     * 优先返回name字段，如果为空则返回username
     * 
     * @return 用户显示名称
     */
    public String getDisplayName() {
        return (name != null && !name.trim().isEmpty()) ? name : username;
    }

    /**
     * 检查用户状态是否为活跃状态
     * 
     * @return 是否为活跃状态
     */
    public boolean isActive() {
        return "active".equals(status);
    }
}