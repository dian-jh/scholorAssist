package com.zd.sccategoriesmanage.manager.impl;

import com.zd.scapi.api.literature.DocumentApi;
import com.zd.sccategoriesmanage.manager.CategoryManager;
import com.zd.sccategoriesmanage.mapper.CategoryMapper;
import com.zd.sccategoriesmanage.model.domain.Category;
import com.zd.sccommon.utils.UserContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类数据管理实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryManagerImpl implements CategoryManager {

    private final CategoryMapper categoryMapper;
    private final DocumentApi documentApi;

    @Override
    public Category create(Category category) {
        log.info("创建分类，分类ID：{}，名称：{}", category.getCategoryId(), category.getName());
        
        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        category.setCreatedAt(now);
        category.setUpdatedAt(now);
        
        // 设置默认文档数量
        if (category.getDocumentCount() == null) {
            category.setDocumentCount(0);
        }
        
        // 设置默认排序顺序
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        
        int result = categoryMapper.insert(category);
        if (result > 0) {
            log.info("分类创建成功，分类ID：{}", category.getCategoryId());
            return category;
        } else {
            log.error("分类创建失败，分类ID：{}", category.getCategoryId());
            return null;
        }
    }

    @Override
    public Category getByCategoryId(String categoryId) {
        log.debug("根据分类ID获取分类信息，分类ID：{}", categoryId);
        return categoryMapper.selectByCategoryId(categoryId);
    }

    @Override
    public List<Category> getByUserId(String userId) {
        log.debug("根据用户ID获取所有分类，用户ID：{}", userId);
        return categoryMapper.selectByUserId(userId);
    }

    @Override
    public List<Category> getByUserIdAndParentId(String userId, String parentId) {
        log.debug("根据用户ID和父分类ID获取子分类，用户ID：{}，父分类ID：{}", userId, parentId);
        return categoryMapper.selectByUserIdAndParentId(userId, parentId);
    }

    @Override
    public boolean updateByCategoryId(Category category) {
        log.info("更新分类信息，分类ID：{}，名称：{}", category.getCategoryId(), category.getName());
        
        // 设置更新时间
        category.setUpdatedAt(LocalDateTime.now());
        
        int result = categoryMapper.updateByCategoryId(category);
        if (result > 0) {
            log.info("分类更新成功，分类ID：{}", category.getCategoryId());
            return true;
        } else {
            log.warn("分类更新失败，分类ID：{}", category.getCategoryId());
            return false;
        }
    }

    @Override
    public boolean deleteByCategoryId(String categoryId) {
        log.info("删除分类，分类ID：{}", categoryId);
        
        int result = categoryMapper.deleteByCategoryId(categoryId);
        if (result > 0) {
            log.info("分类删除成功，分类ID：{}", categoryId);
            return true;
        } else {
            log.warn("分类删除失败，分类ID：{}", categoryId);
            return false;
        }
    }

    @Override
    public boolean existsByUserIdAndParentIdAndName(String userId, String parentId, String name) {
        log.debug("检查分类名称是否存在，用户ID：{}，父分类ID：{}，名称：{}", userId, parentId, name);
        long count = categoryMapper.countByUserIdAndParentIdAndName(userId, parentId, name);
        return count > 0;
    }

    @Override
    public boolean existsByUserIdAndParentIdAndNameExcludeId(String userId, String parentId, 
                                                           String name, String excludeCategoryId) {
        log.debug("检查分类名称是否存在（排除指定分类），用户ID：{}，父分类ID：{}，名称：{}，排除分类ID：{}", 
                 userId, parentId, name, excludeCategoryId);
        long count = categoryMapper.countByUserIdAndParentIdAndNameExcludeId(userId, parentId, name, excludeCategoryId);
        return count > 0;
    }

    @Override
    public boolean updateDocumentCount(String categoryId, Integer documentCount) {
        log.info("更新分类文档数量，分类ID：{}，文档数量：{}", categoryId, documentCount);
        
        int result = categoryMapper.updateDocumentCount(categoryId, documentCount);
        if (result > 0) {
            log.info("分类文档数量更新成功，分类ID：{}", categoryId);
            return true;
        } else {
            log.warn("分类文档数量更新失败，分类ID：{}", categoryId);
            return false;
        }
    }

    // ==================== API接口所需方法实现 ====================

    @Override
    public List<Category> getAllCategories() {
        log.debug("获取所有分类");
        String userId = UserContextUtil.getCurrentUserId();
        return categoryMapper.selectByUserId(userId);
    }

    @Override
    public boolean existsByParentIdAndName(String parentId, String name) {
        log.debug("检查分类名称是否存在，父分类ID：{}，名称：{}", parentId, name);
        long count = categoryMapper.countByParentIdAndName(parentId, name);
        return count > 0;
    }

    @Override
    public boolean existsByParentIdAndNameExcludeId(String parentId, String name, String excludeCategoryId) {
        log.debug("检查分类名称是否存在（排除指定分类），父分类ID：{}，名称：{}，排除分类ID：{}", 
                 parentId, name, excludeCategoryId);
        long count = categoryMapper.countByParentIdAndNameExcludeId(parentId, name, excludeCategoryId);
        return count > 0;
    }

    @Override
    public Category update(Category category) {
        log.info("更新分类信息，分类ID：{}，名称：{}", category.getCategoryId(), category.getName());
        
        // 设置更新时间
        category.setUpdatedAt(LocalDateTime.now());
        
        int result = categoryMapper.updateByCategoryId(category);
        if (result > 0) {
            log.info("分类更新成功，分类ID：{}", category.getCategoryId());
            return category;
        } else {
            log.warn("分类更新失败，分类ID：{}", category.getCategoryId());
            return null;
        }
    }

    @Override
    public boolean delete(String categoryId) {
        log.info("删除分类，分类ID：{}", categoryId);
        
        int result = categoryMapper.deleteByCategoryId(categoryId);
        if (result > 0) {
            log.info("分类删除成功，分类ID：{}", categoryId);
            return true;
        } else {
            log.warn("分类删除失败，分类ID：{}", categoryId);
            return false;
        }
    }

    @Override
    public boolean hasChildren(String categoryId) {
        log.debug("检查分类是否有子分类，分类ID：{}", categoryId);
        long count = categoryMapper.countByParentId(categoryId);
        return count > 0;
    }

    @Override
    public boolean hasDocuments(String categoryId) {
        log.debug("检查分类是否有关联文档，分类ID：{}", categoryId);
        // 这里需要调用文档服务或者查询文档表，暂时返回false
        // TODO: 实现文档关联检查逻辑
        return documentApi.hasDocumentByCategoryId(categoryId);
    }
}