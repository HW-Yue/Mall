package com.permissionsystem.controller;

import com.permissionsystem.dto.ConversationRequestDTO;
import com.permissionsystem.dto.ConversationResponseDTO;
import com.permissionsystem.dto.Reply;
import com.permissionsystem.dto.Result;
import com.permissionsystem.service.ChatService;
import com.permissionsystem.service.FunctionAssistant;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.Get;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    FunctionAssistant functionAssistant;


    @PostMapping("/conversations")
    public ResponseEntity<Flux<String>> chat(@RequestBody ConversationRequestDTO request)
    {
        //从请求中获得用户输入信息
        String userMessage=request.getMessage().getContent();
        //从请求中获取会话ID
        String conversationId=request.getConversationId();
        //如果会话ID为空，则创建一个会话ID
        if(conversationId==null||"".equals(conversationId)){
            conversationId=UUID.randomUUID().toString();
            System.out.println("---------------------------------------"+conversationId);
        }
//        //调用函数,获取ai返回的信息
//        String content=functionAssistant.chat(userMessage);
//
//        //创建一个回复对象
//        ConversationResponseDTO responseDTO=new ConversationResponseDTO();
//        //将content装入回复对象中
//        responseDTO.getReplies().add(new Reply("text",content));
//        responseDTO.setConversationId(conversationId);
//
//        return Result.success(responseDTO);
//
        Flux<String> responseFlux = functionAssistant.chatFlux(conversationId,request.getMessage().getContent());
        return ResponseEntity.ok()
                .header("X-Conversation-Id", conversationId)
                .body(responseFlux);

    }


//    @GetMapping(value = "/gemini")
//    public String geminiCall(@RequestParam(value = "prompt", defaultValue = "你是谁") String prompt)
//    {
//        String result = functionAssistant.chat(prompt);
//
//        System.out.println("通过langchain4j调用模型返回结果：\n"+result);
//
//        return result;
//    }


}
