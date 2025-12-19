package com.zd.scaiservice.controller;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.zd.scaiservice.constants.SystemConstants;
import com.zd.scaiservice.model.dto.request.ChatRequest;
import com.zd.scaiservice.service.ChatHistoryService;
import com.zd.sccommon.utils.UserContextUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "ai模块", description = "ai对话、历史会话等功能")
public class ChatController {

    private final ChatClient chatClient;
    private final ChatHistoryService chatHistoryService;
    private final VectorStore vectorStore; // 【新增】注入 VectorStore

    @PostMapping(value = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody ChatRequest request) {
        String prompt = request.getPrompt();
        String documentId = request.getDocumentId();
        String userId = UserContextUtil.getCurrentUserId();

        // 处理 chatId
        String chatId = request.getChatId();
        if (StrUtil.isBlank(chatId)) {
            chatId = IdUtil.simpleUUID();
        }

        // 保存会话元信息
        if (StrUtil.isNotBlank(documentId)) {
            chatHistoryService.saveSession(chatId, userId, documentId, prompt);
        }

        final String finalChatId = chatId;

        // ============ 第一步：尝试获取专属 anchor query ============
        String anchorFilter = "user_id == '" + userId + "' && document_id == '" + documentId + "' && type == 'anchor'";
        SearchRequest anchorRequest = SearchRequest.builder()
                .query("paper title abstract summary anchor")  // 提高命中率
                .topK(1)
                .similarityThreshold(0.0)
                .filterExpression(anchorFilter)
                .build();

        List<Document> anchorDocs = vectorStore.similaritySearch(anchorRequest);

        String ragQuery;
        if (!anchorDocs.isEmpty()) {
            Document anchorDoc = anchorDocs.get(0);
            String anchorText = anchorDoc.getText();

            if (StrUtil.isBlank(anchorText)) {
                ragQuery = "academic research paper content summary abstract introduction method experiment results conclusion";
            } else {
                ragQuery = anchorText;
            }
            log.info("RAG 使用论文专属 anchor query（长度: {}）", ragQuery.length());
        } else {
            ragQuery = "academic research paper content summary abstract introduction method experiment results conclusion innovation advantage efficiency real-time open-vocabulary";
            log.warn("未找到 anchor 文档，使用通用 fallback query");
        }

        // ============ 第二步：大召回论文内容 ============
        // 你的 ragQuery 在这里被正确使用了
        String contentFilter = "user_id == '" + userId + "' && document_id == '" + documentId + "'";
        SearchRequest searchRequest = SearchRequest.builder()
                .query(ragQuery)
                .topK(10)
                .similarityThreshold(0.3)
                .filterExpression(contentFilter)
                .build();

        // 1. 手动执行检索 (这是你已经写好的)
        List<Document> retrievedDocs = vectorStore.similaritySearch(searchRequest);
        log.info("RAG 最终召回 {} 条文档片段", retrievedDocs.size());

        // 2. 手动拼接上下文字符串
        String context = retrievedDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));
        log.info("实际注入上下文总长度: {} 字符", context.length());

        // 3. 手动渲染 Prompt 模板 (替代 Advisor 的工作)
        // 注意：PromptTemplate 里的占位符要和 Map 的 key 对应
        PromptTemplate customQaTemplate = new PromptTemplate("""
        用户问题：{query}
        
        以下是来自当前论文的相关内容片段（可能来自不同章节）：
        
        ---------------------
        {question_answer_context}
        ---------------------
        
        请基于以上内容，热情、专业地回答用户的问题。
        即使内容片段分散或不完整，也要积极整合、合理总结，给出最有帮助的回答。
        绝对不要说“没有内容”“上下文为空”或“我不知道”。
        用可爱、亲切的语气回复，带上表情符号～♡
        """);

        // 将变量填入模板
        Map<String, Object> promptVariables = new HashMap<>();
        promptVariables.put("query", prompt);
        promptVariables.put("question_answer_context", context);

        // 生成最终发给 LLM 的用户消息内容
        String finalUserMessage = customQaTemplate.render(promptVariables);

        // ============ 返回流式响应 ============
        // 既然已经手动处理了 RAG，就不需要 qaAdvisor 了
        return chatClient.prompt()
                .system(SystemConstants.CHAT_SYSTEM_PROMPT)
                .user(finalUserMessage) // 直接传入处理好的包含上下文的 Prompt
                .advisors(a -> a.param(org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID, finalChatId))
                // .advisors(qaAdvisor)  <-- 删掉这一行，避免重复检索和 query 覆盖
                .stream()
                .content();
    }
}