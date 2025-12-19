package com.zd.sccategoriesmanage.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新分类请求DTO
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "更新分类请求")
public class CategoryUpdateRequest {

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称长度不能超过50个字符")
    @Schema(description = "分类名称", example = "人工智能", required = true)
    private String name;

    /**
     * 父分类ID，null表示设为根分类
     */
    @Schema(description = "父分类ID，null表示设为根分类", example = "cat_1")
    private String parentId;

    /**
     * 分类描述
     */
    @Size(max = 500, message = "分类描述长度不能超过500个字符")
    @Schema(description = "分类描述", example = "存放学术论文相关文档")
    private String description;

    /**
     * 排序顺序
     */
    @Schema(description = "排序顺序，数值越小排序越靠前", example = "1")
    private Integer sortOrder;
}