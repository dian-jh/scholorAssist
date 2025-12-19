package com.zd.scapi.api.ai;

import com.zd.sccommon.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "sc-ai-service", contextId = "aiDocumentApi") // contextId防止bean冲突
public interface AiDocumentApi {

    /**
     * 内部调用：处理文档嵌入（Embedding）
     * 将PDF切片并存入向量数据库
     *
     * @param documentId 文档ID
     * @return 处理结果
     */
    @PostMapping(value = "/inner/ai/document/embedding")
    Result<Boolean> processDocumentEmbedding(
            @RequestParam("filePath") String filePath,
            @RequestParam("documentId") String documentId
    );
}