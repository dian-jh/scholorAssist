package com.zd.scliteraturemanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zd.scliteraturemanage.model.domain.Document;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * 文档数据访问接口
 * 
 * <p>提供文档表的基础CRUD操作</p>
 * <p>继承MyBatis-Plus的BaseMapper，获得基础的增删改查功能</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {

    /**
     * 批量插入文档
     * 
     * <p>使用MyBatis-Plus的批量操作优化性能</p>
     * 
     * @param entities 文档实体列表
     * @return 影响行数
     */
    int batchInsert(@Param("entities") List<Document> entities);

    /**
     * 根据文档ID查询文档
     * 
     * @param documentId 文档唯一标识
     * @return 文档实体
     */
    Document selectByDocumentId(@Param("documentId") String documentId);

    /**
     * 根据用户ID查询文档列表
     * 
     * @param userId 用户ID
     * @return 文档实体列表
     */
    List<Document> selectByUserId(@Param("userId") String userId);

    /**
     * 根据分类ID查询文档列表
     * 
     * @param categoryId 分类ID
     * @return 文档实体列表
     */
    List<Document> selectByCategoryId(@Param("categoryId") String categoryId);

    /**
     * 根据用户ID和分类ID查询文档列表
     * 
     * @param userId 用户ID
     * @param categoryId 分类ID
     * @return 文档实体列表
     */
    List<Document> selectByUserIdAndCategoryId(@Param("userId") String userId, @Param("categoryId") String categoryId);

    /**
     * 根据状态查询文档列表
     * 
     * @param status 处理状态
     * @return 文档实体列表
     */
    List<Document> selectByStatus(@Param("status") String status);

    /**
     * 更新文档处理状态
     * 
     * @param documentId 文档ID
     * @param status 处理状态
     * @return 影响行数
     */
    int updateStatus(@Param("documentId") String documentId, @Param("status") String status);

    /**
     * 更新文档阅读进度
     * 
     * @param documentId 文档ID
     * @param readProgress 阅读进度
     * @return 影响行数
     */
    int updateReadProgress(@Param("documentId") String documentId, @Param("readProgress") BigDecimal readProgress);

    /**
     * 根据分类ID统计文档数量
     * 
     * @param categoryId 分类ID
     * @return 文档数量
     */
    long countByCategoryId(@Param("categoryId") String categoryId);

    /**
     * 根据用户ID统计文档数量
     * 
     * @param userId 用户ID
     * @return 文档数量
     */
    long countByUserId(@Param("userId") String userId);

    /**
     * 全文搜索文档
     * 
     * @param userId 用户ID
     * @param keyword 搜索关键词
     * @return 文档实体列表
     */
    List<Document> searchDocuments(@Param("userId") String userId, @Param("keyword") String keyword);
}