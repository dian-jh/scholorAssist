package com.zd.sccommon.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 文件存储工具类
 * 
 * <p>提供PDF文件的本地存储功能，包括目录管理、文件命名、安全检查等</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Slf4j
@Component
public class FileStorageUtil {

    /**
     * 文件存储根目录
     */
    @Value("${app.file.storage.root-path:./uploads}")
    private String rootPath;

    /**
     * 文档存储子目录
     */
    @Value("${app.file.storage.document-path:documents}")
    private String documentPath;

    

    /**
     * 最大文件大小（字节）
     */
    @Value("${app.file.storage.max-file-size:52428800}") // 50MB
    private long maxFileSize;

    /**
     * 允许的文件扩展名
     */
    private static final String[] ALLOWED_EXTENSIONS = {".pdf"};

    /**
     * 日期格式化器
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * 存储PDF文件
     * 
     * @param file 上传的文件
     * @param documentId 文档ID
     * @return 文件存储信息
     * @throws IOException 文件操作异常
     */
    public FileStorageInfo storeDocument(MultipartFile file, String documentId) throws IOException {
        log.info("开始存储文档文件，documentId: {}, filename: {}, size: {}", 
                documentId, file.getOriginalFilename(), file.getSize());

        // 校验文件
        validateFile(file);

        // 生成存储路径
        String relativePath = generateDocumentPath(documentId);
        // 统一为绝对路径，避免出现以 ./ 开头的相对路径
        Path fullPath = Paths.get(rootPath, relativePath).toAbsolutePath().normalize();

        // 确保目录存在
        createDirectoriesIfNotExists(fullPath.getParent());

        // 存储文件
        Files.copy(file.getInputStream(), fullPath, StandardCopyOption.REPLACE_EXISTING);

        // 构建文件信息
        FileStorageInfo storageInfo = FileStorageInfo.builder()
                .documentId(documentId)
                .originalFilename(file.getOriginalFilename())
                .storedFilename(fullPath.getFileName().toString())
                .relativePath(relativePath)
                .fullPath(fullPath.toString())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .build();

        log.info("文档文件存储成功，documentId: {}, path: {}", documentId, relativePath);
        return storageInfo;
    }



    /**
     * 删除文档文件
     * 
     * @param relativePath 相对路径
     * @return 是否删除成功
     */
    public boolean deleteDocument(String relativePath) {
        try {
            Path fullPath = Paths.get(rootPath, relativePath).toAbsolutePath().normalize();
            boolean deleted = Files.deleteIfExists(fullPath);
            
            if (deleted) {
                log.info("文档文件删除成功，path: {}", relativePath);
                // 尝试删除空目录
                cleanupEmptyDirectories(fullPath.getParent());
            } else {
                log.warn("文档文件不存在或删除失败，path: {}", relativePath);
            }
            
            return deleted;
        } catch (IOException e) {
            log.error("删除文档文件失败，path: {}", relativePath, e);
            return false;
        }
    }



    /**
     * 检查文件是否存在
     * 
     * @param relativePath 相对路径
     * @return 是否存在
     */
    public boolean fileExists(String relativePath) {
        Path fullPath = Paths.get(rootPath, relativePath).toAbsolutePath().normalize();
        return Files.exists(fullPath);
    }

    /**
     * 获取文件的完整路径
     * 
     * @param relativePath 相对路径
     * @return 完整路径
     */
    public String getFullPath(String relativePath) {
        // 返回绝对路径并规范化，确保跨平台兼容
        return Paths.get(rootPath, relativePath).toAbsolutePath().normalize().toString();
    }

    /**
     * 获取文件访问URL
     * 
     * @param relativePath 相对路径
     * @return 访问URL
     */
    public String getFileUrl(String relativePath) {
        // 这里返回相对路径，实际的URL前缀由前端或网关处理
        return "/" + relativePath.replace("\\", "/");
    }

