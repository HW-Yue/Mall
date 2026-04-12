package cn.bugstack.ai.test.spring.ai;

import com.alibaba.fastjson.JSON;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;

/**
 * mcp；https://sai.baidu.com/zh/
 * 百度搜索；<a href="https://sai.baidu.com/zh/detail/e014c6ffd555697deabf00d058baf388">https://sai.baidu.com/zh/detail/e014c6ffd555697deabf00d058baf388</a>
 * key申请；<a href="https://console.bce.baidu.com/iam/?_=1753597622044#/iam/apikey/list">apikey</a>
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/27 14:29
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class AiSearchMCPTest {

    @Test
    public void test() {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl("https://apis.itedus.cn")
                        .apiKey("sk-j6n4nPXRP4KeSP4U3d67272c198042278dAfF262E9Ea1cFa")
                        .completionsPath("v1/chat/completions")
                        .embeddingsPath("v1/embeddings")
                        .build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("gpt-4.1")
                        .toolCallbacks(new SyncMcpToolCallbackProvider(sseMcpClient()).getToolCallbacks())
                        .build())
                .build();

        ChatResponse call = chatModel.call(Prompt.builder().messages(new UserMessage("搜索小傅哥技术博客有哪些项目")).build());
        log.info("测试结果:{}", JSON.toJSONString(call.getResult()));
    }

    /**
     * g-search-mcp：通过 stdio 启动，内部使用 Playwright 控制本地 Chromium 做 Google 搜索。
     * 需要本机已安装 Node/npx，首次运行会拉取 g-search-mcp 包。
     *
     * 若出现 Google「异常流量 / reCAPTCHA」页面：
     * 1. g-search-mcp 会切换到可见浏览器，请在弹出的 Chrome 窗口里勾选「I'm not                      a robot」并完成验证；
     * 2. 验证通过后脚本会继续，无需改代码；
     * 3. 若希望避免验证，可改用 {@link #testSearchWithBaiduMcp()} 百度 MCP（走 API，无验证码）。
     */
    @Test
    public void testGSearchMcpWithLocalBrowser() {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl("https://apis.itedus.cn")
                        .apiKey("sk-j6n4nPXRP4KeSP4U3d67272c198042278dAfF262E9Ea1cFa")
                        .completionsPath("v1/chat/completions")
                        .embeddingsPath("v1/embeddings")
                        .build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("gpt-4.1")
                        .toolCallbacks(new SyncMcpToolCallbackProvider(stdioMcpClientGSearch()).getToolCallbacks())
                        .toolChoice("required")
                        .build())
                .build();

        ChatResponse call = chatModel.call(Prompt.builder()
                .messages(new UserMessage("请用 Google 搜索「小傅哥 技术博客」并简要总结前 3 条结果。"))
                .build());
        log.info("g-search-mcp 测试结果: {}", JSON.toJSONString(call.getResult()));
    }

    /**
     * 使用百度 MCP 做搜索（走百度 API，无 reCAPTCHA，适合自动化/CI）。
     * 当 g-search-mcp 因 Google 验证码无法完成时，可用本测试验证「AI + 搜索 MCP」流程。
     */
    @Test
    public void testSearchWithBaiduMcp() {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl("https://apis.itedus.cn")
                        .apiKey("sk-j6n4nPXRP4KeSP4U3d67272c198042278dAfF262E9Ea1cFa")
                        .completionsPath("v1/chat/completions")
                        .embeddingsPath("v1/embeddings")
                        .build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("gpt-4.1")
                        .toolCallbacks(new SyncMcpToolCallbackProvider(sseMcpClient()).getToolCallbacks())
                        .build())
                .build();

        ChatResponse call = chatModel.call(Prompt.builder()
                .messages(new UserMessage("搜索小傅哥技术博客有哪些项目，并简要总结前 3 条。"))
                .build());
        log.info("百度 MCP 测试结果: {}", JSON.toJSONString(call.getResult()));
    }

    /**
     * 使用 stdio 连接 g-search-mcp（npx -y g-search-mcp），本地浏览器内核方式搜索。
     */
    public McpSyncClient stdioMcpClientGSearch() {
        var stdioParams = ServerParameters.builder("npx.cmd")
                .args("-y", "g-search-mcp","--debug")
                .build();

        McpSyncClient mcpSyncClient = McpClient.sync(new StdioClientTransport(stdioParams))
                .requestTimeout(Duration.ofSeconds(120))
                .build();

        var init = mcpSyncClient.initialize();
        log.info("g-search-mcp Stdio MCP Initialized: {}", init);
        var tools = mcpSyncClient.listTools();
        log.info("当前 MCP Server 提供的工具列表: {}", tools);
        return mcpSyncClient;
    }

    public McpSyncClient sseMcpClient() {
        HttpClientSseClientTransport sseClientTransport = HttpClientSseClientTransport.builder("http://appbuilder.baidu.com/v2/ai_search/mcp/")
                .sseEndpoint("sse?api_key=bce-v3/ALTAK-8RimCYNN0Gefl0lJFIzSL/35c4df21c4c6c2c9dfd4661cfef2e6ad287040d3")
                .build();

        McpSyncClient mcpSyncClient = McpClient.sync(sseClientTransport).requestTimeout(Duration.ofMinutes(360)).build();
        var init_sse = mcpSyncClient.initialize();
        log.info("Tool SSE MCP Initialized {}", init_sse);

        return mcpSyncClient;
    }

}
