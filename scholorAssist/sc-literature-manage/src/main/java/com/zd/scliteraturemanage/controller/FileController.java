package com.zd.scliteraturemanage.controller;

import com.zd.sccommon.common.BusinessException;
import com.zd.sccommon.utils.FileStorageUtil;
import com.zd.sccommon.utils.UserContextUtil;
import com.zd.scliteraturemanage.manager.DocumentManager;
import com.zd.scliteraturemanage.model.domain.Document;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件访问控制器
 * * <p>提供文档文件的下载功能，包含权限验证</p>
 * * @author System
 * @since 2024-01-21
 */
@Controller
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "文件访问", description = "文档文件下载功能")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final DocumentManager documentManager;
    private final FileStorageUtil fileStorageUtil;

    /**
     * 下载文档文件
     * * @param documentId 文档ID
     * @return 文件资源
     */
    @GetMapping("/documents/{documentId}/download")
    @Operation(summary = "下载文档文件", description = "下载指定文档的PDF文件")
    public void downloadDocument(
            @Parameter(description = "文档ID", example = "doc_1", required = true)
            @PathVariable String documentId,
            HttpServletResponse response) { // <-- 添加 HttpServletResponse

        String userId = UserContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        log.info("下载文档文件，userId: {}, documentId: {}", userId, documentId);

        // 获取文档信息
        Document document = documentManager.getByDocumentId(documentId);
        if (document == null) {
            throw new BusinessException(404, "文档不存在");
        }

        // 检查权限
        if (!userId.equals(document.getUserId())) {
            throw new BusinessException(403, "无权访问该文档");
        }

        // 检查文件是否存在
        if (document.getFilePath() == null || !fileStorageUtil.fileExists(document.getFilePath())) {
            throw new BusinessException(404, "文件不存在");
        }

        try {
            // 获取文件
            String fullPath = fileStorageUtil.getFullPath(document.getFilePath());
            File file = new File(fullPath);
            Resource resource = new FileSystemResource(file);

            if (!resource.exists()) {
                throw new BusinessException(404, "文件不存在");
            }

            // 【修改】直接设置 Response Headers（强化防拦截与兼容性）
            response.setContentType(MediaType.APPLICATION_PDF_VALUE);
            response.setContentLength((int) file.length());
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Accept-Ranges", "bytes");
            // 暴露必要头部，便于前端读取文件名和长度（跨域下有效）
            response.setHeader("Access-Control-Expose-Headers", String.join(", ",
                    HttpHeaders.CONTENT_TYPE,
                    HttpHeaders.CONTENT_LENGTH,
                    HttpHeaders.CONTENT_DISPOSITION,
                    "Accept-Ranges"
            ));

            // 设置响应头，使用 ContentDisposition 构建器并正确处理中文文件名；强制 inline 以避免下载管理器拦截
            ContentDisposition contentDisposition = ContentDisposition.builder("inline")
                    .filename(document.getFilename(), StandardCharsets.UTF_8)
                    .build();
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());

            // 【修改】直接将文件流复制到响应输出流
            try (InputStream inputStream = resource.getInputStream()) {
                StreamUtils.copy(inputStream, response.getOutputStream());
                response.flushBuffer();
            }

        } catch (AsyncRequestNotUsableException e) {
            // 客户端中断连接（例如插件拦截或页面切换）视为非致命情况，不返回500
            log.warn("客户端中断下载连接，documentId: {}, msg: {}", documentId, e.getMessage());
            return;
        } catch (java.io.IOException e) {
            String msg = String.valueOf(e.getMessage()).toLowerCase();
            if (msg.contains("connection reset") || msg.contains("broken pipe") || msg.contains("forcibly closed")) {
                // 常见的客户端主动断开场景
                log.warn("下载过程中连接被客户端关闭，documentId: {}, msg: {}", documentId, e.getMessage());
                return;
            }
            log.error("下载文档文件IO异常，documentId: {}", documentId, e);
            throw new BusinessException(500, "文件下载失败");
        } catch (Exception e) {
            log.error("下载文档文件失败，documentId: {}", documentId, e);
            throw new BusinessException(500, "文件下载失败");
        }
    }

    /**
     * 以字节流方式返回文档内容，避免浏览器或下载管理器基于扩展名/.pdf拦截
     * 请求路径不包含 .pdf，响应类型为 application/octet-stream
     */
    @GetMapping("/documents/{documentId}/bytes")
    @Operation(summary = "文档字节流", description = "返回指定文档的PDF字节流以供前端生成Blob")
    public void getDocumentBytes(
            @Parameter(description = "文档ID", example = "doc_1", required = true)
            @PathVariable String documentId,
            HttpServletResponse response) { // <-- 添加 HttpServletResponse

        String userId = UserContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        log.info("获取文档字节流，userId: {}, documentId: {}", userId, documentId);

        Document document = documentManager.getByDocumentId(documentId);
        if (document == null) {
            throw new BusinessException(404, "文档不存在");
        }

        if (!userId.equals(document.getUserId())) {
            throw new BusinessException(403, "无权访问该文档");
        }

        if (document.getFilePath() == null || !fileStorageUtil.fileExists(document.getFilePath())) {
            throw new BusinessException(404, "文件不存在");
        }

        try {
            String fullPath = fileStorageUtil.getFullPath(document.getFilePath());
            File file = new File(fullPath);
            if (!file.exists()) {
                throw new BusinessException(404, "文件不存在");
            }

            // 【修改】直接设置 Response Headers（返回字节流，前端转 PDF Blob）
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE); // 恢复为 OCTET_STREAM
            response.setContentLength((int) file.length());
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Accept-Ranges", "bytes");
            // 关键：不设置 Content-Disposition，避免浏览器或下载管理器误判为下载
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setStatus(HttpStatus.OK.value());
            // 暴露必要头部，便于前端读取长度（跨域下有效）
            response.setHeader("Access-Control-Expose-Headers", String.join(", ",
                    HttpHeaders.CONTENT_TYPE,
                    HttpHeaders.CONTENT_LENGTH,
                    "Accept-Ranges"
            ));

            // 【修改】直接将文件流复制到响应输出流
            try (InputStream inputStream = new FileSystemResource(file).getInputStream()) {
                StreamUtils.copy(inputStream, response.getOutputStream());
                response.flushBuffer();
            }

        } catch (AsyncRequestNotUsableException e) {
            log.warn("客户端中断字节流读取，documentId: {}, msg: {}", documentId, e.getMessage());
            return;
        } catch (java.io.IOException e) {
            String msg = String.valueOf(e.getMessage()).toLowerCase();
            if (msg.contains("connection reset") || msg.contains("broken pipe") || msg.contains("forcibly closed")) {
                log.warn("字节流传输过程中连接被客户端关闭，documentId: {}, msg: {}", documentId, e.getMessage());
                return;
            }
            log.error("读取文档字节流IO异常，documentId: {}", documentId, e);
            throw new BusinessException(500, "读取文件失败");
        } catch (Exception e) {
            log.error("读取文档字节流失败，documentId: {}", documentId, e);
            throw new BusinessException(500, "读取文件失败");
        }
    }
}