package com.zd.scaiservice.controller;
import com.zd.scaiservice.model.dto.response.MessageResponse;
import com.zd.scaiservice.service.ChatHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/history")
@RequiredArgsConstructor
public class ChatHistoryController {
    private final ChatHistoryService chatHistoryService;
    private final ChatMemory chatMemory;

    @Operation(summary = "获取会话ID列表", description = "根据类型用户id和文档id获取所有历史会话ID")
    @GetMapping("/{documentId}")
    public List<String> getChatIds(
            @Parameter(description = "文献ID", required = true)
            @PathVariable("documentId")
            String documentId) {
        return chatHistoryService.getChatIds(documentId);
    }

    @Operation(summary = "获取会话消息", description = "获取指定会话的所有历史消息")
    @Parameter(name = "chatId", description = "会话ID", required = true)
    @GetMapping("/{documentId}/{chatId}")
    public List<MessageResponse> getMessages(@PathVariable("documentId") String documentId,@PathVariable("chatId") String chatId) {
        List<Message> messages = chatMemory.get(chatId);
        if (messages == null){
            return List.of();
        }
        return messages.stream().map(MessageResponse::new).toList();
    }

    @Operation(summary = "删除对话")
    @Parameter(name = "documentId", description = "文献ID", required = true)
    @Parameter(name = "chatId", description = "会话ID", required = true)
    @DeleteMapping("/{documentId}/{chatId}")
    public void deleteChatHistory(@PathVariable("documentId") String documentId,@PathVariable("chatId") String chatId){
        chatHistoryService.deleteChatHistory(chatId);
    }
}
