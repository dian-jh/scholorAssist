package com.zd.sccategoriesmanage.manager;

import com.zd.sccategoriesmanage.model.domain.Category;

import java.util.List;

/**
 * 分类数据管理接口
 * 
 * @author system
 * @since 2024-01-01
 */
public interface CategoryManager {

    /**
     * 创建分类
     * 
     * @param category 分类信息
     * @return 创建的分类
     */
    Category create(Category category);

    /**
     * 根据分类ID获取分类信息
     * 
     * @param categoryId 分类ID
     * @return 分类信息
     */
    Category getByCategoryId(String categoryId);

    /**
     * 根据用户ID获取所有分类
     * 
     * @param userId 用户ID
     * @return 分类列表
     */
    List<Category> getByUserId(String userId);

    /**
     * 根据用户ID和父分类ID获取子分类
     * 
     * @param userId 用户ID
     * @param parentId 父分类ID，为null时获取根分类
     * @return 子分类列表
     */
    List<Category> getByUserIdAndParentId(String userId, String parentId);

    /**
     * 更新分类信息
     * 
     * @param category 分类信息
     * @return 是否更新成功
     */
    boolean updateByCategoryId(Category category);

    /**
     * 删除分类
     * 
     * @param categoryId 分类ID
     * @return 是否删除成功
     */
    boolean deleteByCategoryId(String categoryId);

    /**
     * 检查分类名称在同一父级下是否存在
     * 
     * @param userId 用户ID
     * @param parentId 父分类ID
     * @param name 分类名称
     * @return 是否存在
     */
    boolean existsByUserIdAndParentIdAndName(String userId, String parentId, String name);

    /**
     * 检查分类名称在同一父级下是否存在（排除指定分类）
     * 
     * @param userId 用户ID
     * @param parentId 父分类ID
     * @param name 分类名称
     * @param excludeCategoryId 要排除的分类ID
     * @return 是否存在
     */
    boolean existsByUserIdAndParentIdAndNameExcludeId(String userId, String parentId, 
                                                     String name, String excludeCategoryId);

    /**
     * 更新分类文档数量
     * 
     * @param categoryId 分类ID
     * @param documentCount 文档数量
     * @return 是否更新成功
     */
    boolean updateDocumentCount(String categoryId, Integer documentCount);

    // ==================== API接口所需方法 ====================

    /**
     * 获取所有分类
     * 
     * @return 分类列表
     */
    List<Category> getAllCategories();

    /**
     * 检查分类名称在同一父级下是否存在（不限用户）
     * 
     * @param parentId 父分类ID
     * @param name 分类名称
     * @return 是否存在
     */
    boolean existsByParentIdAndName(String parentId, String name);

    /**
     * 检查分类名称在同一父级下是否存在（排除指定分类，不限用户）
     * 
     * @param parentId 父分类ID
     * @param name 分类名称
     * @param excludeCategoryId 要排除的分类ID
     * @return 是否存在
     */
    boolean existsByParentIdAndNameExcludeId(String parentId, String name, String excludeCategoryId);

    /**
     * 更新分类信息
     * 
     * @param category 分类信息
     * @return 更新后的分类
     */
    Category update(Category category);

    /**
     * 删除分类
     * 
     * @param categoryId 分类ID
     * @return 是否删除成功
     */
    boolean delete(String categoryId);

    /**
     * 检查分类是否有子分类
     * 
     * @param categoryId 分类ID
     * @return 是否有子分类
     */
    boolean hasChildren(String categoryId);

    /**
     * 检查分类是否有关联文档
     * 
     * @param categoryId 分类ID
     * @return 是否有关联文档
     */
    boolean hasDocuments(String categoryId);
}