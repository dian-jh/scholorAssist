package com.zd.scliteraturemanage.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 文档上传响应DTO
 * 
 * <p>用于返回文档上传后的基本信息</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档上传响应")
public class DocumentUploadResponse {

    @Schema(description = "文档唯一标识", example = "doc_123456789")
    private String id;

    @Schema(description = "文档标题", example = "新上传文档")
    private String title;

    @Schema(description = "文件名", example = "document.pdf")
    private String filename;

    @Schema(description = "所属分类ID", example = "cat_1")
    private String categoryId;

    @Schema(description = "作者", example = "未知作者")
    private String author;

    @Schema(description = "上传时间", example = "2024-01-21T10:30:00Z")
    private LocalDateTime uploadDate;

    @Schema(description = "文件大小", example = "1.5 MB")
    private String fileSize;

    @Schema(description = "页数", example = "10")
    private Integer pages;

    @Schema(description = "处理状态", example = "processing", allowableValues = {"ready", "processing"})
    private String status;



    @Schema(description = "摘要", example = "文档正在处理中...")
    private String abstractText;

    @Schema(description = "标签列表")
    private String[] tags;

    @Schema(description = "阅读进度", example = "0", minimum = "0", maximum = "1")
    private BigDecimal readProgress;

    public static DocumentUploadResponseBuilder builder() {
        return new DocumentUploadResponseBuilder();
    }

    public static class DocumentUploadResponseBuilder {
        private String id;
        private String title;
        private String filename;
        private String categoryId;
        private String author;
        private LocalDateTime uploadDate;
        private String fileSize;
        private Integer pages;
        private String status;
        private String abstractText;
        private String[] tags;
        private BigDecimal readProgress;

        public DocumentUploadResponseBuilder id(String id) {
            this.id = id;
            return this;
        }

        public DocumentUploadResponseBuilder title(String title) {
            this.title = title;
            return this;
        }

        public DocumentUploadResponseBuilder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public DocumentUploadResponseBuilder categoryId(String categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public DocumentUploadResponseBuilder author(String author) {
            this.author = author;
            return this;
        }

        public DocumentUploadResponseBuilder uploadDate(LocalDateTime uploadDate) {
            this.uploadDate = uploadDate;
            return this;
        }

        public DocumentUploadResponseBuilder fileSize(String fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public DocumentUploadResponseBuilder pages(Integer pages) {
            this.pages = pages;
            return this;
        }

        public DocumentUploadResponseBuilder status(String status) {
            this.status = status;
            return this;
        }

        public DocumentUploadResponseBuilder abstractText(String abstractText) {
            this.abstractText = abstractText;
            return this;
        }

        public DocumentUploadResponseBuilder tags(String[] tags) {
            this.tags = tags;
            return this;
        }

        public DocumentUploadResponseBuilder readProgress(BigDecimal readProgress) {
            this.readProgress = readProgress;
            return this;
        }

        public DocumentUploadResponse build() {
            DocumentUploadResponse response = new DocumentUploadResponse();
            response.id = this.id;
            response.title = this.title;
            response.filename = this.filename;
            response.categoryId = this.categoryId;
            response.author = this.author;
            response.uploadDate = this.uploadDate;
            response.fileSize = this.fileSize;
            response.pages = this.pages;
            response.status = this.status;
            response.abstractText = this.abstractText;
            response.tags = this.tags;
            response.readProgress = this.readProgress;
            return response;
        }
    }
}