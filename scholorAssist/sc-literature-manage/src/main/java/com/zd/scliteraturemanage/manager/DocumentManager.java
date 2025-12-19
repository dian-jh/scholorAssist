package com.zd.scliteraturemanage.manager;

import com.zd.scliteraturemanage.model.domain.Document;

import java.math.BigDecimal;
import java.util.List;

/**
 * 文档数据管理接口
 * 
 * <p>提供文档数据的原子操作，封装对Mapper和缓存的访问</p>
 * 
 * @author System
 * @since 2024-01-21
 */
public interface DocumentManager {

    /**
     * 创建文档
     * 
     * @param document 文档实体
     * @return 创建成功的文档实体
     */
    Document createDocument(Document document);

    /**
     * 根据文档ID获取文档
     * 
     * @param documentId 文档ID
     * @return 文档实体，不存在返回null
     */
    Document getByDocumentId(String documentId);

    /**
     * 根据用户ID获取文档列表
     * 
     * @param userId 用户ID
     * @return 文档列表
     */
    List<Document> getByUserId(String userId);

    /**
     * 根据用户ID和分类ID获取文档列表
     * 
     * @param userId 用户ID
     * @param categoryId 分类ID
     * @return 文档列表
     */
    List<Document> getByUserIdAndCategoryId(String userId, String categoryId);

    /**
     * 搜索文档
     * 
     * @param userId 用户ID
     * @param keyword 搜索关键词
     * @return 文档列表
     */
    List<Document> searchDocuments(String userId, String keyword);

    /**
     * 更新文档状态
     * 
     * @param documentId 文档ID
     * @param status 新状态
     * @return 是否更新成功
     */
    boolean updateStatus(String documentId, String status);

    /**
     * 更新阅读进度
     * 
     * @param documentId 文档ID
     * @param readProgress 阅读进度
     * @return 是否更新成功
     */
    boolean updateReadProgress(String documentId, BigDecimal readProgress);

    /**
     * 删除文档
     * 
     * @param documentId 文档ID
     * @return 是否删除成功
     */
    boolean deleteDocument(String documentId);

    /**
     * 检查文档是否存在
     * 
     * @param documentId 文档ID
     * @return 是否存在
     */
    boolean existsByDocumentId(String documentId);

    /**
     * 检查文档是否属于指定用户
     * 
     * @param documentId 文档ID
     * @param userId 用户ID
     * @return 是否属于该用户
     */
    boolean belongsToUser(String documentId, String userId);
}