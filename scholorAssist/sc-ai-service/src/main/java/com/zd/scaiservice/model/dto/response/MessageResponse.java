package com.zd.scaiservice.model.dto.response;


import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.Message;

@NoArgsConstructor
@Data
public class MessageResponse {
    private String role;
    private String content;
    public MessageResponse(Message message){
        switch (message.getMessageType()){
            case USER -> this.role = "user";
            case ASSISTANT -> this.role = "assistant";
            default -> this.role = "unknown";
        }
        this.content = message.getText();
    }
}
