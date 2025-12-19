package com.zd.scuserservice.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户会话实体类
 * 
 * <p>对应数据库表：user_sessions</p>
 * <p>存储用户登录会话信息，用于token管理和安全控制</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "user_sessions", autoResultMap = true)
public class UserSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 会话唯一标识
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * 所属用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * JWT Token哈希值
     */
    @TableField("token_hash")
    private String tokenHash;

    /**
     * 设备信息
     */
    @TableField(value = "device_info", typeHandler = JacksonTypeHandler.class)
    private Object deviceInfo;

    /**
     * IP地址
     */
    @TableField("ip_address")
    private String ipAddress;

    /**
     * 用户代理字符串
     */
    @TableField("user_agent")
    private String userAgent;

    /**
     * 过期时间
     */
    @TableField("expires_at")
    private LocalDateTime expiresAt;

    /**
     * 是否活跃
     */
    @TableField("is_active")
    private Boolean isActive;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 最后访问时间
     */
    @TableField(value = "last_accessed_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastAccessedAt;
}