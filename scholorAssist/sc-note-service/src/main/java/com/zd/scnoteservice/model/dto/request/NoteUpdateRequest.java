package com.zd.scnoteservice.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.Map;

/**
 * 更新笔记请求DTO
 * 
 * <p>用于接收更新笔记的请求参数</p>
 * <p>所有字段都是可选的，只更新传入的非空字段</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "更新笔记请求")
public class NoteUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 笔记标题
     */
    @Size(min = 1, max = 100, message = "笔记标题长度必须在1-100字符之间")
    @Schema(description = "笔记标题", example = "更新后的标题")
    private String title;

    /**
     * 笔记内容
     */
    @Size(max = 10000, message = "笔记内容不能超过10000字符")
    @Schema(description = "笔记内容", example = "更新后的内容...")
    private String content;

    /**
     * 关联的页码
     */
    @Schema(description = "关联的页码", example = "6")
    private Integer pageNumber;

    /**
     * 标签列表
     */
    @Size(max = 10, message = "标签数量不能超过10个")
    @Schema(description = "标签列表", example = "[\"更新\", \"重要\"]")
    private String[] tags;

    /**
     * 选中的原文文本
     */
    @Schema(description = "选中的原文文本", example = "更新后的选中文本...")
    private String selectedText;

    /**
     * 位置信息（坐标、高亮区域等）
     */
    @Schema(description = "位置信息", example = "{\"x\": 150, \"y\": 250}")
    private Map<String, Object> positionInfo;

    /**
     * 是否收藏
     */
    @Schema(description = "是否收藏", example = "true")
    private Boolean isFavorite;
}