package com.zd.sccategoriesmanage.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 分类响应DTO
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分类信息响应")
public class CategoryResponse {

    /**
     * 分类ID
     */
    @Schema(description = "分类ID", example = "cat_123456789")
    private String categoryId;

    /**
     * 父分类ID
     */
    @Schema(description = "父分类ID", example = "cat_987654321")
    private String parentId;

    /**
     * 分类名称
     */
    @Schema(description = "分类名称", example = "学术论文")
    private String name;

    /**
     * 分类描述
     */
    @Schema(description = "分类描述", example = "存放学术论文相关文档")
    private String description;

    /**
     * 排序顺序
     */
    @Schema(description = "排序顺序", example = "1")
    private Integer sortOrder;

    /**
     * 文档数量
     */
    @Schema(description = "该分类下的文档数量", example = "5")
    private Integer documentCount;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2024-01-01T10:00:00")
    private LocalDateTime updatedAt;
}