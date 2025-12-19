package com.zd.scnoteservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zd.scnoteservice.model.domain.Note;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 笔记数据访问接口
 * 
 * <p>提供笔记表的基础CRUD操作</p>
 * <p>继承MyBatis-Plus的BaseMapper，获得基础的增删改查功能</p>
 * <p>支持笔记的创建、查询、更新、删除等操作</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Mapper
public interface NoteMapper extends BaseMapper<Note> {

    /**
     * 批量插入笔记
     * 
     * <p>使用MyBatis-Plus的批量操作优化性能</p>
     * <p>适用于批量导入笔记数据的场景</p>
     * 
     * @param entities 笔记实体列表
     * @return 影响行数
     */
    int batchInsert(@Param("entities") List<Note> entities);

    /**
     * 根据笔记ID查询笔记
     * 
     * <p>通过笔记的唯一标识查询笔记详情</p>
     * 
     * @param noteId 笔记唯一标识，格式：note_xxxxxxxxx
     * @return 笔记实体，如果不存在返回null
     */
    Note selectByNoteId(@Param("noteId") String noteId);

    /**
     * 根据用户ID查询笔记列表
     * 
     * <p>查询指定用户创建的所有笔记</p>
     * <p>按创建时间倒序排列</p>
     * 
     * @param userId 用户ID
     * @return 笔记实体列表
     */
    List<Note> selectByUserId(@Param("userId") String userId);

    /**
     * 根据文档ID查询笔记列表
     * 
     * <p>查询指定文档下的所有笔记</p>
     * <p>按页码和创建时间排序</p>
     * 
     * @param documentId 文档ID
     * @return 笔记实体列表
     */
    List<Note> selectByDocumentId(@Param("documentId") String documentId);

    /**
     * 根据用户ID和文档ID查询笔记列表
     * 
     * <p>查询指定用户在指定文档下创建的笔记</p>
     * <p>用于文档阅读页面显示当前用户的笔记</p>
     * 
     * @param userId 用户ID
     * @param documentId 文档ID
     * @return 笔记实体列表
     */
    List<Note> selectByUserIdAndDocumentId(@Param("userId") String userId, @Param("documentId") String documentId);

    /**
     * 根据用户ID和页码查询笔记列表
     * 
     * <p>查询指定用户在指定页码的笔记</p>
     * <p>用于按页码筛选笔记</p>
     * 
     * @param userId 用户ID
     * @param documentId 文档ID
     * @param pageNumber 页码
     * @return 笔记实体列表
     */
    List<Note> selectByUserIdAndDocumentIdAndPageNumber(@Param("userId") String userId, 
                                                        @Param("documentId") String documentId, 
                                                        @Param("pageNumber") Integer pageNumber);

    /**
     * 根据用户ID分页查询笔记列表
     * 
     * <p>支持分页查询用户的笔记列表</p>
     * <p>按创建时间倒序排列</p>
     * 
     * @param userId 用户ID
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 笔记实体列表
     */
    List<Note> selectByUserIdWithPage(@Param("userId") String userId, 
                                      @Param("offset") Integer offset, 
                                      @Param("limit") Integer limit);

    /**
     * 根据文档ID分页查询笔记列表
     * 
     * <p>支持分页查询文档的笔记列表</p>
     * <p>按页码和创建时间排序</p>
     * 
     * @param documentId 文档ID
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 笔记实体列表
     */
    List<Note> selectByDocumentIdWithPage(@Param("documentId") String documentId, 
                                          @Param("offset") Integer offset, 
                                          @Param("limit") Integer limit);

    /**
     * 根据用户ID和文档ID分页查询笔记列表
     * 
     * <p>支持分页查询指定用户在指定文档下的笔记</p>
     * <p>用于文档阅读页面的笔记分页显示</p>
     * 
     * @param userId 用户ID
     * @param documentId 文档ID，可为null表示查询所有文档的笔记
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 笔记实体列表
     */
    List<Note> selectByUserIdAndDocumentIdWithPage(@Param("userId") String userId, 
                                                   @Param("documentId") String documentId, 
                                                   @Param("offset") Integer offset, 
                                                   @Param("limit") Integer limit);

    /**
     * 统计用户笔记总数
     * 
     * <p>统计指定用户创建的笔记总数</p>
     * 
     * @param userId 用户ID
     * @return 笔记总数
     */
    long countByUserId(@Param("userId") String userId);

    /**
     * 统计文档笔记总数
     * 
     * <p>统计指定文档下的笔记总数</p>
     * 
     * @param documentId 文档ID
     * @return 笔记总数
     */
    long countByDocumentId(@Param("documentId") String documentId);

    /**
     * 统计用户在指定文档下的笔记总数
     * 
     * <p>统计指定用户在指定文档下创建的笔记总数</p>
     * 
     * @param userId 用户ID
     * @param documentId 文档ID，可为null表示统计所有文档的笔记
     * @return 笔记总数
     */
    long countByUserIdAndDocumentId(@Param("userId") String userId, @Param("documentId") String documentId);

    /**
     * 根据用户ID查询收藏的笔记列表
     * 
     * <p>查询指定用户收藏的笔记</p>
     * <p>按创建时间倒序排列</p>
     * 
     * @param userId 用户ID
     * @return 收藏的笔记实体列表
     */
    List<Note> selectFavoritesByUserId(@Param("userId") String userId);

    /**
     * 根据标签查询笔记列表
     * 
     * <p>查询包含指定标签的笔记</p>
     * <p>支持模糊匹配标签</p>
     * 
     * @param userId 用户ID
     * @param tag 标签名称
     * @return 笔记实体列表
     */
    List<Note> selectByUserIdAndTag(@Param("userId") String userId, @Param("tag") String tag);

    /**
     * 根据笔记ID更新笔记信息
     * 
     * <p>根据笔记ID更新笔记的标题、内容等信息</p>
     * <p>自动更新updated_at字段</p>
     * 
     * @param note 笔记实体，包含要更新的字段
     * @return 影响行数
     */
    int updateByNoteId(@Param("note") Note note);

    /**
     * 根据笔记ID删除笔记
     * 
     * <p>物理删除指定的笔记记录</p>
     * 
     * @param noteId 笔记唯一标识
     * @return 影响行数
     */
    int deleteByNoteId(@Param("noteId") String noteId);

    /**
     * 根据文档ID批量删除笔记
     * 
     * <p>删除指定文档下的所有笔记</p>
     * <p>用于文档删除时的级联删除操作</p>
     * 
     * @param documentId 文档ID
     * @return 影响行数
     */
    int deleteByDocumentId(@Param("documentId") String documentId);

    /**
     * 根据用户ID批量删除笔记
     * 
     * <p>删除指定用户的所有笔记</p>
     * <p>用于用户注销时的数据清理</p>
     * 
     * @param userId 用户ID
     * @return 影响行数
     */
    int deleteByUserId(@Param("userId") String userId);
}