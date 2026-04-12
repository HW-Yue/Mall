package com.permissionsystem.service;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

import java.util.List;

public interface FunctionAssistant {

    String chat(String prompt);
//    @SystemMessage("你是一个企业用户权限管理系统的智能客服,具有Function Calling功能,用户需要你查询的时候," +
//            "不要说你在调用什么参数,或者说正在查询,直接给用户结果就行了" +
//            "当用户需要执行特定操作查询时，优先考虑使用工具。如果问题只是关于知识性的，才使用 RAG 知识库。" +
//            "你有以下职责:" +
//            "1. 当用户需要根据账号查询用户角色信息,调用getRoles工具,参数名是username" +
//            "2. 当用户需要根据账号查询用户权限信息,调用getPermissions工具,参数名是username" +
//            "3.当用户根据角色昵称,查询角色的权限信息,调用getPermissionsByRoleName工具,参数名是rolename" )
    Flux<String> chatFlux(@MemoryId String conversationId ,@UserMessage String userMessage);
}
