package com.zd.scaiservice.controller.inner;

import com.zd.scaiservice.service.AiDocumentService;
import com.zd.scapi.api.ai.AiDocumentApi;
import com.zd.sccommon.model.Result;
import com.zd.sccommon.utils.UserContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/inner/ai/document")
@RequiredArgsConstructor
public class InnerAiDocumentController implements AiDocumentApi {

    private final AiDocumentService aiDocumentService;

    @Override
    @PostMapping("/embedding")
    public Result<Boolean> processDocumentEmbedding(String filePath, String documentId) {
        log.info("收到内部调用请求：处理文档Embedding, path: {}", filePath);

        Boolean success = aiDocumentService.processEmbedding(filePath, documentId);

        return Result.ok(success);
    }
}