package com.zd.scuserservice.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 
 * <p>对应数据库表：users</p>
 * <p>存储系统用户的基本信息、认证数据和状态信息</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("users")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户唯一标识，格式：user_xxxxxxxxx
     */
    @TableField("user_id")
    private String userId;

    /**
     * 用户名，3-20字符
     */
    @TableField("username")
    private String username;

    /**
     * 邮箱地址
     */
    @TableField("email")
    private String email;

    /**
     * 密码哈希值
     */
    @TableField("password_hash")
    private String passwordHash;

    /**
     * 真实姓名
     */
    @TableField("real_name")
    private String realName;

    /**
     * 用户角色：user/admin/super_admin
     */
    @TableField("role")
    private String role;

    /**
     * 用户状态：pending_verification/active/suspended
     */
    @TableField("status")
    private String status;

    /**
     * 头像URL
     */
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * 最后登录时间
     */
    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}