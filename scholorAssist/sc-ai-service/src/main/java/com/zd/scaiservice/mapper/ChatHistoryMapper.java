package com.zd.scaiservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zd.scaiservice.model.domain.springAiChatMemory;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatHistoryMapper extends BaseMapper<springAiChatMemory> {

    @Delete("DELETE FROM spring_ai_chat_memory WHERE conversation_id = #{chatId}")
    void deleteByConversationId(@Param("chatId") String chatId);
}