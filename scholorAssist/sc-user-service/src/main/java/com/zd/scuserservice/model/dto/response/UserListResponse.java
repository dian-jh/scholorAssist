package com.zd.scuserservice.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户列表响应DTO
 * 
 * @author system
 * @since 2024-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户列表响应")
public class UserListResponse {

    /**
     * 用户列表
     */
    @Schema(description = "用户列表")
    private List<UserInfoResponse> users;

    /**
     * 分页信息
     */
    @Schema(description = "分页信息")
    private PageInfo pageInfo;

    /**
     * 分页信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "分页信息")
    public static class PageInfo {

        /**
         * 当前页码
         */
        @Schema(description = "当前页码", example = "1")
        private Integer currentPage;

        /**
         * 每页数量
         */
        @Schema(description = "每页数量", example = "20")
        private Integer pageSize;

        /**
         * 总记录数
         */
        @Schema(description = "总记录数", example = "100")
        private Long totalCount;

        /**
         * 总页数
         */
        @Schema(description = "总页数", example = "5")
        private Integer totalPages;

        /**
         * 是否有下一页
         */
        @Schema(description = "是否有下一页", example = "true")
        private Boolean hasNext;

        /**
         * 是否有上一页
         */
        @Schema(description = "是否有上一页", example = "false")
        private Boolean hasPrevious;
    }
}