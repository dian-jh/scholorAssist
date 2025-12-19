package com.zd.scliteraturemanage.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 文档列表响应DTO
 * 
 * <p>用于返回文档列表信息，包含文档的基本信息和状态</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档列表响应")
public class DocumentListResponse {

    @Schema(description = "文档唯一标识", example = "doc_1")
    private String id;

    @Schema(description = "文档标题", example = "Attention Is All You Need")
    private String title;

    @Schema(description = "文件名", example = "attention_is_all_you_need.pdf")
    private String filename;

    @Schema(description = "所属分类ID", example = "cat_2")
    private String categoryId;

    @Schema(description = "作者", example = "Vaswani et al.")
    private String author;

    @Schema(description = "上传时间", example = "2024-01-15T10:30:00Z")
    private LocalDateTime uploadDate;

    @Schema(description = "文件大小", example = "2.3 MB")
    private String fileSize;

    @Schema(description = "页数", example = "15")
    private Integer pages;

    @Schema(description = "处理状态", example = "ready", allowableValues = {"ready", "processing"})
    private String status;



    @Schema(description = "摘要", example = "The dominant sequence transduction models...")
    private String abstractText;

    @Schema(description = "标签列表", example = "[\"transformer\", \"attention\", \"nlp\"]")
    private String[] tags;

    @Schema(description = "阅读进度", example = "0.6", minimum = "0", maximum = "1")
    private BigDecimal readProgress;

    public static DocumentListResponseBuilder builder() {
        return new DocumentListResponseBuilder();
    }

    public static class DocumentListResponseBuilder {
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

        public DocumentListResponseBuilder id(String id) {
            this.id = id;
            return this;
        }

        public DocumentListResponseBuilder title(String title) {
            this.title = title;
            return this;
        }

        public DocumentListResponseBuilder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public DocumentListResponseBuilder categoryId(String categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public DocumentListResponseBuilder author(String author) {
            this.author = author;
            return this;
        }

        public DocumentListResponseBuilder uploadDate(LocalDateTime uploadDate) {
            this.uploadDate = uploadDate;
            return this;
        }

        public DocumentListResponseBuilder fileSize(String fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public DocumentListResponseBuilder pages(Integer pages) {
            this.pages = pages;
            return this;
        }

        public DocumentListResponseBuilder status(String status) {
            this.status = status;
            return this;
        }

        public DocumentListResponseBuilder abstractText(String abstractText) {
            this.abstractText = abstractText;
            return this;
        }

        public DocumentListResponseBuilder tags(String[] tags) {
            this.tags = tags;
            return this;
        }

        public DocumentListResponseBuilder readProgress(BigDecimal readProgress) {
            this.readProgress = readProgress;
            return this;
        }

        public DocumentListResponse build() {
            DocumentListResponse response = new DocumentListResponse();
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