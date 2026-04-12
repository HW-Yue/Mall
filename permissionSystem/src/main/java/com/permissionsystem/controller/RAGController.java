package com.permissionsystem.controller;

import com.permissionsystem.dto.ConversationRequestDTO;
import com.permissionsystem.dto.Result;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rag")
public class RAGController {

    @Resource
    EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    EmbeddingModel embeddingModel;

    @PostMapping("/knowledge")
    public Result putRAG(@RequestBody String content)
    {
        //从请求中获得用户输入信息
        String userMessage=content;

        // 1. 将字符串转换为 Document 对象
        Document document = Document.from(userMessage);

        // 创建数据摄入器，它负责将文本向量化并存储
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(500, 100))
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build();

        // 3. 执行摄入
        ingestor.ingest(document);

        return Result.success();
    }
}
