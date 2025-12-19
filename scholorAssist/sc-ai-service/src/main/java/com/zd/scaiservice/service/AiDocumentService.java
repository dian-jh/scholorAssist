package com.zd.scaiservice.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * AI 文档处理业务接口
 */
public interface AiDocumentService {

    /**
     * 处理文档嵌入（Embedding）
     * 包括：读取PDF -> 切片 -> 注入元数据 -> 存入向量库
     *
     * @param documentId 文档ID
     * @return 是否成功
     */
    Boolean processEmbedding(String filePath, String documentId);
}