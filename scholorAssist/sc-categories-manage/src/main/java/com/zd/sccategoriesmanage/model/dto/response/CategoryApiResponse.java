package com.zd.sccategoriesmanage.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分类API响应DTO - 严格遵循API文档字段命名规范
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分类信息响应")
public class CategoryApiResponse {

    /**
     * 分类唯一标识
     */
    @JsonProperty("id")
    @Schema(description = "分类唯一标识", example = "cat_1")
    private String id;

    /**
     * 分类名称
     */
    @JsonProperty("name")
    @Schema(description = "分类名称", example = "机器学习")
    private String name;

    /**
     * 父分类ID，null表示根分类
     */
    @JsonProperty("parent_id")
    @Schema(description = "父分类ID，null表示根分类")
    private String parentId;

    /**
     * 创建时间（ISO格式）
     */
    @JsonProperty("created_at")
    @Schema(description = "创建时间（ISO格式）", example = "2024-01-15T10:30:00Z")
    private String createdAt;

    /**
     * 该分类下的文档数量
     */
    @JsonProperty("document_count")
    @Schema(description = "该分类下的文档数量", example = "15")
    private Integer documentCount;
}