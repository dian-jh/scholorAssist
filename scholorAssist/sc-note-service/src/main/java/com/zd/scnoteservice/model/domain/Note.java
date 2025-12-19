package com.zd.scnoteservice.model.domain;


import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.type.JdbcType;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 笔记实体类
 * 
 * <p>对应数据库表：notes</p>
 * <p>存储用户在阅读文档时创建的笔记和标注信息</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "notes", autoResultMap = true)
public class Note implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 笔记唯一标识，格式：note_xxxxxxxxx
     */
    @TableField("note_id")
    private String noteId;

    /**
     * 所属用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 关联的文档ID
     */
    @TableField("document_id")
    private String documentId;

    /**
     * 笔记标题
     */
    @TableField("title")
    private String title;

    /**
     * 笔记内容
     */
    @TableField("content")
    private String content;

    /**
     * 关联的页码
     */
    @TableField("page_number")
    private Integer pageNumber;

    /**
     * 选中的原文文本
     */
    @TableField("selected_text")
    private String selectedText;

    /**
     * 位置信息（坐标、高亮区域等）
     */
    @TableField(value = "position_info", typeHandler = com.zd.scnoteservice.config.PostgreSQLJsonbTypeHandler.class,
            jdbcType = JdbcType.OTHER)
    private Map<String, Object> positionInfo;

    /**
     * 标签数组
     */
    @TableField(value = "tags", typeHandler = com.zd.scnoteservice.config.PostgreSQLJsonbTypeHandler.class)
    private String[] tags;

    /**
     * 是否收藏
     */
    @TableField("is_favorite")
    private Boolean isFavorite;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}