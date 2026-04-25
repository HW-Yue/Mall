package com.yue.opsagent.springai.agent.parent;

import com.yue.opsagent.springai.agent.OrchestrationContextHolder;
import com.yue.opsagent.springai.agent.sub.ISubAgent;
import com.yue.opsagent.springai.alert.AlertEvent;
import com.yue.opsagent.springai.config.OpsAiProperties;
import com.yue.opsagent.springai.observability.LlmCallTracer;
import com.yue.opsagent.springai.observability.OpsLogFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警编排父 Agent：system 注入 SOP 正文（纯文本），工具委派七子域；线程内通过 {@link OrchestrationContextHolder} 传递告警上下文。
 */
@Component
public class OrchestratorReactAgent {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorReactAgent.class);

    private final ChatClient chatClient;
    private final OrchestrationContextHolder contextHolder;
    private final LlmCallTracer llmCallTracer;

    public OrchestratorReactAgent(
            ChatModel chatModel,
            List<ISubAgent> agents,
            OrchestrationContextHolder contextHolder,
            LlmCallTracer llmCallTracer) {
        this.contextHolder = contextHolder;
        this.llmCallTracer = llmCallTracer;
        List<ToolCallback> callbacks = new ArrayList<>();
        for (ISubAgent a : agents) {
            callbacks.add(FunctionToolCallback.builder(
                            a.parentToolName(),
                            (DelegateTask t) -> a.runReact(
                                    t.task() == null ? "" : t.task(),
                                    contextHolder.mutableCopyForSubAgent()))
                    .description(a.parentToolDescription())
                    .inputType(DelegateTask.class)
                    .build());
        }
        this.chatClient = ChatClient.builder(chatModel)
                .defaultToolCallbacks(callbacks)
                .build();
    }

    /**
     * 单次告警 + 匹配到的 SOP 规则（含 sopMarkdown）；在 finally 中清理线程上下文。
     */
    public String runForAlert(AlertEvent event, OpsAiProperties.Sop.Rule rule) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("alert", Map.of(
                "status", event.status() == null ? "" : event.status(),
                "alertname", event.alertname() == null ? "" : event.alertname(),
                "severity", event.severity() == null ? "" : event.severity(),
                "application", event.application() == null ? "" : event.application(),
                "labels", event.labels() == null ? Map.of() : event.labels(),
                "annotations", event.annotations() == null ? Map.of() : event.annotations()));
        ctx.put("sopMarkdown", rule.getSopMarkdown() == null ? "" : rule.getSopMarkdown());
        contextHolder.set(ctx);
        try {
            log.info("[Orchestrator] 开始处理告警 alertname={} application={} labels={} annotations={}",
                    event.alertname(),
                    event.application(),
                    event.labels(),
                    event.annotations());
            String system = buildOrchestratorSystem(rule);
            String user = """
                    请根据上述 SOP 与下列告警信息排查。
                    要求：
                    1. 先确认告警里的 application/service 是否真实存在，再分析依赖或容量。
                    2. 如果 Prometheus 没有该服务指标、Nacos 没有该服务实例、Docker 也没有该服务容器或 inspect 不存在，立即给 FINAL：服务未注册/未部署/名称不一致，别继续查数据库、缓存、MQ。
                    3. 每个子域只问一个明确问题，不要让子 Agent 做泛泛排查。
                    4. 最终回复必须包含：结论、证据、下一步动作。证据不足时直接说缺哪条证据。

                    """
                    + formatAlert(event);
            String outbound = "[system]\n"
                    + OpsLogFormatter.truncate(system, OpsLogFormatter.LLM_BODY_MAX) + "\n[user]\n"
                    + OpsLogFormatter.truncate(user, OpsLogFormatter.LLM_BODY_MAX);
            String out = llmCallTracer.chatText(
                    "orchestrator-react",
                    outbound,
                    () -> chatClient.prompt()
                            .system(system)
                            .user(user)
                            .call()
                            .chatResponse());
            log.info("[Orchestrator] 父 Agent 本轮编排结束 alertname={} replyChars={}（完整回复见上方 [LLM] 大模型回复）",
                    event.alertname(), out == null ? 0 : out.length());
            return out;
        } finally {
            contextHolder.clear();
        }
    }

    private static String buildOrchestratorSystem(OpsAiProperties.Sop.Rule rule) {
        String sop = rule.getSopMarkdown() == null ? "" : rule.getSopMarkdown();
        return """
                你是运维编排 Agent，负责按标准作业程序（SOP）处理告警。目标是尽快给出可执行结论，而不是把所有工具跑一遍。

                调度原则：
                - 不要编造监控、日志、实例或配置内容；只使用工具返回的事实。
                - 先查“服务是否存在”：Prometheus 指标、Nacos 实例、Docker 容器。服务不存在或名称不一致时，立即结束。
                - 当证据已经足够支持结论时，必须 FINAL，不要继续查无关依赖。
                - 对子域工具一次只委派一个清晰任务。例如“查询 application=order-service 是否有 up/http 指标”，不要说“全面排查”。
                - 写操作必须由审批流处理；没有明确修复依据时不要提出写配置。

                快速终止规则：
                - Prometheus 查不到该 application/job 的任何时序，且 Nacos 查不到同名服务实例，且 Docker 查不到同名/近似容器：结论为服务未注册、未部署或告警标签名称错误。
                - Nacos 无健康实例但 Docker 有容器：结论优先为服务启动或注册失败，下一步看容器日志。
                - Prometheus 有指标、Nacos 有实例、Docker 有容器后，才继续查日志和依赖。

                ## 标准作业程序（SOP）

                """
                + sop;
    }

    private static String formatAlert(AlertEvent e) {
        StringBuilder sb = new StringBuilder();
        sb.append("status=").append(e.status()).append('\n');
        sb.append("alertname=").append(e.alertname()).append('\n');
        sb.append("severity=").append(e.severity()).append('\n');
        sb.append("application=").append(e.application()).append('\n');
        sb.append("labels=").append(e.labels()).append('\n');
        sb.append("annotations=").append(e.annotations()).append('\n');
        return sb.toString();
    }
}
