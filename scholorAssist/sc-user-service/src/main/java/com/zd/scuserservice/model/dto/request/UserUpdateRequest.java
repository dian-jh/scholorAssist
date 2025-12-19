package com.zd.scuserservice.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 用户更新请求DTO
 * 
 * @author system
 * @since 2024-01-21
 */
@Data
@Schema(description = "用户更新请求")
public class UserUpdateRequest {

    /**
     * 邮箱地址
     */
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱地址", example = "john@example.com")
    private String email;

    /**
     * 真实姓名
     */
    @Size(max = 50, message = "真实姓名不能超过50个字符")
    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    /**
     * 头像URL
     */
    @Size(max = 500, message = "头像URL不能超过500个字符")
    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatarUrl;
}