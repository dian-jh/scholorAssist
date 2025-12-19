package com.zd.scuserservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求DTO
 * 
 * @author system
 * @since 2024-01-21
 */
@Data
public class UserLoginRequest {

    /**
     * 用户名或邮箱地址
     */
    @NotBlank(message = "用户名或邮箱不能为空")
    private String login;

    /**
     * 用户密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 是否记住登录状态，默认false
     */
    private Boolean rememberMe = false;
}