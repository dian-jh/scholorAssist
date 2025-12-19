package com.zd.scliteraturemanage.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zd.scapi.api.ai.AiDocumentApi;
import com.zd.sccommon.common.BusinessException;
import com.zd.sccommon.model.Result;
import com.zd.sccommon.model.UserContext;
import com.zd.sccommon.utils.FileStorageUtil;
import com.zd.sccommon.utils.UserContextHolder;
import com.zd.sccommon.utils.UserContextUtil;
import com.zd.scliteraturemanage.manager.DocumentManager;
import com.zd.scliteraturemanage.mapper.DocumentMapper;
import com.zd.scliteraturemanage.model.domain.Document;
import com.zd.scliteraturemanage.model.dto.request.ProgressUpdateRequest;
import com.zd.scliteraturemanage.model.dto.response.*;
import com.zd.scliteraturemanage.service.DocumentService;
import lombok.RequiredArgsConstructor;

// 【导入正确的 PDFBox 类】
import org.apache.pdfbox.Loader; // 【修正：导入 Loader】
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 文档业务服务实现类
 *
 * <p>实现文档管理的业务逻辑，包括文档上传、查询、更新等功能</p>
 *
 * @author System
 * @since 2024-01-21
 */
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);

    private final DocumentManager documentManager;
    private final DocumentMapper documentMapper;
    private final FileStorageUtil fileStorageUtil;

    private final AiDocumentApi aiDocumentApi;

    /**
     * Hutool雪花算法ID生成器
     * 使用机器ID=1，数据中心ID=1
     */
    private final Snowflake snowflake = IdUtil.getSnowflake(1, 1);

    // 【新增】用于提取摘要的正则表达式
    private static final Pattern ABSTRACT_PATTERN = Pattern.compile(
            "(?i)(Abstract|摘\\s*要)\\s*[:\\.]?\\s*([\\s\\S]+?)(?=(\\n\\n(1\\.?\\s*Introduction|Key\\s*Words|关\\s*键\\s*词)|$))",
            Pattern.DOTALL
    );

    /**
     * 【新增】用于存储PDF解析信息的内部辅助类
     */
    private static class PdfParseInfo {
        String title;
        String author;
        String abstractText = "未能自动提取摘要。"; // 默认值
        int pages = 0;
        boolean parseSuccess = false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentListResponse> getDocuments(String categoryId, String search, Integer page, Integer pageSize) {
        String userId = UserContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        log.info("获取文档列表，userId: {}, categoryId: {}, search: {}, page: {}, pageSize: {}",
                userId, categoryId, search, page, pageSize);

        List<Document> documents;

        if (search != null && !search.trim().isEmpty()) {
            // 搜索文档
            documents = documentManager.searchDocuments(userId, search.trim());
        } else if (categoryId != null && !"all".equals(categoryId)) {
            // 按分类查询
            documents = documentManager.getByUserIdAndCategoryId(userId, categoryId);
        } else {
            // 查询全部文档
            documents = documentManager.getByUserId(userId);
        }

        // 简单分页处理
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, documents.size());

        if (start >= documents.size()) {
            return List.of();
        }

        return documents.subList(start, end).stream()
                .map(this::convertToListResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDetailResponse getDocumentDetail(String documentId) {
        String userId = UserContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        log.info("获取文档详情，userId: {}, documentId: {}", userId, documentId);

        Document document = documentManager.getByDocumentId(documentId);
        if (document == null) {
            throw new BusinessException(404, "文档不存在");
        }

        // 检查文档是否属于当前用户
        if (!userId.equals(document.getUserId())) {
            throw new BusinessException(403, "无权访问该文档");
        }

        return convertToDetailResponse(document);
    }

    @Override
    @Transactional
    public DocumentUploadResponse uploadDocument(MultipartFile file, String title, String categoryId) {
        // 1. 用户登录校验
        String userId = UserContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        log.info("上传文档，userId: {}, filename: {}, title: {}, categoryId: {}",
                userId, file.getOriginalFilename(), title, categoryId);

        try {
            // 2. 校验文件格式与大小
            validateUploadFile(file);

            // 3. 生成文档ID
            String documentId = "doc_" + snowflake.nextId();

            // 4. 存储文件 (持久化到本地磁盘)
            // 使用 FileStorageUtil 将文件保存到配置的目录中
            FileStorageUtil.FileStorageInfo storageInfo = fileStorageUtil.storeDocument(file, documentId);

            // 【关键】获取文件存储的绝对路径，供后续 AI 服务直接读取
            String storedFilePath = storageInfo.getFullPath();

            // 5. 同步解析 PDF 基础信息 (用于提取标题、页数、摘要等元数据)
            // 这一步使用流读取，是为了快速响应前端，不需要等待 AI 深度解析
            PdfParseInfo parseInfo;
            try (InputStream is = file.getInputStream()) {
                parseInfo = extractPdfInfo(is);
            } catch (Exception e) {
                log.error("PDF基础解析失败，将使用默认值。 documentId: {}", documentId, e);
                parseInfo = new PdfParseInfo();
            }

            // 6. 确定最终标题 (优先级：用户输入 > PDF元数据 > 文件名)
            String finalTitle;
            if (title != null && !title.trim().isEmpty()) {
                finalTitle = title;
            } else if (parseInfo.title != null) {
                finalTitle = parseInfo.title;
            } else {
                finalTitle = getFileNameWithoutExtension(file.getOriginalFilename());
            }

            // 7. 构建文档实体对象
            Document document = Document.builder()
                    .documentId(documentId)
                    .userId(userId)
                    .categoryId(categoryId != null ? categoryId : "default")
                    .title(finalTitle)
                    .author(parseInfo.author)
                    .filename(file.getOriginalFilename())
                    .abstractText(parseInfo.abstractText)
                    .filePath(storageInfo.getRelativePath()) // 数据库存相对路径，方便迁移
                    .fileSize(file.getSize())
                    .fileSizeDisplay(formatFileSize(file.getSize()))
                    .pages(parseInfo.pages)
                    .status("ready") // 初始状态
                    .tags(new String[]{})
                    .readProgress(BigDecimal.ZERO)
                    .uploadDate(LocalDateTime.now())
                    .processedAt(LocalDateTime.now())
                    .build();

            // 8. 保存文档记录到数据库
            Document savedDocument = documentManager.createDocument(document);

            log.info("文档上传并保存成功，documentId: {}, path: {}", documentId, storageInfo.getRelativePath());

            // ================= 【核心：异步调用 AI 服务 (Path模式)】 =================
            // 获取当前主线程的用户上下文，以便传递给子线程
            UserContext parentContext = UserContextHolder.getContext();

            CompletableFuture.runAsync(() -> {
                try {
                    // 1. 设置子线程的用户上下文 (解决 Feign 拦截器获取不到 UserInfo 的问题)
                    UserContextHolder.setContext(parentContext);

                    log.info("发起AI向量化请求(Path模式): documentId={}, path={}", documentId, storedFilePath);

                    // 2. 远程调用 AI 服务 (直接传递本地绝对路径字符串)
                    // 这样避免了文件流传输，且解决了临时文件被删的问题
                    Result<Boolean> aiResult = aiDocumentApi.processDocumentEmbedding(storedFilePath, documentId);

                    if (!Boolean.TRUE.equals(aiResult.getData())) {
                        log.error("AI服务向量化失败: {}", aiResult.getMsg());
                        // 可选：在此处更新数据库状态，例如 status = "vector_failed"
                    } else {
                        log.info("AI服务向量化成功: documentId={}", documentId);
                    }
                } catch (Exception e) {
                    log.error("调用AI服务异常", e);
                } finally {
                    // 3. 清理子线程上下文，防止内存泄漏或污染
                    UserContextHolder.clear();
                }
            });
            // ===================================================================

            // 9. 立即返回响应给前端
            return DocumentUploadResponse.builder()
                    .id(savedDocument.getDocumentId())
                    .title(savedDocument.getTitle())
                    .filename(savedDocument.getFilename())
                    .fileSize(savedDocument.getFileSizeDisplay())
                    .status(savedDocument.getStatus())
                    .uploadDate(savedDocument.getUploadDate())
                    .pages(savedDocument.getPages())
                    .author(savedDocument.getAuthor())
                    .abstractText(savedDocument.getAbstractText())
                    .build();

        } catch (IOException e) {
            log.error("文件存储IO异常，userId: {}, filename: {}", userId, file.getOriginalFilename(), e);
            throw new BusinessException(500, "文件存储失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("文档上传业务异常，userId: {}, filename: {}", userId, file.getOriginalFilename(), e);
            if (e instanceof BusinessException) {
                throw (BusinessException) e;
            }
            throw new BusinessException(500, "文档上传失败：" + e.getMessage());
        }
    }
    /**
     * 【修正】PDF解析辅助方法
     */
    private PdfParseInfo extractPdfInfo(InputStream inputStream) throws IOException {
        PdfParseInfo info = new PdfParseInfo();

        // 【修正】使用 Loader.loadPDF 来加载 3.x 版本的 PDF
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDDocumentInformation docInfo = document.getDocumentInformation();

            // 1. 获取页数
            info.pages = document.getNumberOfPages();

            // 2. 尝试从元数据获取标题和作者
            if (docInfo.getTitle() != null && !docInfo.getTitle().trim().isEmpty()) {
                info.title = docInfo.getTitle().trim();
            }
            if (docInfo.getAuthor() != null && !docInfo.getAuthor().trim().isEmpty()) {
                info.author = docInfo.getAuthor().trim();
            }

            // 3. 尝试提取摘要（仅限第一页）
            if (info.pages > 0) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(1);
                stripper.setEndPage(1);
                String firstPageText = stripper.getText(document);

                // 移除换行符导致的单词中断
                firstPageText = firstPageText.replaceAll("-\\n", "");

                Matcher matcher = ABSTRACT_PATTERN.matcher(firstPageText);
                if (matcher.find()) {
                    String extractedAbstract = matcher.group(2);
                    if (extractedAbstract != null) {
                        // 清理摘要文本，替换多个换行符为空格
                        info.abstractText = extractedAbstract.trim().replaceAll("\\s*\\n\\s*", " ");
                        // 截断摘要以防过长
                        if (info.abstractText.length() > 500) {
                            info.abstractText = info.abstractText.substring(0, 500) + "...";
                        }
                    }
                }
            }
            info.parseSuccess = true;
        }
        return info;
    }


    @Override
    public ProgressUpdateResponse updateProgress(String documentId, ProgressUpdateRequest request) {
        String userId = UserContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        log.info("更新阅读进度，userId: {}, documentId: {}, progress: {}",
                userId, documentId, request.getProgress());

        // 检查文档是否存在且属于当前用户
        if (!documentManager.belongsToUser(documentId, userId)) {
            throw new BusinessException(404, "文档不存在或无权访问");
        }

        // 更新进度
        boolean success = documentManager.updateReadProgress(documentId, request.getProgress());
        if (!success) {
            throw new BusinessException(500, "更新进度失败");
        }

        return ProgressUpdateResponse.builder()
                .progress(request.getProgress())
                .build();
    }

    @Override
    @Transactional
    public void deleteDocument(String documentId) {
        String userId = UserContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        log.info("删除文档，userId: {}, documentId: {}", userId, documentId);

        Document document = documentManager.getByDocumentId(documentId);
        if (document == null) {
            throw new BusinessException(404, "文档不存在");
        }

        // 检查文档是否属于当前用户
        if (!userId.equals(document.getUserId())) {
            throw new BusinessException(403, "无权删除该文档");
        }

        try {
            // 删除文档记录
            boolean success = documentManager.deleteDocument(documentId);
            if (!success) {
                throw new BusinessException(500, "删除文档记录失败");
            }

            // 删除文件存储
            if (document.getFilePath() != null) {
                boolean fileDeleted = fileStorageUtil.deleteDocument(document.getFilePath());
                if (!fileDeleted) {
                    log.warn("删除文档文件失败，documentId: {}, fileUrl: {}", documentId, document.getFilePath());
                }
            }

            // TODO: 删除相关的文档分片数据和向量索引

            log.info("文档删除成功，documentId: {}", documentId);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除文档失败，documentId: {}", documentId, e);
            throw new BusinessException(500, "删除文档失败：" + e.getMessage());
        }
    }

    /**
     * 根据分类id检查是否存在文档
     * @param categoryId 分类ID
     * @return
     */
    @Override
    public Boolean hasDocumentByCategoryId(String categoryId) {
        return documentMapper.exists(
                new LambdaQueryWrapper<Document>()
                        .eq(Document::getCategoryId, categoryId)
                        .last("limit 1")
        );
    }

    /**
     * 校验上传文件
     */
    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new BusinessException(400, "文件格式不支持，仅支持PDF格式");
        }

        // 检查文件大小（50MB）
        long maxSize = 50 * 1024 * 1024L;
        if (file.getSize() > maxSize) {
            throw new BusinessException(400, "文件大小不能超过50MB");
        }
    }

    /**
     * 获取不带扩展名的文件名
     */
    private String getFileNameWithoutExtension(String filename) {
        if (filename == null) {
            return "未知文档";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(0, lastDotIndex) : filename;
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        }
    }

    /**
     * 转换为列表响应DTO
     */
    private DocumentListResponse convertToListResponse(Document document) {
        return DocumentListResponse.builder()
                .id(document.getDocumentId())
                .title(document.getTitle())
                .filename(document.getFilename())
                .categoryId(document.getCategoryId())
                .author(document.getAuthor())
                .uploadDate(document.getUploadDate())
                .fileSize(document.getFileSizeDisplay())
                .pages(document.getPages())
                .status(document.getStatus())
                .abstractText(document.getAbstractText())
                .tags(document.getTags())
                .readProgress(document.getReadProgress())
                .build();
    }

    /**
     * 转换为详情响应DTO
     */
    private DocumentDetailResponse convertToDetailResponse(Document document) {
        // 不在服务层填充绝对路径与下载URL，由控制器统一生成对前端友好的 filePath

        return DocumentDetailResponse.builder()
                .id(document.getDocumentId())
                .title(document.getTitle())
                .filename(document.getFilename())
                .categoryId(document.getCategoryId())
                .userId(document.getUserId())
                .author(document.getAuthor())
                .uploadDate(document.getUploadDate())
                .processedAt(document.getProcessedAt())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .fileSize(document.getFileSizeDisplay())
                .fileSizeBytes(document.getFileSize())
                .pages(document.getPages())
                .status(document.getStatus())
                .abstractText(document.getAbstractText())
                .tags(document.getTags())
                .readProgress(document.getReadProgress())
                .filePath(document.getFilePath())
                .build();
    }
}