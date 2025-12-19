package com.zd.scliteraturemanage.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文档分片实体类
 * 
 * <p>对应数据库表：document_chunks</p>
 * <p>存储文档的文本片段和向量嵌入，用于AI语义检索</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("document_chunks")
public class DocumentChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 分片唯一标识，格式：chunk_xxxxxxxxx
     */
    @TableField("chunk_id")
    private String chunkId;

    /**
     * 所属文档ID
     */
    @TableField("document_id")
    private String documentId;

    /**
     * 所在页码
     */
    @TableField("page_number")
    private Integer pageNumber;

    /**
     * 在页面中的分片索引
     */
    @TableField("chunk_index")
    private Integer chunkIndex;

    /**
     * 分片文本内容
     */
    @TableField("content")
    private String content;

    /**
     * 内容长度
     */
    @TableField("content_length")
    private Integer contentLength;

    /**
     * 向量嵌入（OpenAI ada-002维度）
     */
    @TableField("embedding_vector")
    private String embeddingVector;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}