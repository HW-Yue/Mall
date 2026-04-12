package com.permissionsystem.config;
import com.permissionsystem.service.FunctionAssistant;
import com.permissionsystem.tools.UserTools;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.internal.OpenAiClient;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class LLMConfig
{
//    @Bean
//    public ChatModel chatModel()
//    {
//        return OpenAiChatModel.builder()
//                .apiKey(System.getenv("aliQwen-api"))
//                .modelName("qwen-max")
//                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
//                .build();
//    }
//    @Bean
//    public ChatModel chatModel()
//    {
//        return OpenAiChatModel.builder()
//                .apiKey(System.getenv("GEMINI_API_KEY"))
//                .modelName("gemini-2.5-flash")
//                .build();
//    }


    //流式输出
    @Bean
    public StreamingChatModel streamingChatModel(){
        return OpenAiStreamingChatModel.builder()
                .apiKey(System.getenv("aliQwen-api"))
                .modelName("qwen-max")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .build();
    }
    //流式输出
//    @Bean
//    public StreamingChatModel streamingChatModel(){
//        return GoogleAiGeminiStreamingChatModel.builder()
//                .apiKey(System.getenv("GEMINI_API_KEY"))
//                .modelName("gemini-2.5-flash")
//                        .build();
//    }
//    @Bean
//    public StreamingChatModel streamingChatModel(){
//        return OpenAiStreamingChatModel.builder()
//                .apiKey(System.getenv("OPENAI_API_KEY"))
//                .modelName("GPT_5_MINI")
//                .build();
//    }

    // 3. RAG 能力 - 向量存储
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel) {
        return QdrantEmbeddingStore.builder()
                .host("106.12.79.165")
                .port(6334)
                .collectionName("test-qdrant")
                .apiKey("123321")
                .build();
    }
    // 4. Function Calling 能力 - 工具类
    @Bean
    public FunctionAssistant functionAssistant(
            StreamingChatModel streamingchatModel
//            ,ChatModel chatModel
            ,UserTools userTools
            ,EmbeddingStore<TextSegment> embeddingStore
            ,EmbeddingModel embeddingModel)
    {
        return AiServices.builder(FunctionAssistant.class)
                .streamingChatModel(streamingchatModel)
//                .chatModel(chatModel)
                .tools(userTools)
                // 注意每个memoryId对应创建一个ChatMemory
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(100))
                .contentRetriever(EmbeddingStoreContentRetriever.builder()
                        .embeddingStore(embeddingStore)
                        .embeddingModel(embeddingModel)
                        .build())
                .build();
    }

    // 2. RAG 能力 - 嵌入模型
    @Bean
    public EmbeddingModel embeddingModel()
    {
        return OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("aliQwen-api"))
                .modelName("text-embedding-v3")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .dimensions(1024)
                .build();
    }

    /**
     * 创建Qdrant客户端
     * @return
     */
    @Bean
    public QdrantClient qdrantClient() {
        QdrantGrpcClient.Builder grpcClientBuilder =
                QdrantGrpcClient.newBuilder("106.12.79.165", 6334, true);
        grpcClientBuilder.withApiKey("123321");
        return new QdrantClient(grpcClientBuilder.build());
    }







//    @Bean
//    public List<ToolSpecification> toolSpecifications() {
//        // 这行代码在这里执行，自动扫描并生成工具规范列表
//        return ToolSpecifications.toolSpecificationsFrom(UserTools.class);
//    }
}