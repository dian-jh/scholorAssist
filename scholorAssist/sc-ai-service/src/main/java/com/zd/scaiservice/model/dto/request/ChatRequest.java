package com.zd.scaiservice.model.dto.request;

import lombok.Data;

@Data
public class ChatRequest {
    private String chatId;
    private String prompt;
    private String documentId;
}