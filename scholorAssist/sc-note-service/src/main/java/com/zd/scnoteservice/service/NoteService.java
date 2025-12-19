package com.zd.scnoteservice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zd.scnoteservice.model.dto.request.NoteCreateRequest;
import com.zd.scnoteservice.model.dto.request.NoteQueryRequest;
import com.zd.scnoteservice.model.dto.request.NoteUpdateRequest;
import com.zd.scnoteservice.model.dto.response.NoteResponse;

/**
 * 笔记服务接口
 * 
 * <p>定义笔记相关的业务操作</p>
 * <p>包括笔记的创建、查询、更新、删除等功能</p>
 * 
 * @author System
 * @since 2024-01-21
 */
public interface NoteService {

    /**
     * 创建笔记
     * 
     * @param request 创建笔记请求
     * @return 创建的笔记信息
     */
    NoteResponse createNote(NoteCreateRequest request);

    /**
     * 分页查询笔记列表
     * 
     * @param request 查询请求
     * @param userId 用户ID
     * @return 分页笔记列表
     */
    IPage<NoteResponse> getNotes(NoteQueryRequest request, String userId);

    /**
     * 根据笔记ID获取笔记详情
     * 
     * @param noteId 笔记ID
     * @param userId 用户ID
     * @return 笔记详情
     */
    NoteResponse getNoteById(String noteId, String userId);

    /**
     * 更新笔记
     * 
     * @param noteId 笔记ID
     * @param request 更新请求
     * @param userId 用户ID
     * @return 更新后的笔记信息
     */
    NoteResponse updateNote(String noteId, NoteUpdateRequest request, String userId);

    /**
     * 删除笔记
     * 
     * @param noteId 笔记ID
     * @param userId 用户ID
     */
    void deleteNote(String noteId, String userId);
}