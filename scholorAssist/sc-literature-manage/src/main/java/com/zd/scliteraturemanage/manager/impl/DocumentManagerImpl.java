package com.zd.scliteraturemanage.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zd.scliteraturemanage.manager.DocumentManager;
import com.zd.scliteraturemanage.mapper.DocumentMapper;
import com.zd.scliteraturemanage.model.domain.Document;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档数据管理实现类
 * 
 * <p>实现文档数据的原子操作，封装对Mapper和缓存的访问</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Component
@RequiredArgsConstructor
public class DocumentManagerImpl implements DocumentManager {

    private static final Logger log = LoggerFactory.getLogger(DocumentManagerImpl.class);

    private final DocumentMapper documentMapper;

    @Override
    public Document createDocument(Document document) {
        document.setId(null); // 让数据库自动生成ID
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        log.info("创建文档，documentId: {}", document.getDocumentId());
        documentMapper.insert(document);
        return document;
    }

    @Override
    public Document getByDocumentId(String documentId) {
        log.debug("根据文档ID查询文档，documentId: {}", documentId);
        return documentMapper.selectByDocumentId(documentId);
    }

    @Override
    public List<Document> getByUserId(String userId) {
        log.debug("根据用户ID查询文档列表，userId: {}", userId);
        return documentMapper.selectByUserId(userId);
    }

    @Override
    public List<Document> getByUserIdAndCategoryId(String userId, String categoryId) {
        log.debug("根据用户ID和分类ID查询文档列表，userId: {}, categoryId: {}", userId, categoryId);
        if ("all".equals(categoryId)) {
            return getByUserId(userId);
        }
        return documentMapper.selectByUserIdAndCategoryId(userId, categoryId);
    }

    @Override
    public List<Document> searchDocuments(String userId, String keyword) {
        log.debug("搜索文档，userId: {}, keyword: {}", userId, keyword);
        return documentMapper.searchDocuments(userId, keyword);
    }

    @Override
    public boolean updateStatus(String documentId, String status) {
        log.info("更新文档状态，documentId: {}, status: {}", documentId, status);
        int result = documentMapper.updateStatus(documentId, status);
        return result > 0;
    }

    @Override
    public boolean updateReadProgress(String documentId, BigDecimal readProgress) {
        log.info("更新阅读进度，documentId: {}, progress: {}", documentId, readProgress);
        int result = documentMapper.updateReadProgress(documentId, readProgress);
        return result > 0;
    }

    @Override
    public boolean deleteDocument(String documentId) {
        log.info("删除文档，documentId: {}", documentId);
        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Document::getDocumentId, documentId);
        int result = documentMapper.delete(queryWrapper);
        return result > 0;
    }

    @Override
    public boolean existsByDocumentId(String documentId) {
        log.debug("检查文档是否存在，documentId: {}", documentId);
        Document document = getByDocumentId(documentId);
        return document != null;
    }

    @Override
    public boolean belongsToUser(String documentId, String userId) {
        log.debug("检查文档是否属于用户，documentId: {}, userId: {}", documentId, userId);
        Document document = getByDocumentId(documentId);
        return document != null && userId.equals(document.getUserId());
    }
}