    /**
     * 校验上传文件
     * 
     * @param file 上传文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        // 检查文件大小
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                String.format("文件大小超过限制，最大允许: %d MB", maxFileSize / 1024 / 1024));
        }

        // 检查文件扩展名
        String filename = file.getOriginalFilename();
        if (filename == null || !isAllowedExtension(filename)) {
            throw new IllegalArgumentException("不支持的文件格式，仅支持PDF文件");
        }

        // 检查MIME类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new IllegalArgumentException("文件类型错误，仅支持PDF文件");
        }
    }

    /**
     * 检查文件扩展名是否允许
     * 
     * @param filename 文件名
     * @return 是否允许
     */
    private boolean isAllowedExtension(String filename) {
        String lowerFilename = filename.toLowerCase();
        for (String extension : ALLOWED_EXTENSIONS) {
            if (lowerFilename.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生成文档存储路径
     * 
     * @param documentId 文档ID
     * @return 相对路径
     */
    private String generateDocumentPath(String documentId) {
        String dateDir = LocalDate.now().format(DATE_FORMATTER);
        return Paths.get(documentPath, dateDir, documentId + ".pdf").toString();
    }



    /**
     * 创建目录（如果不存在）
     * 
     * @param path 目录路径
     * @throws IOException 创建失败
     */
    private void createDirectoriesIfNotExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            log.debug("创建目录: {}", path);
        }
    }

    /**
     * 清理空目录
     * 
     * @param path 目录路径
     */
    private void cleanupEmptyDirectories(Path path) {
        try {
            // 只清理我们创建的目录结构，不清理根目录
            Path rootDir = Paths.get(rootPath);
            while (path != null && !path.equals(rootDir) && Files.exists(path)) {
                if (Files.list(path).findAny().isEmpty()) {
                    Files.delete(path);
                    log.debug("删除空目录: {}", path);
                    path = path.getParent();
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            log.debug("清理空目录时出错: {}", e.getMessage());
        }
    }

    /**
     * 文件存储信息
     */
    public static class FileStorageInfo {
        private String documentId;
        private String originalFilename;
        private String storedFilename;
        private String relativePath;
        private String fullPath;
        private long fileSize;
        private String contentType;

        // 默认构造函数
        public FileStorageInfo() {}

        // 简单构造函数，用于测试
        public FileStorageInfo(String relativePath) {
            this.relativePath = relativePath;
        }

        public static FileStorageInfoBuilder builder() {
            return new FileStorageInfoBuilder();
        }

        // Getters
        public String getDocumentId() { return documentId; }
        public String getOriginalFilename() { return originalFilename; }
        public String getStoredFilename() { return storedFilename; }
        public String getRelativePath() { return relativePath; }
        public String getFullPath() { return fullPath; }
        public long getFileSize() { return fileSize; }
        public String getContentType() { return contentType; }

        public static class FileStorageInfoBuilder {
            private String documentId;
            private String originalFilename;
            private String storedFilename;
            private String relativePath;
            private String fullPath;
            private long fileSize;
            private String contentType;

            public FileStorageInfoBuilder documentId(String documentId) {
                this.documentId = documentId;
                return this;
            }

            public FileStorageInfoBuilder originalFilename(String originalFilename) {
                this.originalFilename = originalFilename;
                return this;
            }

            public FileStorageInfoBuilder storedFilename(String storedFilename) {
                this.storedFilename = storedFilename;
                return this;
            }

            public FileStorageInfoBuilder relativePath(String relativePath) {
                this.relativePath = relativePath;
                return this;
            }

            public FileStorageInfoBuilder fullPath(String fullPath) {
                this.fullPath = fullPath;
                return this;
            }

            public FileStorageInfoBuilder fileSize(long fileSize) {
                this.fileSize = fileSize;
                return this;
            }

            public FileStorageInfoBuilder contentType(String contentType) {
                this.contentType = contentType;
                return this;
            }

            public FileStorageInfo build() {
                FileStorageInfo info = new FileStorageInfo();
                info.documentId = this.documentId;
                info.originalFilename = this.originalFilename;
                info.storedFilename = this.storedFilename;
                info.relativePath = this.relativePath;
                info.fullPath = this.fullPath;
                info.fileSize = this.fileSize;
                info.contentType = this.contentType;
                return info;
            }
        }
    }
}