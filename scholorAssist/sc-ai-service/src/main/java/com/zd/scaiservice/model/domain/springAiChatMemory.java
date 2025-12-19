package com.zd.scaiservice.model.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("spring_ai_chat_memory")
public class springAiChatMemory implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "conversation_id")
    private String conversationId;

    @TableField(value = "content")
    private String content;

    @TableField(value = "type")
    private String type;

    @TableField(value = "timestamp")
    private LocalDateTime timestamp;
}