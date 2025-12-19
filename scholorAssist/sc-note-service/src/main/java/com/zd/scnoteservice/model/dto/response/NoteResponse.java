package com.zd.scnoteservice.model.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 笔记响应DTO
 * 
 * <p>用于返回笔记信息给前端</p>
 * <p>包含笔记的完整信息，符合API文档规范</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "笔记响应")
public class NoteResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 笔记唯一标识
     */
    @Schema(description = "笔记唯一标识", example = "note_123456789")
    private String id;

    /**
     * 关联的文档ID
     */
    @Schema(description = "关联的文档ID", example = "doc_1")
    private String documentId;

    /**
     * 笔记标题
     */
    @Schema(description = "笔记标题", example = "Transformer架构要点")
    private String title;

    /**
     * 笔记内容
     */
    @Schema(description = "笔记内容", example = "Transformer完全基于注意力机制，摒弃了循环和卷积结构...")
    private String content;

    /**
     * 关联的页码
     */
    @Schema(description = "关联的页码", example = "3")
    private Integer pageNumber;

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

    /**
     * 标签列表
     */
    @Schema(description = "标签列表", example = "[\"transformer\", \"architecture\"]")
    private String[] tags;

    /**
     * 是否收藏
     */
    @Schema(description = "是否收藏", example = "false")
    private Boolean isFavorite;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Schema(description = "创建时间", example = "2024-01-20T10:30:00Z")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Schema(description = "更新时间", example = "2024-01-20T10:30:00Z")
    private LocalDateTime updatedAt;
}