package com.zd.scaiservice.config;


import com.zd.scaiservice.constants.SystemConstants;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonConfiguration {
//    @Bean
//    public JdbcChatMemoryRepository chatMemoryRepository(DataSource dataSource) {
//        // 这会使用你在 application.yaml 中配置的 dataSource
//        return JdbcChatMemoryRepository.builder()
//                .dataSource(dataSource)
//                .build();
//    }
    //配置聊天记忆
    @Autowired
    JdbcChatMemoryRepository chatMemoryRepository;//springai自动配置了
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20) // 设置消息窗口大小
                .build();
    }

    //设置一个chatClient
    @Bean
    public ChatClient chatClient(OpenAiChatModel model, ChatMemory chatMemory) {
        ChatClient chatclient = ChatClient
                .builder(model)
                .defaultSystem(SystemConstants.CHAT_SYSTEM_PROMPT)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),//配置日志Advisor
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
        return chatclient;//构建ChatClient实例
    }
}
