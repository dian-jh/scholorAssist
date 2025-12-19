package com.zd.scnoteservice.manager;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zd.scnoteservice.mapper.NoteMapper;
import com.zd.scnoteservice.model.domain.Note;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 笔记数据管理器
 * 
 * <p>负责笔记数据的访问和缓存操作</p>
 * <p>封装对NoteMapper的调用，提供细粒度的数据操作方法</p>
 * <p>统一处理数据库访问异常和缓存逻辑</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoteManager {

    private final NoteMapper noteMapper;
    /**
     * Hutool雪花算法ID生成器
     * 使用机器ID=1，数据中心ID=1
     */
    private final Snowflake snowflake = IdUtil.getSnowflake(1, 1);


    /**
     * 创建笔记
     * 
     * @param note 笔记实体
     * @return 创建的笔记
     */
    public Note createNote(Note note) {
        log.info("创建笔记，用户ID：{}，文档ID：{}", note.getUserId(), note.getDocumentId());
        
        // 生成笔记ID
        note.setNoteId(String.valueOf(snowflake.nextId()));
        
        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        note.setCreatedAt(now);
        note.setUpdatedAt(now);
        
        // 设置默认值
        if (note.getPageNumber() == null) {
            note.setPageNumber(1);
        }
        if (note.getIsFavorite() == null) {
            note.setIsFavorite(false);
        }
        
        int result = noteMapper.insert(note);
        if (result > 0) {
            log.info("笔记创建成功，笔记ID：{}", note.getNoteId());
            return note;
        } else {
            log.error("笔记创建失败，用户ID：{}，文档ID：{}", note.getUserId(), note.getDocumentId());
            throw new RuntimeException("笔记创建失败");
        }
    }

    /**
     * 根据笔记ID查询笔记
     * 
     * @param noteId 笔记ID
     * @return 笔记实体，不存在返回null
     */
    public Note getNoteByNoteId(String noteId) {
        log.debug("根据笔记ID查询笔记：{}", noteId);
        return noteMapper.selectByNoteId(noteId);
    }

    /**
     * 根据用户ID分页查询笔记列表
     * 
     * @param userId 用户ID
     * @param documentId 文档ID，可为空
     * @param page 页码
     * @param pageSize 每页数量
     * @param onlyFavorites 是否只查询收藏
     * @param tag 标签筛选
     * @return 分页结果
     */
    public IPage<Note> getNotesByUserId(String userId, String documentId, Integer page, Integer pageSize, 
                                       Boolean onlyFavorites, String tag) {
        log.debug("分页查询用户笔记，用户ID：{}，文档ID：{}，页码：{}，每页数量：{}", userId, documentId, page, pageSize);
        
        Page<Note> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Note> queryWrapper = new LambdaQueryWrapper<>();
        
        // 用户ID条件
        queryWrapper.eq(Note::getUserId, userId);
        
        // 文档ID条件
        if (StringUtils.hasText(documentId)) {
            queryWrapper.eq(Note::getDocumentId, documentId);
        }
        
        // 收藏条件
        if (Boolean.TRUE.equals(onlyFavorites)) {
            queryWrapper.eq(Note::getIsFavorite, true);
        }
        
        // 标签条件（PostgreSQL数组查询）
        if (StringUtils.hasText(tag)) {
            // 使用自定义查询方法
            return getNotesWithTag(userId, tag, page, pageSize);
        }
        
        // 排序：如果指定文档，按页码和创建时间排序；否则按创建时间倒序
        if (StringUtils.hasText(documentId)) {
            queryWrapper.orderByAsc(Note::getPageNumber).orderByAsc(Note::getCreatedAt);
        } else {
            queryWrapper.orderByDesc(Note::getCreatedAt);
        }
        
        return noteMapper.selectPage(pageParam, queryWrapper);
    }

    /**
     * 根据标签查询笔记（分页）
     * 
     * @param userId 用户ID
     * @param tag 标签
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    private IPage<Note> getNotesWithTag(String userId, String tag, Integer page, Integer pageSize) {
        log.debug("根据标签查询笔记，用户ID：{}，标签：{}", userId, tag);
        
        // 计算偏移量
        int offset = (page - 1) * pageSize;
        
        // 查询数据
        List<Note> notes = noteMapper.selectByUserIdAndTag(userId, tag);
        
        // 手动分页
        int total = notes.size();
        int fromIndex = Math.min(offset, total);
        int toIndex = Math.min(offset + pageSize, total);
        List<Note> pageData = notes.subList(fromIndex, toIndex);
        
        // 构造分页结果
        Page<Note> result = new Page<>(page, pageSize, total);
        result.setRecords(pageData);
        
        return result;
    }

    /**
     * 更新笔记
     * 
     * @param note 笔记实体
     * @return 是否更新成功
     */
    public boolean updateNote(Note note) {
        log.info("更新笔记，笔记ID：{}", note.getNoteId());
        
        // 设置更新时间
        note.setUpdatedAt(LocalDateTime.now());
        
        int result = noteMapper.updateByNoteId(note);
        if (result > 0) {
            log.info("笔记更新成功，笔记ID：{}", note.getNoteId());
            return true;
        } else {
            log.warn("笔记更新失败，笔记ID：{}", note.getNoteId());
            return false;
        }
    }

    /**
     * 删除笔记
     * 
     * @param noteId 笔记ID
     * @return 是否删除成功
     */
    public boolean deleteNote(String noteId) {
        log.info("删除笔记，笔记ID：{}", noteId);
        
        int result = noteMapper.deleteByNoteId(noteId);
        if (result > 0) {
            log.info("笔记删除成功，笔记ID：{}", noteId);
            return true;
        } else {
            log.warn("笔记删除失败，笔记ID：{}", noteId);
            return false;
        }
    }

    /**
     * 检查笔记是否属于指定用户
     * 
     * @param noteId 笔记ID
     * @param userId 用户ID
     * @return 是否属于该用户
     */
    public boolean isNoteOwnedByUser(String noteId, String userId) {
        log.debug("检查笔记所有权，笔记ID：{}，用户ID：{}", noteId, userId);
        
        Note note = getNoteByNoteId(noteId);
        return note != null && userId.equals(note.getUserId());
    }

    /**
     * 根据文档ID删除所有笔记
     * 
     * @param documentId 文档ID
     * @return 删除的笔记数量
     */
    public int deleteNotesByDocumentId(String documentId) {
        log.info("根据文档ID删除笔记，文档ID：{}", documentId);
        
        int result = noteMapper.deleteByDocumentId(documentId);
        log.info("删除笔记完成，文档ID：{}，删除数量：{}", documentId, result);
        
        return result;
    }

    /**
     * 生成笔记ID
     * 
     * @return 笔记ID
     */
    private String generateNoteId() {
        return "note_" + UUID.randomUUID().toString().replace("-", "");
    }
}