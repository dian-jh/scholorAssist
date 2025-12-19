package com.zd.scuserservice.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 用户权限更新请求DTO
 * 
 * @author system
 * @since 2024-01-21
 */
@Data
@Schema(description = "用户权限更新请求")
public class UserPermissionUpdateRequest {

    /**
     * 用户角色
     */
    @NotBlank(message = "用户角色不能为空")
    @Pattern(regexp = "^(user|admin|super_admin)$", message = "用户角色必须是user、admin或super_admin")
    @Schema(description = "用户角色", example = "admin", allowableValues = {"user", "admin", "super_admin"})
    private String role;

    /**
     * 用户状态
     */
    @NotBlank(message = "用户状态不能为空")
    @Pattern(regexp = "^(pending_verification|active|suspended)$", message = "用户状态必须是pending_verification、active或suspended")
    @Schema(description = "用户状态", example = "active", allowableValues = {"pending_verification", "active", "suspended"})
    private String status;

    /**
     * 操作原因
     */
    @Schema(description = "操作原因", example = "用户申请升级为管理员")
    private String reason;
}