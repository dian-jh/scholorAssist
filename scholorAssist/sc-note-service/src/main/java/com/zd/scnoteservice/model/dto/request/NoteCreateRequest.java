package com.zd.scnoteservice.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 创建笔记请求DTO
 * 
 * <p>用于接收创建笔记的请求参数</p>
 * <p>包含笔记的基本信息：文档ID、标题、内容、页码、标签等</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "创建笔记请求")
public class NoteCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关联的文档ID
     */
    @NotBlank(message = "文档ID不能为空")
    @Schema(description = "关联的文档ID", example = "doc_1", required = true)
    private String documentId;

    /**
     * 笔记标题
     */
    @NotBlank(message = "笔记标题不能为空")
    @Size(min = 1, max = 100, message = "笔记标题长度必须在1-100字符之间")
    @Schema(description = "笔记标题", example = "重要概念总结", required = true)
    private String title;

    /**
     * 笔记内容
     */
    @NotBlank(message = "笔记内容不能为空")
    @Size(max = 10000, message = "笔记内容不能超过10000字符")
    @Schema(description = "笔记内容", example = "这一章节介绍了Transformer的核心思想...", required = true)
    private String content;

    /**
     * 关联的页码
     */
    @Schema(description = "关联的页码", example = "5")
    private Integer pageNumber;

    /**
     * 标签列表
     */
    @Size(max = 10, message = "标签数量不能超过10个")
    @Schema(description = "标签列表", example = "[\"重要\", \"概念\"]")
    private String[] tags;

    /**
     * 选中的原文文本
     */
    @Schema(description = "选中的原文文本", example = "Transformer完全基于注意力机制...")
    private String selectedText;

    /**
     * 位置信息（坐标、高亮区域等）
     */
    @Schema(description = "位置信息", example = "{\"x\": 100, \"y\": 200}")
    private Map<String, Object> positionInfo;
}