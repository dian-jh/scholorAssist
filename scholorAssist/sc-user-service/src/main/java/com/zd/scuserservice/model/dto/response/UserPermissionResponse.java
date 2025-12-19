package com.zd.scuserservice.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户权限响应DTO
 * 
 * @author system
 * @since 2024-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户权限响应")
public class UserPermissionResponse {

    /**
     * 用户ID
     */
    @Schema(description = "用户ID", example = "user_123456789")
    private String userId;

    /**
     * 用户角色
     */
    @Schema(description = "用户角色", example = "user", allowableValues = {"user", "admin", "super_admin"})
    private String role;

    /**
     * 权限列表
     */
    @Schema(description = "权限列表")
    private List<String> permissions;

    /**
     * 使用限制
     */
    @Schema(description = "使用限制")
    private UsageLimits usageLimits;

    /**
     * 使用限制内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "使用限制")
    public static class UsageLimits {

        /**
         * 最大文档数量
         */
        @Schema(description = "最大文档数量", example = "100")
        private Integer maxDocuments;

        /**
         * 最大存储空间（MB）
         */
        @Schema(description = "最大存储空间（MB）", example = "1024")
        private Long maxStorageSize;

        /**
         * 每日AI对话次数限制
         */
        @Schema(description = "每日AI对话次数限制", example = "50")
        private Integer dailyAiConversations;

        /**
         * 是否允许导出功能
         */
        @Schema(description = "是否允许导出功能", example = "true")
        private Boolean allowExport;

        /**
         * 是否允许分享功能
         */
        @Schema(description = "是否允许分享功能", example = "true")
        private Boolean allowShare;
    }
}