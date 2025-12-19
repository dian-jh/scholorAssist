package com.zd.sccategoriesmanage.service.impl;

import com.zd.sccommon.common.BusinessException;
import com.zd.sccategoriesmanage.manager.CategoryManager;
import com.zd.sccategoriesmanage.model.domain.Category;
import com.zd.sccategoriesmanage.model.dto.request.CategoryCreateRequest;
import com.zd.sccategoriesmanage.model.dto.request.CategoryUpdateRequest;
import com.zd.sccategoriesmanage.model.dto.response.CategoryResponse;
import com.zd.sccategoriesmanage.model.dto.response.CategoryTreeResponse;
import com.zd.sccategoriesmanage.model.dto.response.CategoryApiResponse;
import com.zd.sccategoriesmanage.model.dto.response.CategoryTreeApiResponse;
import com.zd.sccategoriesmanage.service.CategoryService;
import com.zd.sccommon.utils.UserContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分类业务服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryManager categoryManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryResponse createCategory(String userId, CategoryCreateRequest request) {
        log.info("创建分类，用户ID：{}，分类名称：{}", userId, request.getName());
        
        // 参数校验
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException(400, "用户ID不能为空");
        }
        
        // 验证父分类是否存在（如果指定了父分类）
        if (StringUtils.hasText(request.getParentId())) {
            Category parentCategory = categoryManager.getByCategoryId(request.getParentId());
            if (parentCategory == null) {
                throw new BusinessException(404, "父分类不存在");
            }
            // 验证父分类是否属于当前用户
            if (!userId.equals(parentCategory.getUserId())) {
                throw new BusinessException(403, "无权限访问该父分类");
            }
        }
        
        // 检查同一父级下分类名称是否重复
        if (categoryManager.existsByUserIdAndParentIdAndName(userId, request.getParentId(), request.getName())) {
            throw new BusinessException(409, "同一父级下已存在相同名称的分类");
        }
        
        // 构建分类对象
        Category category = new Category();
        category.setCategoryId(UUID.randomUUID().toString());
        category.setUserId(userId);
        category.setParentId(request.getParentId());
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setSortOrder(request.getSortOrder());
        
        // 创建分类
        Category createdCategory = categoryManager.create(category);
        if (createdCategory == null) {
            throw new BusinessException(500, "分类创建失败");
        }
        
        log.info("分类创建成功，分类ID：{}", createdCategory.getCategoryId());
        return convertToResponse(createdCategory);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryResponse updateCategory(String userId, String categoryId, CategoryUpdateRequest request) {
        log.info("更新分类，用户ID：{}，分类ID：{}", userId, categoryId);
        
        // 参数校验
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException(400, "用户ID不能为空");
        }
        if (!StringUtils.hasText(categoryId)) {
            throw new BusinessException(400, "分类ID不能为空");
        }
        
        // 获取现有分类
        Category existingCategory = categoryManager.getByCategoryId(categoryId);
        if (existingCategory == null) {
            throw new BusinessException(404, "分类不存在");
        }
        
        // 验证权限
        if (!userId.equals(existingCategory.getUserId())) {
            throw new BusinessException(403, "无权限修改该分类");
        }
        
        // 检查名称是否重复（排除当前分类）
        if (categoryManager.existsByUserIdAndParentIdAndNameExcludeId(
                userId, existingCategory.getParentId(), request.getName(), categoryId)) {
            throw new BusinessException(409, "同一父级下已存在相同名称的分类");
        }
        
        // 更新分类信息
        existingCategory.setName(request.getName());
        existingCategory.setDescription(request.getDescription());
        existingCategory.setSortOrder(request.getSortOrder());
        
        boolean updated = categoryManager.updateByCategoryId(existingCategory);
        if (!updated) {
            throw new BusinessException(500, "分类更新失败");
        }
        
        log.info("分类更新成功，分类ID：{}", categoryId);
        return convertToResponse(existingCategory);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(String userId, String categoryId) {
        log.info("删除分类，用户ID：{}，分类ID：{}", userId, categoryId);
        
        // 参数校验
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException(400, "用户ID不能为空");
        }
        if (!StringUtils.hasText(categoryId)) {
            throw new BusinessException(400, "分类ID不能为空");
        }
        
        // 获取分类信息
        Category category = categoryManager.getByCategoryId(categoryId);
        if (category == null) {
            throw new BusinessException(404, "分类不存在");
        }
        
        // 验证权限
        if (!userId.equals(category.getUserId())) {
            throw new BusinessException(403, "无权限删除该分类");
        }
        
        // 检查是否有子分类
        List<Category> children = categoryManager.getByUserIdAndParentId(userId, categoryId);
        if (!children.isEmpty()) {
            throw new BusinessException(409, "存在子分类，无法删除");
        }
        
        // 检查是否有文档
        if (category.getDocumentCount() != null && category.getDocumentCount() > 0) {
            throw new BusinessException(409, "分类下存在文档，无法删除");
        }
        
        // 删除分类
        boolean deleted = categoryManager.deleteByCategoryId(categoryId);
        if (!deleted) {
            throw new BusinessException(500, "分类删除失败");
        }
        
        log.info("分类删除成功，分类ID：{}", categoryId);
    }

    @Override
    public List<CategoryTreeResponse> getCategoryTree(String userId) {
        log.info("获取用户分类树，用户ID：{}", userId);
        
        // 参数校验
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException(400, "用户ID不能为空");
        }
        
        // 获取用户所有分类
        List<Category> allCategories = categoryManager.getByUserId(userId);
        if (allCategories.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 构建分类树
        List<CategoryTreeResponse> tree = buildCategoryTree(allCategories);
        
        log.info("获取用户分类树成功，用户ID：{}，根分类数量：{}", userId, tree.size());
        return tree;
    }

    /**
     * 构建分类树
     * 
     * @param categories 所有分类列表
     * @return 分类树
     */
    private List<CategoryTreeResponse> buildCategoryTree(List<Category> categories) {
        // 按父分类ID分组
        Map<String, List<Category>> categoryMap = categories.stream()
                .collect(Collectors.groupingBy(category -> 
                        category.getParentId() == null ? "ROOT" : category.getParentId()));
        
        // 获取根分类
        List<Category> rootCategories = categoryMap.getOrDefault("ROOT", new ArrayList<>());
        
        // 构建树结构
        return rootCategories.stream()
                .map(category -> buildTreeNode(category, categoryMap))
                .collect(Collectors.toList());
    }

    /**
     * 构建树节点
     * 
     * @param category 分类
     * @param categoryMap 分类映射
     * @return 树节点
     */
    private CategoryTreeResponse buildTreeNode(Category category, Map<String, List<Category>> categoryMap) {
        CategoryTreeResponse node = convertToTreeResponse(category);
        
        // 获取子分类
        List<Category> children = categoryMap.getOrDefault(category.getCategoryId(), new ArrayList<>());
        if (!children.isEmpty()) {
            List<CategoryTreeResponse> childNodes = children.stream()
                    .map(child -> buildTreeNode(child, categoryMap))
                    .collect(Collectors.toList());
            node.setChildren(childNodes);
        }
        
        return node;
    }

    /**
     * 转换为响应对象
     * 
     * @param category 分类
     * @return 响应对象
     */
    private CategoryResponse convertToResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setCategoryId(category.getCategoryId());
        response.setParentId(category.getParentId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        response.setSortOrder(category.getSortOrder());
        response.setDocumentCount(category.getDocumentCount());
        response.setCreatedAt(category.getCreatedAt());
        response.setUpdatedAt(category.getUpdatedAt());
        return response;
    }

    /**
     * 转换为树响应对象
     * 
     * @param category 分类
     * @return 树响应对象
     */
    private CategoryTreeResponse convertToTreeResponse(Category category) {
        CategoryTreeResponse response = new CategoryTreeResponse();
        response.setCategoryId(category.getCategoryId());
        response.setParentId(category.getParentId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        response.setSortOrder(category.getSortOrder());
        response.setDocumentCount(category.getDocumentCount());
        response.setCreatedAt(category.getCreatedAt());
        response.setUpdatedAt(category.getUpdatedAt());
        response.setChildren(new ArrayList<>());
        return response;
    }

    // ==================== API接口实现 ====================

    @Override
    public List<CategoryTreeApiResponse> getCategoriesApi() {
        log.info("获取分类列表API");



        // 1. 获取所有本用户分类（平铺列表）
        List<Category> categories = categoryManager.getAllCategories();

        // 2. 将所有 Category 实体 转换为 API DTO（此时仍然是平铺列表）
        //    注意：这里应该使用 convertToTreeApiResponse
        List<CategoryTreeApiResponse> flatDtoList = categories.stream()
                .map(this::convertToTreeApiResponse) // <-- 已修正：使用 DTO 树版本
                .collect(Collectors.toList());

        // 3. 将平铺列表构建为树形结构
        //    这将调用我们下面新添加的 buildCategoryTree(List<CategoryTreeApiResponse> flatList) 方法
        return buildCategoryTree1(flatDtoList);
    }
    /**
     * 构建API分类树 (重载方法)
     * <p>
     * 将 CategoryTreeApiResponse 的平铺列表构建为树形结构。
     * (注意：此方法与 buildCategoryTree(List<Category> categories) 不同，它处理 DTO 列表)
     *
     * @param flatList 包含所有分类的 DTO 列表
     * @return 只包含根节点（parent_id 为 null）的树形列表
     */
    private List<CategoryTreeApiResponse> buildCategoryTree1(List<CategoryTreeApiResponse> flatList) {
        // 1. 创建一个 Map，用于通过 ID 快速查找任何一个节点
        Map<String, CategoryTreeApiResponse> map = flatList.stream()
                .collect(Collectors.toMap(CategoryTreeApiResponse::getId, Function.identity()));

        // 2. 创建一个列表，用于存储最终的树（只包含根节点）
        List<CategoryTreeApiResponse> tree = new ArrayList<>();

        // 3. 遍历所有节点，将它们放到正确的位置
        for (CategoryTreeApiResponse node : flatList) {
            String parentId = node.getParentId();

            if (parentId == null) {
                // 3.1 如果 parentId 为 null，说明它是一个根节点
                tree.add(node);
            } else {
                // 3.2 如果 parentId 不为 null，通过 Map 找到它的父节点
                CategoryTreeApiResponse parent = map.get(parentId);

                if (parent != null) {
                    // 3.3 如果找到了父节点：
                    //     (因为 convertToTreeApiResponse 已初始化 children, 故无需检查 null)
                    parent.getChildren().add(node);
                } else {
                    // （可选）处理孤儿节点（即 parentId 指向一个不存在的ID）
                    log.warn("发现API分类树孤儿节点: id={}, parentId={}", node.getId(), parentId);
                    // 也可以将孤儿节点视为根节点：
                    // tree.add(node);
                }
            }
        }

        // 4. 返回只包含根节点的列表
        return tree;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryApiResponse createCategoryApi(CategoryCreateRequest request) {
        log.info("创建分类API，分类名称：{}", request.getName());
        
        // 验证父分类是否存在（如果指定了父分类）
        if (StringUtils.hasText(request.getParentId())) {
            Category parentCategory = categoryManager.getByCategoryId(request.getParentId());
            if (parentCategory == null) {
                throw new BusinessException(404, "父分类不存在");
            }
        }
        
        // 检查同一父级下分类名称是否重复
        if (categoryManager.existsByParentIdAndName(request.getParentId(), request.getName())) {
            throw new BusinessException(409, "同一父级下已存在相同名称的分类");
        }
        
        // 构建分类对象
        Category category = new Category();
        category.setCategoryId(UUID.randomUUID().toString());
        category.setParentId(request.getParentId());
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setSortOrder(request.getSortOrder());
        category.setUserId(UserContextUtil.getCurrentUserId());
        
        // 创建分类
        Category createdCategory = categoryManager.create(category);
        if (createdCategory == null) {
            throw new BusinessException(500, "分类创建失败");
        }
        
        log.info("分类创建成功，分类ID：{}", createdCategory.getCategoryId());
        return convertToApiResponse(createdCategory);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryApiResponse updateCategoryApi(String categoryId, CategoryUpdateRequest request) {
        log.info("更新分类API，分类ID：{}", categoryId);
        
        // 参数校验
        if (!StringUtils.hasText(categoryId)) {
            throw new BusinessException(400, "分类ID不能为空");
        }
        
        // 获取现有分类
        Category existingCategory = categoryManager.getByCategoryId(categoryId);
        if (existingCategory == null) {
            throw new BusinessException(404, "分类不存在");
        }
        
        // 验证父分类是否存在（如果指定了父分类）
        if (StringUtils.hasText(request.getParentId())) {
            Category parentCategory = categoryManager.getByCategoryId(request.getParentId());
            if (parentCategory == null) {
                throw new BusinessException(404, "父分类不存在");
            }
            
            // 防止将分类设置为自己的子分类
            if (categoryId.equals(request.getParentId())) {
                throw new BusinessException(400, "不能将分类设置为自己的父分类");
            }
        }
        
        // 检查同一父级下分类名称是否重复（排除自己）
        if (categoryManager.existsByParentIdAndNameExcludeId(request.getParentId(), request.getName(), categoryId)) {
            throw new BusinessException(409, "同一父级下已存在相同名称的分类");
        }
        
        // 更新分类信息
        existingCategory.setParentId(request.getParentId());
        existingCategory.setName(request.getName());
        existingCategory.setDescription(request.getDescription());
        existingCategory.setSortOrder(request.getSortOrder());
        
        // 执行更新
        Category updatedCategory = categoryManager.update(existingCategory);
        if (updatedCategory == null) {
            throw new BusinessException(500, "分类更新失败");
        }
        
        log.info("分类更新成功，分类ID：{}", categoryId);
        return convertToApiResponse(updatedCategory);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategoryApi(String categoryId) {
        log.info("删除分类API，分类ID：{}", categoryId);
        
        // 参数校验
        if (!StringUtils.hasText(categoryId)) {
            throw new BusinessException(400, "分类ID不能为空");
        }
        
        // 获取现有分类
        Category existingCategory = categoryManager.getByCategoryId(categoryId);
        if (existingCategory == null) {
            throw new BusinessException(404, "分类不存在");
        }
        
        // 检查是否有子分类
        if (categoryManager.hasChildren(categoryId)) {
            throw new BusinessException(400, "该分类下存在子分类，无法删除");
        }
        
        // 检查是否有关联的文档
        //TODO 这里进行远程调用文档服务api,检查该分类下是否有文档
        if (categoryManager.hasDocuments(categoryId)) {
            throw new BusinessException(400, "该分类下存在文档，无法删除");
        }
        
        // 执行删除
        boolean deleted = categoryManager.delete(categoryId);
        if (!deleted) {
            throw new BusinessException(500, "分类删除失败");
        }
        
        log.info("分类删除成功，分类ID：{}", categoryId);
    }

    /**
     * 转换为API响应对象
     * 
     * @param category 分类
     * @return API响应对象
     */
    private CategoryApiResponse convertToApiResponse(Category category) {
        CategoryApiResponse response = new CategoryApiResponse();
        response.setId(category.getCategoryId());
        response.setName(category.getName());
        response.setParentId(category.getParentId());
        response.setCreatedAt(String.valueOf(category.getCreatedAt()));
        response.setDocumentCount(category.getDocumentCount());
        return response;
    }

    /**
     * 转换为API树响应对象
     * 
     * @param category 分类
     * @return API树响应对象
     */
    private CategoryTreeApiResponse convertToTreeApiResponse(Category category) {
        CategoryTreeApiResponse response = new CategoryTreeApiResponse();
        response.setId(category.getCategoryId());
        response.setName(category.getName());
        response.setParentId(category.getParentId());
        response.setCreatedAt(String.valueOf(category.getCreatedAt()));
        response.setDocumentCount(category.getDocumentCount());
        response.setChildren(new ArrayList<>());
        return response;
    }
}