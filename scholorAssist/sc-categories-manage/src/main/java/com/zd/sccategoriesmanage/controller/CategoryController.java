package com.zd.sccategoriesmanage.controller;

import com.zd.sccategoriesmanage.model.dto.request.CategoryCreateRequest;
import com.zd.sccategoriesmanage.model.dto.request.CategoryUpdateRequest;
import com.zd.sccategoriesmanage.model.dto.response.CategoryApiResponse;
import com.zd.sccategoriesmanage.model.dto.response.CategoryTreeApiResponse;
import com.zd.sccategoriesmanage.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 分类管理控制器 - 严格遵循API文档规范
 * 
 * <p>提供分类的CRUD操作接口，所有响应由全局响应拦截器统一封装</p>
 * <p>接口设计严格按照 categories.md API文档规范实现</p>
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Validated
@Tag(name = "分类管理", description = "分类管理相关接口")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 获取分类列表
     * 
     * <p>获取所有文档分类，包含层级结构</p>
     * <p>API规范: GET /api/categories</p>
     *
     * @return 分类树列表
     */
    @GetMapping
    @Operation(summary = "获取分类列表", description = "获取所有文档分类，包含层级结构")
    public List<CategoryTreeApiResponse> getCategories() {
        
        log.info("接收获取分类列表请求");
        
        List<CategoryTreeApiResponse> categories = categoryService.getCategoriesApi();
        
        log.info("获取分类列表成功，根分类数量：{}", categories.size());
        return categories;
    }

    /**
     * 创建分类
     * 
     * <p>创建新的文档分类，支持创建根分类和子分类</p>
     * <p>API规范: POST /api/categories</p>
     *
     * @param request 创建请求
     * @return 分类信息
     */
    @PostMapping
    @Operation(summary = "创建分类", description = "创建新的文档分类")
    public CategoryApiResponse createCategory(
            @Parameter(description = "创建分类请求", required = true)
            @RequestBody @Valid CategoryCreateRequest request) {

        log.info("接收创建分类请求，分类名称：{}", request.getName());
        
        CategoryApiResponse response = categoryService.createCategoryApi(request);
        
        log.info("分类创建成功，分类ID：{}", response.getId());
        return response;
    }

    /**
     * 更新分类
     * 
     * <p>更新指定分类的名称或父分类</p>
     * <p>API规范: POST /api/categories/{id}</p>
     * 
     * @param id 分类ID
     * @param request 更新请求
     * @return 分类信息
     */
    @PostMapping("/{id}")
    @Operation(summary = "更新分类", description = "更新分类信息")
    public CategoryApiResponse updateCategory(
            @Parameter(description = "分类ID", required = true)
            @PathVariable @NotBlank(message = "分类ID不能为空") String id,
            @Parameter(description = "更新分类请求", required = true)
            @RequestBody @Valid CategoryUpdateRequest request) {
        
        log.info("接收更新分类请求，分类ID：{}", id);
        
        CategoryApiResponse response = categoryService.updateCategoryApi(id, request);
        
        log.info("分类更新成功，分类ID：{}", id);
        return response;
    }

    /**
     * 删除分类
     * 
     * <p>删除指定分类（需要先移动或删除分类下的文档）</p>
     * <p>API规范: POST /api/categories/{id}/delete</p>
     * 
     * @param id 分类ID
     */
    @PostMapping("/{id}/delete")
    @Operation(summary = "删除分类", description = "删除指定的分类")
    public void deleteCategory(
            @Parameter(description = "分类ID", required = true)
            @PathVariable @NotBlank(message = "分类ID不能为空") String id) {
        
        log.info("接收删除分类请求，分类ID：{}", id);
        
        categoryService.deleteCategoryApi(id);
        
        log.info("分类删除成功，分类ID：{}", id);
    }
}