package com.zd.sccategoriesmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zd.sccategoriesmanage.model.domain.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 分类数据访问接口
 * 
 * <p>提供分类表的基础CRUD操作</p>
 * <p>继承MyBatis-Plus的BaseMapper，获得基础的增删改查功能</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * 批量插入分类
     * 
     * <p>使用MyBatis-Plus的批量操作优化性能</p>
     * 
     * @param entities 分类实体列表
     * @return 影响行数
     */
    int batchInsert(@Param("entities") List<Category> entities);
    /**
     * 根据分类ID查询分类
     * 
     * @param categoryId 分类唯一标识
     * @return 分类实体
     */
    Category selectByCategoryId(@Param("categoryId") String categoryId);

    /**
     * 根据用户ID查询所有分类
     * 
     * @param userId 用户ID
     * @return 分类实体列表
     */
    List<Category> selectByUserId(@Param("userId") String userId);

    /**
     * 根据父分类ID查询子分类
     * 
     * @param parentId 父分类ID
     * @return 分类实体列表
     */
    List<Category> selectByParentId(@Param("parentId") String parentId);

    /**
     * 根据用户ID和父分类ID查询子分类
     * 
     * @param userId 用户ID
     * @param parentId 父分类ID
     * @return 分类实体列表
     */
    List<Category> selectByUserIdAndParentId(@Param("userId") String userId, @Param("parentId") String parentId);

    /**
     * 更新分类文档数量
     * 
     * @param categoryId 分类ID
     * @param documentCount 文档数量
     * @return 影响行数
     */
    int updateDocumentCount(@Param("categoryId") String categoryId, @Param("documentCount") Integer documentCount);

    /**
     * 检查分类名称在同一父级下是否存在
     * 
     * @param userId 用户ID
     * @param parentId 父分类ID
     * @param name 分类名称
     * @return 存在数量
     */
    long countByUserIdAndParentIdAndName(@Param("userId") String userId, 
                                        @Param("parentId") String parentId, 
                                        @Param("name") String name);

    /**
     * 检查分类名称在同一父级下是否存在（排除指定分类）
     * 
     * @param userId 用户ID
     * @param parentId 父分类ID
     * @param name 分类名称
     * @param excludeCategoryId 要排除的分类ID
     * @return 存在数量
     */
    long countByUserIdAndParentIdAndNameExcludeId(@Param("userId") String userId,
                                                 @Param("parentId") String parentId,
                                                 @Param("name") String name,
                                                 @Param("excludeCategoryId") String excludeCategoryId);

    /**
     * 根据分类ID删除分类
     * 
     * @param categoryId 分类ID
     * @return 删除行数
     */
    int deleteByCategoryId(@Param("categoryId") String categoryId);

    /**
     * 根据分类ID更新分类信息
     * 
     * @param category 分类信息
     * @return 更新行数
     */
    int updateByCategoryId(@Param("category") Category category);

    // ==================== API接口所需方法 ====================

    /**
     * 查询所有分类
     * 
     * @return 分类实体列表
     */
    List<Category> selectAll();

    /**
     * 检查分类名称在同一父级下是否存在（不限用户）
     * 
     * @param parentId 父分类ID
     * @param name 分类名称
     * @return 存在数量
     */
    long countByParentIdAndName(@Param("parentId") String parentId, @Param("name") String name);

    /**
     * 检查分类名称在同一父级下是否存在（排除指定分类，不限用户）
     * 
     * @param parentId 父分类ID
     * @param name 分类名称
     * @param excludeCategoryId 要排除的分类ID
     * @return 存在数量
     */
    long countByParentIdAndNameExcludeId(@Param("parentId") String parentId,
                                        @Param("name") String name,
                                        @Param("excludeCategoryId") String excludeCategoryId);

    /**
     * 统计指定父分类下的子分类数量
     * 
     * @param parentId 父分类ID
     * @return 子分类数量
     */
    long countByParentId(@Param("parentId") String parentId);
}