package com.zd.scliteraturemanage.service;

import com.zd.scliteraturemanage.model.dto.request.ProgressUpdateRequest;
import com.zd.scliteraturemanage.model.dto.response.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档业务服务接口
 * 
 * <p>提供文档管理的业务逻辑，包括文档上传、查询、更新等功能</p>
 * 
 * @author System
 * @since 2024-01-21
 */
public interface DocumentService {

    /**
     * 获取文档列表
     * 
     * @param categoryId 分类ID，"all"表示获取全部
     * @param search 搜索关键词
     * @param page 页码，从1开始
     * @param pageSize 每页数量
     * @return 文档列表
     */
    List<DocumentListResponse> getDocuments(String categoryId, String search, Integer page, Integer pageSize);

    /**
     * 获取文档详情
     * 
     * @param documentId 文档ID
     * @return 文档详情
     */
    DocumentDetailResponse getDocumentDetail(String documentId);

    /**
     * 上传文档
     * 
     * @param file PDF文件
     * @param title 文档标题，可选
     * @param categoryId 分类ID，可选
     * @return 上传结果
     */
    DocumentUploadResponse uploadDocument(MultipartFile file, String title, String categoryId);

    /**
     * 更新阅读进度
     * 
     * @param documentId 文档ID
     * @param request 进度更新请求
     * @return 更新结果
     */
    ProgressUpdateResponse updateProgress(String documentId, ProgressUpdateRequest request);

    /**
     * 删除文档
     * 
     * @param documentId 文档ID
     */
    void deleteDocument(String documentId);

    /**
     * 根据分类ID检查是否存在文档
     *
     * @param categoryId 分类ID
     * @return 是否存在文档
     */
    Boolean hasDocumentByCategoryId(String categoryId);
}