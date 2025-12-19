package com.zd.scnoteservice.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * 查询笔记请求DTO
 * 
 * <p>用于接收查询笔记列表的请求参数</p>
 * <p>支持按文档筛选和分页查询</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "查询笔记请求")
public class NoteQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文档ID，不传则获取全部笔记
     */
    @Schema(description = "文档ID，不传则获取全部笔记", example = "doc_1")
    private String documentId;

    /**
     * 页码，从1开始
     */
    @Min(value = 1, message = "页码必须大于0")
    @Schema(description = "页码，从1开始", example = "1")
    private Integer page = 1;

    /**
     * 每页数量
     */
    @Min(value = 1, message = "每页数量必须大于0")
    @Max(value = 100, message = "每页数量不能超过100")
    @Schema(description = "每页数量", example = "20")
    private Integer pageSize = 20;

    /**
     * 是否只查询收藏的笔记
     */
    @Schema(description = "是否只查询收藏的笔记", example = "false")
    private Boolean onlyFavorites = false;

    /**
     * 标签筛选
     */
    @Schema(description = "标签筛选", example = "重要")
    private String tag;
}