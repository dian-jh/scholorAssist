package com.zd.scaiservice.service.impl;

import com.zd.scaiservice.service.AiDocumentService;
import com.zd.sccommon.common.BusinessException;
import com.zd.sccommon.utils.UserContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiDocumentServiceImpl implements AiDocumentService {
    private final VectorStore vectorStore;

    // --- 1. 权重映射表 (定义论文的标准顺序) ---
    private static final Map<String, Integer> SECTION_WEIGHTS = new HashMap<>();
    static {
        // 0-9: 前置部分
        SECTION_WEIGHTS.put("abstract", 0);
        // 10-19: 引入
        SECTION_WEIGHTS.put("introduction", 10);
        SECTION_WEIGHTS.put("background", 12);
        SECTION_WEIGHTS.put("related work", 15);
        SECTION_WEIGHTS.put("related works", 15);
        // 20-29: 方法论
        SECTION_WEIGHTS.put("method", 20);
        SECTION_WEIGHTS.put("methods", 20);
        SECTION_WEIGHTS.put("methodology", 20);
        SECTION_WEIGHTS.put("proposed method", 20);
        SECTION_WEIGHTS.put("approach", 20);
        SECTION_WEIGHTS.put("materials and methods", 20);
        // 30-39: 实验
        SECTION_WEIGHTS.put("experiment", 30);
        SECTION_WEIGHTS.put("experiments", 30);
        SECTION_WEIGHTS.put("experimental evaluation", 30);
        SECTION_WEIGHTS.put("experimental results", 30);
        SECTION_WEIGHTS.put("results", 32);
        SECTION_WEIGHTS.put("analysis", 34);
        SECTION_WEIGHTS.put("discussion", 35);
        // 40-49: 结论
        SECTION_WEIGHTS.put("conclusion", 40);
        SECTION_WEIGHTS.put("conclusions", 40);
        SECTION_WEIGHTS.put("future work", 42);
        // 99: 参考文献 (通常会被拦截，防守用)
        SECTION_WEIGHTS.put("reference", 99);
        SECTION_WEIGHTS.put("references", 99);
    }

    // --- 2. 正则规则优化 (移除 MULTILINE) ---
    private static final Pattern SECTION_TITLE_PATTERN = Pattern.compile(
            "^(\\d+(\\.\\d+)*\\s+)?(Abstract|Introduction|Related Works?|Background|Method(?:ology|s)?|Materials? and Methods?|Experiments?|Results?|Discussion|Conclusions?|Future Work|References)$",
            Pattern.CASE_INSENSITIVE // 移除 MULTILINE，因为我们是逐行扫描
    );

    private static final Pattern REFERENCE_PATTERN = Pattern.compile(
            "^(References|Bibliography)$",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public Boolean processEmbedding(String filePath, String documentId) {
        String userId = UserContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户信息缺失");
        }

        log.info("开始智能切分论文(本地文件模式), Path: {}, DocumentId: {}", filePath, documentId);

        File pdfFile = new File(filePath);
        if (!pdfFile.exists() || !pdfFile.isFile()) {
            log.error("文件不存在: {}", filePath);
            throw new BusinessException(500, "目标文件不存在，无法进行向量化");
        }

        // 使用 Loader.loadPDF(File) 直接读取文件，比流更高效
        try (PDDocument document = Loader.loadPDF(pdfFile)) {

            // 1. 提取文本 (开启 SortByPosition 解决双栏乱序)
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String fullText = stripper.getText(document);

            // 2. 智能结构化切分 (逻辑保持之前的代码不变)
            List<Document> semanticChunks = parsePaperStructure(fullText, userId, documentId);

            // 3. 二次切分 (逻辑保持之前的代码不变)
            List<Document> finalChunks = splitLargeChunks(semanticChunks);

            // 4. 存入向量库
            vectorStore.add(finalChunks);

            String anchorQuery = generateAnchorQuery(fullText, documentId); // 自定义方法

            Map<String, Object> anchorMetadata = new HashMap<>();
            anchorMetadata.put("user_id", userId);
            anchorMetadata.put("document_id", documentId);
            anchorMetadata.put("type", "anchor"); // 特殊标记，方便后续识别

            Document anchorDoc = new Document(anchorQuery, anchorMetadata);
            vectorStore.add(List.of(anchorDoc));

            log.info("论文处理完成, 生成向量切片数: {}", finalChunks.size());
            return true;

        } catch (IOException e) {
            log.error("PDF解析失败, path: {}", filePath, e);
            throw new BusinessException(500, "PDF解析失败: " + e.getMessage());
        }
    }

    private String generateAnchorQuery(String fullText, String documentId) {
        String title = extractTitle(fullText);
        String abstractSnippet = extractAbstractSnippet(fullText);

        return String.format("""
        Paper Title: %s
        Abstract Summary: %s
        """,
                title != null ? title.trim() : "Unknown Academic Paper",
                abstractSnippet != null ? abstractSnippet.trim() : ""
        ).replaceAll("\\s+", " "); // 去重空格
    }

    // 简单提取标题（第一行大写或包含数字的）
    private String extractTitle(String text) {
        String[] lines = text.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() > 10 && trimmed.length() < 200 &&
                    (trimmed.matches(".*[A-Z].{10,}.*") || trimmed.contains(":"))) {
                return trimmed;
            }
        }
        return "academic paper on computer vision and object detection";
    }

    // 提取摘要前200字
    private String extractAbstractSnippet(String text) {
        int abstractEnd = text.length() > 1000 ? 1000 : text.length();
        String snippet = text.substring(0, abstractEnd);
        return snippet.replaceAll("\\s+", " ");
    }

    /**
     * 核心方法：基于状态机 + 权重约束的解析
     */
    private List<Document> parsePaperStructure(String text, String userId, String documentId) {
        List<Document> chunks = new ArrayList<>();
        String[] lines = text.split("\\r?\\n"); // 统一换行符

        StringBuilder currentBuffer = new StringBuilder();
        String currentSection = "Abstract"; // 初始状态
        int currentWeight = 0; // 初始权重

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;

            // --- 规则 A: 参考文献硬拦截 ---
            if (REFERENCE_PATTERN.matcher(trimmedLine).matches()) {
                log.info("达到参考文献区域，停止解析。");
                break;
            }

            // --- 规则 B: 标题识别与状态转移 ---
            if (trimmedLine.length() < 80) { // 长度预过滤
                Matcher matcher = SECTION_TITLE_PATTERN.matcher(trimmedLine);
                if (matcher.find()) {
                    // 提取核心标题词 (Group 3 是正则里的标题部分，Group 1 是数字编号)
                    // 例如 "1. Introduction" -> extracted="Introduction"
                    String extractedTitle = matcher.group(3);

                    // 计算新标题的权重
                    int newWeight = getSectionWeight(extractedTitle);

                    // 【核心优化】方向约束：禁止章节倒流
                    // 只有当 新权重 >= 当前权重，或者当前还在 Abstract (权重0) 时，才允许切换
                    // 这样可以防止 "Conclusion" 里提到 "Introduction" 这个词时被误判回跳
                    if (newWeight >= currentWeight) {

                        // 1. 结算上一章
                        if (currentBuffer.length() > 0) {
                            chunks.add(createDocument(currentBuffer.toString(), currentSection, userId, documentId));
                            currentBuffer.setLength(0);
                        }

                        // 2. 状态转移
                        currentSection = extractedTitle; // 更新 Section 名
                        currentWeight = newWeight;       // 更新权重
                        log.debug("章节切换: {} (权重: {})", currentSection, currentWeight);

                        // 【核心优化】标题不写入正文 Buffer
                        // 标题只存在于 metadata 中，保持 content 纯净，避免 embedding 偏移
                        continue;
                    }
                }
            }

            // --- 普通内容 ---
            currentBuffer.append(trimmedLine).append("\n");
        }

        // 处理缓冲区剩余内容
        if (currentBuffer.length() > 0) {
            chunks.add(createDocument(currentBuffer.toString(), currentSection, userId, documentId));
        }

        return chunks;
    }

    /**
     * 辅助：获取章节权重，未知章节给予默认处理
     */
    private int getSectionWeight(String title) {
        if (title == null) return 0;
        String key = title.toLowerCase();
        // 如果完全匹配
        if (SECTION_WEIGHTS.containsKey(key)) {
            return SECTION_WEIGHTS.get(key);
        }
        // 模糊匹配 (处理复数、空格等细微差异)
        for (Map.Entry<String, Integer> entry : SECTION_WEIGHTS.entrySet()) {
            if (key.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return -1; // 未知章节，不进行跳转，或者你可以设置为 currentWeight 保持不变
    }

    private Document createDocument(String content, String section, String userId, String documentId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("user_id", userId);
        metadata.put("document_id", documentId);
        metadata.put("section", section); // Metadata 才是标题最好的归宿
        metadata.put("source_type", "paper");
        return new Document(content, metadata);
    }

    private List<Document> splitLargeChunks(List<Document> semanticChunks) {
        // 【核心优化】调整切分参数
        // chunkSize: 800 (保持不变，适应大多数 LLM 窗口)
        // minChunkSize: 400 (稍微加大，避免产生太细碎的碎片)
        // overlap: 100 (大幅提升，从 10 -> 100，保证跨段落逻辑连贯)
        TokenTextSplitter splitter = new TokenTextSplitter(800, 400, 100, 10000, true);

        List<Document> result = new ArrayList<>();
        for (Document doc : semanticChunks) {
            List<Document> subChunks = splitter.split(List.of(doc));
            for (Document sub : subChunks) {
                // 继承父级 Section 元数据
                sub.getMetadata().putAll(doc.getMetadata());
            }
            result.addAll(subChunks);
        }
        return result;
    }
}