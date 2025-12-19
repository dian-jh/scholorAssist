package com.zd.scliteraturemanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zd.scliteraturemanage.model.domain.DocumentChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 文档分片数据访问接口
 * 
 * <p>提供文档分片表的基础CRUD操作</p>
 * <p>继承MyBatis-Plus的BaseMapper，获得基础的增删改查功能</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {

    /**
     * 批量插入文档分片
     * 
     * <p>使用MyBatis-Plus的批量操作优化性能</p>
     * 
     * @param entities 文档分片实体列表
     * @return 影响行数
     */
    int batchInsert(@Param("entities") List<DocumentChunk> entities);



    /**
     * 根据分片ID查询文档分片
     * 
     * @param chunkId 分片唯一标识
     * @return 文档分片实体
     */
    DocumentChunk selectByChunkId(@Param("chunkId") String chunkId);

    /**
     * 根据文档ID查询所有分片
     * 
     * @param documentId 文档ID
     * @return 文档分片实体列表
     */
    List<DocumentChunk> selectByDocumentId(@Param("documentId") String documentId);

    /**
     * 根据文档ID和页码查询分片
     * 
     * @param documentId 文档ID
     * @param pageNumber 页码
     * @return 文档分片实体列表
     */
    List<DocumentChunk> selectByDocumentIdAndPageNumber(@Param("documentId") String documentId, 
                                                       @Param("pageNumber") Integer pageNumber);

    /**
     * 根据文档ID删除所有分片
     * 
     * @param documentId 文档ID
     * @return 影响行数
     */
    int deleteByDocumentId(@Param("documentId") String documentId);

    /**
     * 根据文档ID统计分片数量
     * 
     * @param documentId 文档ID
     * @return 分片数量
     */
    long countByDocumentId(@Param("documentId") String documentId);

    /**
     * 向量相似度搜索
     * 
     * @param embeddingVector 查询向量
     * @param limit 返回数量限制
     * @return 相似文档分片列表
     */
    List<DocumentChunk> vectorSimilaritySearch(@Param("embeddingVector") String embeddingVector, 
                                              @Param("limit") Integer limit);
}