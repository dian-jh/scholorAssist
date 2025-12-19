package com.zd.scaiservice.service;

import java.util.List;

public interface ChatHistoryService {
    /**
     * 获取会话id列表
     * @param documentId 文献id
     * @return
     */
    List<String> getChatIds(String documentId);

    /**
     * 删除对话历史
     * @param chatId
     */
    void deleteChatHistory(String chatId);

    /**
     * 保存会话元信息（幂等）
     * @param chatId 会话ID
     * @param userId 用户ID
     * @param documentId 文档ID
     * @param firstMessage 首次提问内容（可选，用于生成标题等）
     */
    void saveSession(String chatId, String userId, String documentId, String firstMessage);
}
