package com.zd.scnoteservice.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zd.sccommon.utils.UserContextUtil;
import com.zd.scnoteservice.model.dto.request.NoteCreateRequest;
import com.zd.scnoteservice.model.dto.request.NoteQueryRequest;
import com.zd.scnoteservice.model.dto.request.NoteUpdateRequest;
import com.zd.scnoteservice.model.dto.response.NoteResponse;
import com.zd.scnoteservice.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * 笔记管理控制器
 * 
 * <p>提供笔记相关的REST API接口</p>
 * <p>包括笔记的创建、查询、更新、删除等功能</p>
 * <p>严格按照API文档规范实现接口</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Slf4j
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@Validated
@Tag(name = "笔记管理", description = "笔记的创建、查询、更新、删除等功能")
public class NoteController {

    private final NoteService noteService;

    /**
     * 获取笔记列表
     * 
     * <p>获取用户创建的笔记列表，支持按文档筛选和分页</p>
     * 
     * @param documentId 文档ID，可选
     * @param page 页码，从1开始，默认1
     * @param pageSize 每页数量，默认20，最大100
     * @param onlyFavorites 是否只查询收藏，默认false
     * @param tag 标签筛选，可选
     * @return 笔记列表
     */
    @GetMapping
    @Operation(summary = "获取笔记列表", description = "获取用户的笔记列表，支持按文档筛选")
    public IPage<NoteResponse> getNotes(
            @Parameter(description = "文档ID，不传则获取全部笔记") 
            @RequestParam(required = false) String documentId,
            
            @Parameter(description = "页码，从1开始") 
            @RequestParam(defaultValue = "1") Integer page,
            
            @Parameter(description = "每页数量，默认20，最大100") 
            @RequestParam(defaultValue = "20") Integer pageSize,
            
            @Parameter(description = "是否只查询收藏的笔记") 
            @RequestParam(defaultValue = "false") Boolean onlyFavorites,
            
            @Parameter(description = "标签筛选") 
            @RequestParam(required = false) String tag) {
        
        log.info("获取笔记列表请求，文档ID：{}，页码：{}，每页数量：{}", documentId, page, pageSize);
        
        // 模拟获取当前用户ID（实际项目中应从JWT或Session中获取）
        String userId = UserContextUtil.getCurrentUserId();
        
        // 构建查询请求
        NoteQueryRequest request = NoteQueryRequest.builder()
                .documentId(documentId)
                .page(page)
                .pageSize(pageSize)
                .onlyFavorites(onlyFavorites)
                .tag(tag)
                .build();
        
        return noteService.getNotes(request, userId);
    }

    /**
     * 创建笔记
     * 
     * <p>为指定文档创建新的笔记</p>
     * 
     * @param request 创建笔记请求
     * @return 创建的笔记信息
     */
    @PostMapping
    @Operation(summary = "创建笔记", description = "创建新的笔记")
    public NoteResponse createNote(@Valid @RequestBody NoteCreateRequest request) {
        log.info("创建笔记请求，文档ID：{}，标题：{}", request.getDocumentId(), request.getTitle());

        
        return noteService.createNote(request);
    }

    /**
     * 获取笔记详情
     * 
     * <p>根据笔记ID获取笔记的详细信息</p>
     * 
     * @param noteId 笔记ID
     * @return 笔记详情
     */
    @GetMapping("/{noteId}")
    @Operation(summary = "获取笔记详情", description = "根据笔记ID获取笔记详情")
    public NoteResponse getNoteById(
            @Parameter(description = "笔记ID", required = true)
            @PathVariable @NotBlank(message = "笔记ID不能为空") String noteId) {
        
        log.info("获取笔记详情请求，笔记ID：{}", noteId);
        
        // 模拟获取当前用户ID（实际项目中应从JWT或Session中获取）
        String userId = UserContextUtil.getCurrentUserId();
        
        return noteService.getNoteById(noteId, userId);
    }

    /**
     * 更新笔记
     * 
     * <p>更新指定笔记的标题、内容、页码或标签信息</p>
     * 
     * @param noteId 笔记ID
     * @param request 更新笔记请求
     * @return 更新后的笔记信息
     */
    @PostMapping("/{noteId}")
    @Operation(summary = "更新笔记", description = "更新笔记内容")
    public NoteResponse updateNote(
            @Parameter(description = "笔记ID", required = true)
            @PathVariable @NotBlank(message = "笔记ID不能为空") String noteId,
            
            @Valid @RequestBody NoteUpdateRequest request) {
        
        log.info("更新笔记请求，笔记ID：{}", noteId);
        
        // 模拟获取当前用户ID（实际项目中应从JWT或Session中获取）
        String userId = UserContextUtil.getCurrentUserId();
        
        return noteService.updateNote(noteId, request, userId);
    }

    /**
     * 删除笔记
     * 
     * <p>删除指定的笔记，删除操作不可恢复</p>
     * 
     * @param noteId 笔记ID
     */
    @PostMapping("/{noteId}/delete")
    @Operation(summary = "删除笔记", description = "删除指定的笔记")
    public void deleteNote(
            @Parameter(description = "笔记ID", required = true)
            @PathVariable @NotBlank(message = "笔记ID不能为空") String noteId) {
        
        log.info("删除笔记请求，笔记ID：{}", noteId);
        
        // 模拟获取当前用户ID（实际项目中应从JWT或Session中获取）
        String userId = UserContextUtil.getCurrentUserId();
        
        noteService.deleteNote(noteId, userId);
    }
}