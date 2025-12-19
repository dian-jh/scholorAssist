package com.zd.scliteraturemanage.controller;

import com.zd.scliteraturemanage.model.dto.request.ProgressUpdateRequest;
import com.zd.scliteraturemanage.model.dto.response.*;
import com.zd.scliteraturemanage.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.nio.file.Paths;

import com.zd.scliteraturemanage.model.domain.Document;
import com.zd.scliteraturemanage.manager.DocumentManager;
import com.zd.sccommon.utils.FileStorageUtil;
import com.zd.sccommon.utils.UserContextUtil;
import com.zd.sccommon.common.BusinessException;

/**
 * 文档管理控制器
 * 
 * <p>提供文档管理的RESTful接口，包括文档上传、查询、更新、删除等功能</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Validated
@Tag(name = "文档管理", description = "文档上传、查询、更新、删除等功能")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);
    private final DocumentService documentService;
    private final DocumentManager documentManager;
    private final FileStorageUtil fileStorageUtil;

    @Value("${app.file.storage.static-prefix:/files/}")
    private String staticPrefix;

    // 不使用域名拼接，统一返回相对路径以适配本地开发

    @GetMapping
    @Operation(summary = "获取文档列表", description = "分页获取文档列表，支持分类筛选和搜索")
    public List<DocumentListResponse> getDocuments(
            @Parameter(description = "分类ID，不传或传'all'表示获取全部", example = "cat_1")
            @RequestParam(value = "category_id", required = false) String categoryId,
            
            @Parameter(description = "搜索关键词，支持标题、作者、摘要搜索", example = "transformer")
            @RequestParam(required = false) String search,
            
            @Parameter(description = "页码，从1开始", example = "1")
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            
            @Parameter(description = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer pageSize) {
        
        log.info("获取文档列表请求，categoryId: {}, search: {}, page: {}, pageSize: {}", 
                categoryId, search, page, pageSize);
        
        return documentService.getDocuments(categoryId, search, page, pageSize);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取文档详情", description = "根据文档ID获取文档详细信息")
    public DocumentDetailResponse getDocumentDetail(
            @Parameter(description = "文档ID", example = "doc_1", required = true)
            @PathVariable String id) {

        log.info("获取文档详情请求，documentId: {}", id);
        DocumentDetailResponse response = documentService.getDocumentDetail(id);

        // 将存储的相对文件路径转换为静态资源相对URL，例如：
        // /files/2025/11/07/doc_xxx.pdf （不带域名）
        // 规则：去除相对路径中的"documents/"前缀，并统一分隔符
        try {
            String filePath = response.getFilePath();
            if (filePath != null && !filePath.isBlank()) {
                String normalized = filePath.replace("\\", "/");
                String withoutDocuments = normalized.startsWith("documents/")
                        ? normalized.substring("documents/".length())
                        : normalized;

                String prefix = staticPrefix.endsWith("/") ? staticPrefix : staticPrefix + "/";
                String fileUrl = prefix + withoutDocuments;

                // 仅返回 filePath（供前端 iframe 使用），不返回 previewUrl / absoluteFilePath
                response.setFilePath(fileUrl);
                response.setPreviewUrl(null);
                response.setAbsoluteFilePath(null);
                log.info("生成静态资源URL，documentId: {}, url: {}", id, fileUrl);
            } else {
                log.warn("文档详情缺少文件路径，无法生成静态资源URL，documentId: {}", id);
            }
        } catch (Exception e) {
            // 发生异常不影响基础数据返回，但记录警告日志
            log.warn("生成静态资源URL时发生异常，documentId: {}，error: {}", id, e.getMessage());
        }

        return response;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文档", description = "上传PDF文档并进行解析处理")
    public DocumentUploadResponse uploadDocument(
            @Parameter(description = "PDF文件，最大50MB", required = true)
            @RequestParam("file") MultipartFile file,
            
            @Parameter(description = "文档标题，不传则使用文件名", example = "深度学习论文")
            @RequestParam(required = false) String title,
            
            @Parameter(description = "分类ID，不传则放入默认分类", example = "cat_1")
            @RequestParam(value = "category_id", required = false) String categoryId) {
        
        log.info("上传文档请求，filename: {}, title: {}, categoryId: {}", 
                file.getOriginalFilename(), title, categoryId);
        
        return documentService.uploadDocument(file, title, categoryId);
    }

    @PostMapping("/{id}/progress")
    @Operation(summary = "更新阅读进度", description = "更新文档的阅读进度")
    public ProgressUpdateResponse updateProgress(
            @Parameter(description = "文档ID", example = "doc_1", required = true)
            @PathVariable String id,
            
            @Parameter(description = "进度更新请求", required = true)
            @Valid @RequestBody ProgressUpdateRequest request) {
        
        log.info("更新阅读进度请求，documentId: {}, progress: {}", id, request.getProgress());
        
        return documentService.updateProgress(id, request);
    }

    @PostMapping("/{id}/delete")
    @Operation(summary = "删除文档", description = "删除指定的文档")
    public void deleteDocument(
            @Parameter(description = "文档ID", example = "doc_1", required = true)
            @PathVariable String id) {
        
        log.info("删除文档请求，documentId: {}", id);
        
        documentService.deleteDocument(id);
    }
}