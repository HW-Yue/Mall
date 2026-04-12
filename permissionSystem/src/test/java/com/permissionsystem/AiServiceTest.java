package com.permissionsystem;

import com.permissionsystem.service.FunctionAssistant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@AutoConfigureMockMvc
@SpringBootTest
public class AiServiceTest {

    // 注入你定义的 AI 助手
    @Autowired
    private FunctionAssistant assistant;

    @Autowired
    private MockMvc mockMvc;

    @Test
        // 标记这是一个测试方法
    void should_get_roles_from_tool() {
        // 确保 assistant Bean 已经被成功创建和注入
        assertNotNull(assistant);

        // 模拟一个用户请求，这个请求会触发工具调用
        String userMessage = "帮我查询一下 ID 为 117 的用户角色。";

        // 调用 AI 助手
        String aiResponse = assistant.chat(userMessage);

        // 打印大模型的响应，以便观察和验证
        System.out.println("AI Response: " + aiResponse);

        // 这里你可以根据实际情况添加断言，来验证响应是否符合预期
        // 例如：assertTrue(aiResponse.contains("角色信息"));
    }


    @Test
    void shouldReturnDefaultResponse() throws Exception {
        // 模拟对 /gemini 接口的 GET 请求，不带参数
        mockMvc.perform(get("/chat/gemini"))
                .andExpect(status().isOk()) // 期望 HTTP 状态码为 200 OK
                .andExpect(content().string("你是一个助手")) // 期望返回的字符串是 "你是一个助手"
                .andReturn();
    }

    @Test
    void shouldReturnCustomResponse() throws Exception {
        // 模拟对 /gemini 接口的 GET 请求，并带上 prompt 参数
        String prompt = "你好";
        MvcResult result = mockMvc.perform(get("/chat/gemini")
                        .param("prompt", prompt))
                .andExpect(status().isOk()) // 期望 HTTP 状态码为 200 OK
                .andReturn();

        // 获取并验证响应结果
        String content = result.getResponse().getContentAsString();
        // 因为大模型返回的内容不确定，这里我们只验证它不是空的
        assertTrue(content.length() > 0);
        // 如果你期望特定的关键词，也可以这样验证：
        // assertTrue(content.contains("你好"));
    }

}


