package com.zd.scuserservice.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改密码请求DTO
 * 
 * @author system
 * @since 2024-01-21
 */
@Data
@Schema(description = "修改密码请求")
public class ChangePasswordRequest {

    /**
     * 当前密码
     */
    @NotBlank(message = "当前密码不能为空")
    @Schema(description = "当前密码", example = "oldPassword123")
    private String currentPassword;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 50, message = "新密码长度必须在8-50个字符之间")
    @Schema(description = "新密码", example = "newPassword123")
    private String newPassword;

    /**
     * 确认新密码
     */
    @NotBlank(message = "确认密码不能为空")
    @Schema(description = "确认新密码", example = "newPassword123")
    private String confirmPassword;
}