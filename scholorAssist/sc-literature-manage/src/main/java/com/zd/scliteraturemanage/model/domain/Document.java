package com.zd.scliteraturemanage.model.domain;


import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 文档实体类
 *
 * <p>对应数据库表：documents</p>
 * <p>存储PDF文档的基本信息、元数据和处理状态</p>
 *
 * @author System
 * @since 2024-01-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "documents", autoResultMap = true)
public class Document implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文档唯一标识，使用雪花算法生成，格式：doc_xxxxxxxxx
     */
    @TableField("document_id")
    private String documentId;

    /**
     * 所属用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 所属分类ID
     */
    @TableField("category_id")
    private String categoryId;

    /**
     * 文档标题
     */
    @TableField("title")
    private String title;

    /**
     * 原始文件名
     */
    @TableField("filename")
    private String filename;

    /**
     * 作者
     */
    @TableField("author")
    private String author;

    /**
     * 文档摘要
     */
    @TableField("abstract")
    private String abstractText;

    /**
     * 文件存储URL（相对路径）
     */
    @TableField("file_path")
    private String filePath;

    /**
     * 文件大小（字节）
     */
    @TableField("file_size")
    private Long fileSize;

    /**
     * 文件大小显示（如：2.3 MB）
     */
    @TableField("file_size_display")
    private String fileSizeDisplay;

    /**
     * 页数
     */
    @TableField("pages")
    private Integer pages;

    /**
     * 处理状态：processing/ready/failed
     */
    @TableField("status")
    private String status;

    /**
     * 标签数组（JSON格式存储）
     */
    @TableField(value = "tags", typeHandler = com.zd.scliteraturemanage.config.PostgreSQLJsonbTypeHandler.class)
    private String[] tags;

    /**
     * 阅读进度（0-1）
     */
    @TableField("read_progress")
    private BigDecimal readProgress;

    /**
     * 上传时间
     */
    @TableField("upload_date")
    private LocalDateTime uploadDate;

    /**
     * 处理完成时间
     */
    @TableField("processed_at")
    private LocalDateTime processedAt;

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

    // --- 手动 Getter/Setter 保持不变 ---
    public String getDocumentId() {
        return documentId;
    }

    public String getTitle() {
        return title;
    }

    public String getFilename() {
        return filename;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public String getFileSizeDisplay() {
        return fileSizeDisplay;
    }

    public Integer getPages() {
        return pages;
    }

    public String getStatus() {
        return status;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public String[] getTags() {
        return tags;
    }

    public BigDecimal getReadProgress() {
        return readProgress;
    }

    // getAuthor 方法现在直接返回字段值
    public String getAuthor() {
        return author;
    }

    public String getUserId() {
        return userId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static DocumentBuilder builder() {
        return new DocumentBuilder();
    }

    // --- Builder 类的完整实现 ---
//    public static class DocumentBuilder {
//        private Long id;
//        private String documentId;
//        private String userId;
//        private String categoryId;
//        private String title;
//        private String filename;
//        private String author; // Builder中添加author
//        private String abstractText;
//        private String fileUrl;
//        private Long fileSize;
//        private String fileSizeDisplay;
//        private Integer pages;
//        private String status;
//        private String[] tags;
//        private BigDecimal readProgress;
//        private LocalDateTime uploadDate;
//        private LocalDateTime processedAt;
//        private LocalDateTime createdAt;
//        private LocalDateTime updatedAt;
//
//        public DocumentBuilder id(Long id) {
//            this.id = id;
//            return this;
//        }
//
//        public DocumentBuilder documentId(String documentId) {
//            this.documentId = documentId;
//            return this;
//        }
//
//        public DocumentBuilder userId(String userId) {
//            this.userId = userId;
//            return this;
//        }
//
//        public DocumentBuilder categoryId(String categoryId) {
//            this.categoryId = categoryId;
//            return this;
//        }
//
//        public DocumentBuilder title(String title) {
//            this.title = title;
//            return this;
//        }
//
//        public DocumentBuilder filename(String filename) {
//            this.filename = filename;
//            return this;
//        }
//
//        public DocumentBuilder author(String author) { // author的builder方法
//            this.author = author;
//            return this;
//        }
//
//        public DocumentBuilder abstractText(String abstractText) {
//            this.abstractText = abstractText;
//            return this;
//        }
//
//        public DocumentBuilder fileUrl(String fileUrl) {
//            this.fileUrl = fileUrl;
//            return this;
//        }
//
//        public DocumentBuilder fileSize(Long fileSize) {
//            this.fileSize = fileSize;
//            return this;
//        }
//
//        public DocumentBuilder fileSizeDisplay(String fileSizeDisplay) {
//            this.fileSizeDisplay = fileSizeDisplay;
//            return this;
//        }
//
//        public DocumentBuilder pages(Integer pages) {
//            this.pages = pages;
//            return this;
//        }
//
//        public DocumentBuilder status(String status) {
//            this.status = status;
//            return this;
//        }
//
//        public DocumentBuilder tags(String[] tags) {
//            this.tags = tags;
//            return this;
//        }
//
//        public DocumentBuilder readProgress(BigDecimal readProgress) {
//            this.readProgress = readProgress;
//            return this;
//        }
//
//        public DocumentBuilder uploadDate(LocalDateTime uploadDate) {
//            this.uploadDate = uploadDate;
//            return this;
//        }
//
//        public DocumentBuilder processedAt(LocalDateTime processedAt) {
//            this.processedAt = processedAt;
//            return this;
//        }
//
//        public DocumentBuilder createdAt(LocalDateTime createdAt) {
//            this.createdAt = createdAt;
//            return this;
//        }
//
//        public DocumentBuilder updatedAt(LocalDateTime updatedAt) {
//            this.updatedAt = updatedAt;
//            return this;
//        }
//
//        /**
//         * 【完整且正确的 build 方法】
//         * 这个方法现在会设置所有的字段，包括新增的 author 字段。
//         */
////        public Document build() {
////            Document document = new Document();
////            document.id = this.id;
////            document.documentId = this.documentId;
////            document.userId = this.userId;
////            document.categoryId = this.categoryId;
////            document.title = this.title;
////            document.filename = this.filename;
////            document.author = this.author; // 正确设置 author
////            document.abstractText = this.abstractText;
////            document.filePath = this.filePath;
////            document.fileSize = this.fileSize;
////            document.fileSizeDisplay = this.fileSizeDisplay;
////            document.pages = this.pages;
////            document.status = this.status;
////            document.tags = this.tags;
////            document.readProgress = this.readProgress;
////            document.uploadDate = this.uploadDate;
////            document.processedAt = this.processedAt;
////            document.createdAt = this.createdAt;
////            document.updatedAt = this.updatedAt;
////            return document;
////        }
//    }
}