package com.zd.scliteraturemanage.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * 文档详情响应DTO
 *
 * <p>用于返回单个文档的详细信息；包含文件路径转换辅助方法</p>
 *
 * @author System
 * @since 2024-01-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档详情响应")
@JsonInclude(Include.NON_NULL)
public class DocumentDetailResponse {

    @Schema(description = "文档唯一标识", example = "doc_1")
    private String id;

    @Schema(description = "文档标题", example = "Attention Is All You Need")
    private String title;

    @Schema(description = "文件名", example = "attention_is_all_you_need.pdf")
    private String filename;

    @Schema(description = "所属分类ID", example = "cat_2")
    private String categoryId;

    @Schema(description = "所属用户ID", example = "user_123")
    private String userId;

    @Schema(description = "作者", example = "Vaswani et al.")
    private String author;

    @Schema(description = "上传时间", example = "2024-01-15T10:30:00Z")
    private LocalDateTime uploadDate;

    @Schema(description = "处理完成时间", example = "2024-01-15T11:00:00Z")
    private LocalDateTime processedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "文件大小（显示）", example = "2.3 MB")
    private String fileSize;

    @Schema(description = "文件大小（字节）", example = "2411720")
    private Long fileSizeBytes;

    @Schema(description = "页数", example = "15")
    private Integer pages;

    @Schema(description = "处理状态", example = "ready", allowableValues = {"ready", "processing", "failed"})
    private String status;

    @Schema(description = "摘要", example = "The dominant sequence transduction models...")
    private String abstractText;

    @Schema(description = "标签列表", example = "[\"transformer\", \"attention\", \"nlp\"]")
    private String[] tags;

    @Schema(description = "阅读进度", example = "0.6", minimum = "0", maximum = "1")
    private BigDecimal readProgress;

    @Schema(description = "相对文件路径（存储）", example = "documents/2025/01/21/doc_123.pdf")
    private String filePath;

    @Schema(description = "绝对文件路径（转换后）")
    private String absoluteFilePath;

    @Schema(description = "PDF预览URL", example = "/api/files/documents/doc_1/download")
    private String previewUrl;

    /**
     * 将相对路径转换为绝对路径（跨平台处理路径分隔符）
     *
     * <p>使用系统配置的根路径与相对路径进行拼接，统一分隔符。</p>
     *
     * @param rootBasePath 系统配置的文件存储根路径，例如 "./uploads" 或 "D:/data/uploads"
     * @param relativePath 存储的相对路径，例如 "documents/2025/01/21/doc_123.pdf"
     * @return 绝对路径字符串，符合当前操作系统的路径格式
     */
    public static String toAbsolutePath(String rootBasePath, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        String base = normalizeSeparators(rootBasePath);
        String rel = normalizeSeparators(relativePath);
        // 去除相对路径前导分隔符，避免Paths.get将其视为绝对路径
        rel = rel.replaceAll("^[/\\\\]+", "");
        Path full = Paths.get(base, rel).normalize();
        return full.toString();
    }

    /**
     * 统一路径分隔符为当前系统分隔符
     *
     * @param path 任意分隔符的路径
     * @return 使用系统分隔符的路径
     */
    public static String normalizeSeparators(String path) {
        if (path == null) {
            return null;
        }
        // 先统一为系统分隔符
        String unified = path.replace("\\", File.separator).replace("/", File.separator);
        // 将可能出现的多个分隔符合并为一个
        String sepRegex = File.separator.equals("\\") ? "\\\\+" : "/+";
        return unified.replaceAll(sepRegex, File.separator);
    }

    /**
     * 应用根路径，计算并设置 absoluteFilePath 字段
     *
     * @param rootBasePath 系统配置的文件存储根路径
     */
    public void applyBasePath(String rootBasePath) {
        this.absoluteFilePath = toAbsolutePath(rootBasePath, this.filePath);
    }

    public static DocumentDetailResponseBuilder builder() {
        return new DocumentDetailResponseBuilder();
    }

    public static class DocumentDetailResponseBuilder {
        private String id;
        private String title;
        private String filename;
        private String categoryId;
        private String userId;
        private String author;
        private LocalDateTime uploadDate;
        private LocalDateTime processedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String fileSize;
        private Long fileSizeBytes;
        private Integer pages;
        private String status;
        private String abstractText;
        private String[] tags;
        private BigDecimal readProgress;
        private String filePath;
        private String absoluteFilePath;
        private String previewUrl;

        public DocumentDetailResponseBuilder id(String id) {
            this.id = id;
            return this;
        }

        public DocumentDetailResponseBuilder title(String title) {
            this.title = title;
            return this;
        }

        public DocumentDetailResponseBuilder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public DocumentDetailResponseBuilder categoryId(String categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public DocumentDetailResponseBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public DocumentDetailResponseBuilder author(String author) {
            this.author = author;
            return this;
        }

        public DocumentDetailResponseBuilder uploadDate(LocalDateTime uploadDate) {
            this.uploadDate = uploadDate;
            return this;
        }

        public DocumentDetailResponseBuilder processedAt(LocalDateTime processedAt) {
            this.processedAt = processedAt;
            return this;
        }

        public DocumentDetailResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public DocumentDetailResponseBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public DocumentDetailResponseBuilder fileSize(String fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public DocumentDetailResponseBuilder fileSizeBytes(Long fileSizeBytes) {
            this.fileSizeBytes = fileSizeBytes;
            return this;
        }

        public DocumentDetailResponseBuilder pages(Integer pages) {
            this.pages = pages;
            return this;
        }

        public DocumentDetailResponseBuilder status(String status) {
            this.status = status;
            return this;
        }

        public DocumentDetailResponseBuilder abstractText(String abstractText) {
            this.abstractText = abstractText;
            return this;
        }

        public DocumentDetailResponseBuilder tags(String[] tags) {
            this.tags = tags;
            return this;
        }

        public DocumentDetailResponseBuilder readProgress(BigDecimal readProgress) {
            this.readProgress = readProgress;
            return this;
        }

        public DocumentDetailResponseBuilder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public DocumentDetailResponseBuilder absoluteFilePath(String absoluteFilePath) {
            this.absoluteFilePath = absoluteFilePath;
            return this;
        }

        public DocumentDetailResponseBuilder previewUrl(String previewUrl) {
            this.previewUrl = previewUrl;
            return this;
        }

        public DocumentDetailResponse build() {
            DocumentDetailResponse response = new DocumentDetailResponse();
            response.id = this.id;
            response.title = this.title;
            response.filename = this.filename;
            response.categoryId = this.categoryId;
            response.userId = this.userId;
            response.author = this.author;
            response.uploadDate = this.uploadDate;
            response.processedAt = this.processedAt;
            response.createdAt = this.createdAt;
            response.updatedAt = this.updatedAt;
            response.fileSize = this.fileSize;
            response.fileSizeBytes = this.fileSizeBytes;
            response.pages = this.pages;
            response.status = this.status;
            response.abstractText = this.abstractText;
            response.tags = this.tags;
            response.readProgress = this.readProgress;
            response.filePath = this.filePath;
            response.absoluteFilePath = this.absoluteFilePath;
            response.previewUrl = this.previewUrl;
            return response;
        }
    }
}