package com.zd.sccategoriesmanage.service;

import com.zd.sccategoriesmanage.model.dto.request.CategoryCreateRequest;
import com.zd.sccategoriesmanage.model.dto.request.CategoryUpdateRequest;
import com.zd.sccategoriesmanage.model.dto.response.CategoryResponse;
import com.zd.sccategoriesmanage.model.dto.response.CategoryTreeApiResponse;
import com.zd.sccategoriesmanage.model.dto.response.CategoryTreeResponse;
import com.zd.sccategoriesmanage.model.dto.response.CategoryApiResponse;

import java.util.List;

/**
 * 分类业务服务接口
 * 
 * @author system
 * @since 2024-01-01
 */
public interface CategoryService {

    // ========== 原有方法（保持向后兼容） ==========

    /**
     * 创建分类
     * 
     * @param userId 用户ID
     * @param request 创建请求
     * @return 分类信息
     */
    CategoryResponse createCategory(String userId, CategoryCreateRequest request);

    /**
     * 更新分类
     * 
     * @param userId 用户ID
     * @param categoryId 分类ID
     * @param request 更新请求
     * @return 分类信息
     */
    CategoryResponse updateCategory(String userId, String categoryId, CategoryUpdateRequest request);

    /**
     * 删除分类
     * 
     * @param userId 用户ID
     * @param categoryId 分类ID
     */
    void deleteCategory(String userId, String categoryId);

    /**
     * 获取用户分类树
     * 
     * @param userId 用户ID
     * @return 分类树列表
     */
    List<CategoryTreeResponse> getCategoryTree(String userId);

    // ========== 新增API方法（严格遵循API文档规范） ==========

    /**
     * 获取分类列表 - API规范方法
     *
     * <p>获取所有文档分类，包含层级结构</p>
     * <p>对应API: GET /api/categories</p>
     *
     * @return 分类树列表
     */
    List<CategoryTreeApiResponse> getCategoriesApi();

    /**
     * 创建分类 - API规范方法
     * 
     * <p>创建新的文档分类，支持创建根分类和子分类</p>
     * <p>对应API: POST /api/categories</p>
     * 
     * @param request 创建请求
     * @return 分类信息
     */
    CategoryApiResponse createCategoryApi(CategoryCreateRequest request);

    /**
     * 更新分类 - API规范方法
     * 
     * <p>更新指定分类的名称或父分类</p>
     * <p>对应API: POST /api/categories/{id}</p>
     * 
     * @param id 分类ID
     * @param request 更新请求
     * @return 分类信息
     */
    CategoryApiResponse updateCategoryApi(String id, CategoryUpdateRequest request);

    /**
     * 删除分类 - API规范方法
     * 
     * <p>删除指定分类（需要先移动或删除分类下的文档）</p>
     * <p>对应API: POST /api/categories/{id}/delete</p>
     * 
     * @param id 分类ID
     */
    void deleteCategoryApi(String id);
}