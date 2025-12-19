package com.zd.scuserservice.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import jakarta.validation.constraints.*;

/**
 * 用户注册请求DTO
 * 
 * @author system
 * @since 2024-01-21
 */
@Data
public class UserRegisterRequest {

    /**
     * 用户名，3-20字符，只能包含字母、数字、下划线
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字、下划线")
    private String username;

    /**
     * 邮箱地址，必须是有效的邮箱格式
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100字符")
    private String email;

    /**
     * 密码，8-32字符，必须包含大小写字母、数字和特殊字符
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度必须在8-32字符之间")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$", 
             message = "密码必须包含大小写字母、数字和特殊字符")
    private String password;

    /**
     * 确认密码，必须与password一致
     */
    @NotBlank(message = "确认密码不能为空")
    @JsonProperty("confirm_password")
    private String confirmPassword;

    /**
     * 真实姓名，1-50字符
     */
    @Size(max = 50, message = "真实姓名长度不能超过50字符")
    @JsonProperty("real_name")
    private String realName;
}