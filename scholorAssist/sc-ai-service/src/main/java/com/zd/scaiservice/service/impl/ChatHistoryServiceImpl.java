package com.zd.scaiservice.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zd.scaiservice.mapper.AiChatSessionMapper;
import com.zd.scaiservice.mapper.ChatHistoryMapper;
import com.zd.scaiservice.model.domain.AiChatSession;
import com.zd.scaiservice.service.ChatHistoryService;
import com.zd.sccommon.common.BusinessException;
import com.zd.sccommon.utils.UserContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatHistoryServiceImpl implements ChatHistoryService {

    private final ChatHistoryMapper chatHistoryMapper;
    private final AiChatSessionMapper aiChatSessionMapper;

    @Override
    public List<String> getChatIds(String documentId) {
        String userId = UserContextUtil.getCurrentUserId();
        if (ObjectUtil.isEmpty(userId)) {
            throw new BusinessException(401, "用户未登录");
        }
        // 1. 从业务表查询会话ID列表
        List<AiChatSession> sessions = aiChatSessionMapper.selectList(
                new LambdaQueryWrapper<AiChatSession>()
                        .eq(AiChatSession::getUserId, userId)
                        .eq(AiChatSession::getDocumentId, documentId)
                        .orderByDesc(AiChatSession::getCreatedAt)
        );

        if (ObjectUtil.isEmpty(sessions)) {
            return Collections.emptyList();
        }
        return sessions.stream().map(AiChatSession::getConversationId).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChatHistory(String chatId) {
        // 1. 删除业务索引
        aiChatSessionMapper.delete(new LambdaQueryWrapper<AiChatSession>().eq(AiChatSession::getConversationId, chatId));
        chatHistoryMapper.deleteByConversationId(chatId);
    }

    @Override
    public void saveSession(String chatId, String userId, String documentId, String firstMessage) {
        // 检查是否已存在
        Long count = aiChatSessionMapper.selectCount(
                new LambdaQueryWrapper<AiChatSession>().eq(AiChatSession::getConversationId, chatId)
        );
        if (count > 0) return;

        AiChatSession session = AiChatSession.builder()
                .conversationId(chatId)
                .userId(userId)
                .documentId(documentId)
                .createdAt(LocalDateTime.now())
                .build();
        aiChatSessionMapper.insert(session);
    }
}