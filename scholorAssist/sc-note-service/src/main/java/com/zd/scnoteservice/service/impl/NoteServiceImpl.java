package com.zd.scnoteservice.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zd.sccommon.common.BusinessException;
import com.zd.sccommon.utils.UserContextUtil;
import com.zd.scnoteservice.manager.NoteManager;
import com.zd.scnoteservice.model.domain.Note;
import com.zd.scnoteservice.model.dto.request.NoteCreateRequest;
import com.zd.scnoteservice.model.dto.request.NoteQueryRequest;
import com.zd.scnoteservice.model.dto.request.NoteUpdateRequest;
import com.zd.scnoteservice.model.dto.response.NoteResponse;
import com.zd.scnoteservice.service.NoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.zd.scnoteservice.exception.NoteException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 笔记服务实现类
 * 
 * <p>实现笔记相关的业务逻辑</p>
 * <p>负责参数校验、权限控制、业务流程编排</p>
 * <p>通过NoteManager访问数据层</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final NoteManager noteManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoteResponse createNote(NoteCreateRequest request) {
        String userId = UserContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        log.info("创建笔记，用户ID：{}，文档ID：{}，标题：{}", userId, request.getDocumentId(), request.getTitle());
        
        // 参数校验
        validateCreateRequest(request);
        
        // 构建笔记实体
        Note note = buildNoteFromCreateRequest(request, userId);
        
        // 创建笔记
        Note createdNote = noteManager.createNote(note);
        
        // 转换为响应DTO
        return convertToNoteResponse(createdNote);
    }

    @Override
    public IPage<NoteResponse> getNotes(NoteQueryRequest request, String userId) {
        log.info("查询笔记列表，用户ID：{}，文档ID：{}，页码：{}，每页数量：{}", 
                userId, request.getDocumentId(), request.getPage(), request.getPageSize());
        
        // 参数校验
        validateQueryRequest(request);
        
        // 查询笔记列表
        IPage<Note> notePage = noteManager.getNotesByUserId(
                userId, 
                request.getDocumentId(), 
                request.getPage(), 
                request.getPageSize(),
                request.getOnlyFavorites(),
                request.getTag()
        );
        
        // 转换为响应DTO
        List<NoteResponse> responseList = notePage.getRecords().stream()
                .map(this::convertToNoteResponse)
                .collect(Collectors.toList());
        
        // 构建分页响应
        Page<NoteResponse> responsePage = new Page<>(request.getPage(), request.getPageSize(), notePage.getTotal());
        responsePage.setRecords(responseList);
        
        return responsePage;
    }

    @Override
    public NoteResponse getNoteById(String noteId, String userId) {
        log.info("获取笔记详情，笔记ID：{}，用户ID：{}", noteId, userId);
        
        // 参数校验
        if (!StringUtils.hasText(noteId)) {
            throw new IllegalArgumentException("笔记ID不能为空");
        }
        
        // 查询笔记
        Note note = noteManager.getNoteByNoteId(noteId);
        if (note == null) {
            throw NoteException.noteNotFound(noteId);
        }
        
        // 权限校验
        if (!userId.equals(note.getUserId())) {
            throw NoteException.accessDenied("查看笔记");
        }
        
        return convertToNoteResponse(note);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoteResponse updateNote(String noteId, NoteUpdateRequest request, String userId) {
        log.info("更新笔记，笔记ID：{}，用户ID：{}", noteId, userId);
        
        // 参数校验
        if (!StringUtils.hasText(noteId)) {
            throw new IllegalArgumentException("笔记ID不能为空");
        }
        validateUpdateRequest(request);
        
        // 检查笔记是否存在
        Note existingNote = noteManager.getNoteByNoteId(noteId);
        if (existingNote == null) {
            throw NoteException.noteNotFound(noteId);
        }
        
        // 检查权限
        if (!existingNote.getUserId().equals(userId)) {
            throw NoteException.accessDenied("更新笔记");
        }
        
        // 构建更新实体
        Note updateNote = buildNoteFromUpdateRequest(request, noteId);
        
        // 执行更新
        boolean success = noteManager.updateNote(updateNote);
        if (!success) {
            throw new RuntimeException("笔记更新失败");
        }
        
        // 查询更新后的笔记
        Note updatedNote = noteManager.getNoteByNoteId(noteId);
        return convertToNoteResponse(updatedNote);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNote(String noteId, String userId) {
        log.info("删除笔记，笔记ID：{}，用户ID：{}", noteId, userId);
        
        // 参数校验
        if (!StringUtils.hasText(noteId)) {
            throw new IllegalArgumentException("笔记ID不能为空");
        }
        
        // 检查笔记是否存在
        Note existingNote = noteManager.getNoteByNoteId(noteId);
        if (existingNote == null) {
            throw NoteException.noteNotFound(noteId);
        }
        
        // 检查权限
        if (!existingNote.getUserId().equals(userId)) {
            throw NoteException.accessDenied("删除笔记");
        }
        
        // 执行删除
        boolean success = noteManager.deleteNote(noteId);
        if (!success) {
            throw new RuntimeException("笔记删除失败");
        }
    }

    /**
     * 校验创建请求参数
     */
    private void validateCreateRequest(NoteCreateRequest request) {
        if (!StringUtils.hasText(request.getDocumentId())) {
            throw NoteException.invalidParameter("文档ID不能为空");
        }
        if (request.getTags() != null && request.getTags().length > 10) {
            throw NoteException.invalidParameter("标签数量不能超过10个");
        }
    }

    /**
     * 校验查询请求参数
     */
    private void validateQueryRequest(NoteQueryRequest request) {
        if (request.getPage() == null || request.getPage() < 1) {
            request.setPage(1);
        }
        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(20);
        }
        if (request.getPageSize() > 100) {
            request.setPageSize(100);
        }
    }

    /**
     * 校验更新请求参数
     */
    private void validateUpdateRequest(NoteUpdateRequest request) {
        // 至少要有一个字段需要更新
        if (!StringUtils.hasText(request.getTitle()) && 
            !StringUtils.hasText(request.getContent()) &&
            request.getPageNumber() == null &&
            request.getTags() == null &&
            request.getSelectedText() == null &&
            request.getPositionInfo() == null &&
            request.getIsFavorite() == null) {
            throw new IllegalArgumentException("至少需要更新一个字段");
        }
        
        if (request.getTags() != null && request.getTags().length > 10) {
            throw new IllegalArgumentException("标签数量不能超过10个");
        }
    }

    /**
     * 从创建请求构建笔记实体
     */
    private Note buildNoteFromCreateRequest(NoteCreateRequest request, String userId) {
        return Note.builder()
                .userId(userId)
                .documentId(request.getDocumentId())
                .title(request.getTitle())
                .content(request.getContent())
                .pageNumber(request.getPageNumber())
                .selectedText(request.getSelectedText())
                .positionInfo(request.getPositionInfo())
                .tags(request.getTags())
                .isFavorite(false) // 默认不收藏
                .build();
    }

    /**
     * 从更新请求构建笔记实体
     */
    private Note buildNoteFromUpdateRequest(NoteUpdateRequest request, String noteId) {
        Note.NoteBuilder builder = Note.builder().noteId(noteId);
        
        if (StringUtils.hasText(request.getTitle())) {
            builder.title(request.getTitle());
        }
        if (StringUtils.hasText(request.getContent())) {
            builder.content(request.getContent());
        }
        if (request.getPageNumber() != null) {
            builder.pageNumber(request.getPageNumber());
        }
        if (request.getSelectedText() != null) {
            builder.selectedText(request.getSelectedText());
        }
        if (request.getPositionInfo() != null) {
            builder.positionInfo(request.getPositionInfo());
        }
        if (request.getTags() != null) {
            builder.tags(request.getTags());
        }
        if (request.getIsFavorite() != null) {
            builder.isFavorite(request.getIsFavorite());
        }
        
        return builder.build();
    }

    /**
     * 转换笔记实体为响应DTO
     */
    private NoteResponse convertToNoteResponse(Note note) {
        return NoteResponse.builder()
                .id(note.getNoteId())
                .documentId(note.getDocumentId())
                .title(note.getTitle())
                .content(note.getContent())
                .pageNumber(note.getPageNumber())
                .selectedText(note.getSelectedText())
                .positionInfo(note.getPositionInfo())
                .tags(note.getTags())
                .isFavorite(note.getIsFavorite())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
}