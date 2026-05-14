package com.yue.opsagent.springai.agent.parent;

import com.yue.opsagent.springai.agent.AgentContextHolder;
import com.yue.opsagent.springai.agent.registry.AgentToolRegistry;
import com.yue.opsagent.springai.agent.react.ReactAgentSpec;
import com.yue.opsagent.springai.agent.react.ReactRunResult;
import com.yue.opsagent.springai.agent.react.ReactRunner;
import com.yue.opsagent.springai.domain.alert.AlertEvent;
import com.yue.opsagent.springai.domain.alert.EnrichedAlertContext;
import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警运维父 Agent：system 注入 SOP 正文（纯文本），工具委派七子域；线程内通过 {@link AgentContextHolder} 传递告警上下文。
 */
@Component
public class OpsAgent {

    private static final Logger log = LoggerFactory.getLogger(OpsAgent.class);

    private final AgentToolRegistry agentToolRegistry;
    private final AgentContextHolder contextHolder;
    private final ReactRunner reactRunner;
    private final int maxParentIters;

    public OpsAgent(
            AgentToolRegistry agentToolRegistry,
            AgentContextHolder contextHolder,
            ReactRunner reactRunner,
            OpsAiProperties props) {
        this.agentToolRegistry = agentToolRegistry;
        this.contextHolder = contextHolder;
        this.reactRunner = reactRunner;
        this.maxParentIters = props.getReact().getMaxParentIters();
    }

    /**
     * 单次告警 + 匹配到的 SOP 规则（含 sopMarkdown）；在 finally 中清理线程上下文。
     */
    public ReactRunResult runForAlert(AlertEvent event, EnrichedAlertContext enrichment, OpsAiProperties.Sop.Rule rule) {
        Map<String, Object> ctx = baseContext(event);
        ctx.put("enrichment", enrichment == null ? EnrichedAlertContext.empty() : enrichment);
        ctx.put("sopMarkdown", nullToEmpty(rule.getSopMarkdown()));
        log.info("[OpsAgent] 开始处理告警 alertname={} application={} labels={} annotations={}",
                event.alertname(),
                event.application(),
                event.labels(),
                event.annotations());
        return runReact(
                "OpsAgent",
                "ops-agent-react",
                buildAlertSystemPrompt(rule),
                buildAlertUserMessage(event, enrichment),
                ctx,
                event.alertname());
    }

    public ReactRunResult runForText(
            String text,
            AlertEvent event,
            EnrichedAlertContext enrichment,
            Map<String, Object> matchData,
            String sopMarkdown) {
        Map<String, Object> ctx = baseContext(event);
        ctx.put("textInput", nullToEmpty(text));
        ctx.put("match", matchData == null ? Map.of("matched", false) : new HashMap<>(matchData));
        ctx.put("enrichment", enrichment == null ? EnrichedAlertContext.empty() : enrichment);
        ctx.put("sopMarkdown", nullToEmpty(sopMarkdown));
        log.info("[OpsAgent] 开始处理文本预警 textChars={} primaryService={} matched={}",
                text == null ? 0 : text.length(),
                enrichment == null ? "" : enrichment.primaryService(),
                matchData != null && Boolean.TRUE.equals(matchData.get("matched")));
        return runReact(
                "OpsTextAgent",
                "ops-text-react",
                buildTextSystemPrompt(),
                buildTextUserMessage(text, event, enrichment, matchData),
                ctx,
                event == null ? "PlainTextOpsRequest" : event.alertname());
    }

    private ReactRunResult runReact(
            String agentName,
            String traceName,
            String systemPrompt,
            String userMessage,
            Map<String, Object> ctx,
            String logKey) {
        contextHolder.set(ctx);
        try {
            ReactRunResult out = reactRunner.runDetailed(new ReactAgentSpec(
                    agentName,
                    traceName,
                    systemPrompt,
                    userMessage,
                    ctx,
                    agentToolRegistry.reactTools(contextHolder::mutableCopyForSubAgent),
                    maxParentIters));
            log.info("[OpsAgent] {} 本轮排查结束 replyChars={} converged={} finishReason={}（完整回复见上方 [LLM] 大模型回复）",
                    logKey,
                    out == null || out.answer() == null ? 0 : out.answer().length(),
                    out != null && out.converged(),
                    out == null ? "" : out.finishReason());
            return out;
        } finally {
            contextHolder.clear();
        }
    }

    private String buildAlertSystemPrompt(OpsAiProperties.Sop.Rule rule) {
        String sop = rule.getSopMarkdown() == null ? "" : rule.getSopMarkdown();
        return """
                你是运维编排 Agent，负责按标准作业程序（SOP）处理告警。目标是尽快给出可执行结论，而不是把所有工具跑一遍。
                你必须只输出一段合法 JSON（不要 markdown），格式二选一：
                1) {"action":"CALL_TOOL","tool":"<子Agent工具名>","args":{"task":"<委派给子Agent的明确任务>"}}
                2) {"action":"FINAL","answer":"<给用户的中文结论>"}

                调度原则：
                - 不要编造监控、日志、实例或配置内容；只使用工具返回的事实。
                - 先查“服务是否存在”：Prometheus 指标、Nacos 实例、Docker 容器。服务不存在或名称不一致时，立即结束。
                - 当证据已经足够支持结论时，必须 FINAL，不要继续查无关依赖。
                - 对子域工具一次只委派一个清晰任务。例如“查询 application=order-service 是否有 up/http 指标”，不要说“全面排查”。
                - CALL_TOOL 的 args 必须包含 task 字段。
                - 写操作必须由审批流处理；没有明确修复依据时不要提出写配置。

                快速终止规则：
                - Prometheus 查不到该 application/job 的任何时序，且 Nacos 查不到同名服务实例，且 Docker 查不到同名/近似容器：结论为服务未注册、未部署或告警标签名称错误。
                - Nacos 无健康实例但 Docker 有容器：结论优先为服务启动或注册失败，下一步看容器日志。
                - Prometheus 有指标、Nacos 有实例、Docker 有容器后，才继续查日志和依赖。

                可用委派工具（名称即函数名；子域内部会自行选择更具体的方法）：
                """
                + agentToolRegistry.buildMenu()
                + """

                ## 标准作业程序（SOP）

                """
                + sop;
    }

    private String buildTextSystemPrompt() {
        return """
                你是运维编排 Agent，负责处理文本预警或运维排查请求。目标是根据文本、服务归因、参考 SOP 和工具事实，自主规划最短排查路径。
                你必须只输出一段合法 JSON（不要 markdown），格式二选一：
                1) {"action":"CALL_TOOL","tool":"<子Agent工具名>","args":{"task":"<委派给子Agent的明确任务>"}}
                2) {"action":"FINAL","answer":"<给用户的中文结论>"}

                硬性步骤：
                - 对纯文本请求，第一条动作必须先调用 Catalog Skill，先查“当前有哪些服务”，再从服务清单里筛选候选服务。
                - 在拿到服务清单之前，不要直接调用 Docker、Prometheus、MySQL、RocketMQ、Nacos、Redis 或 Elasticsearch。
                - 如果文本只出现“下单链路”“支付链路”“退款链路”“拼团链路”“秒杀链路”等业务语义，不要把这些词直接当成服务名；必须先从服务清单里确定候选服务。
                - 筛出候选服务后，优先继续用 Catalog Skill 查看候选服务的 application、container、topic、database、pool，再进入具体排查。

                调度原则：
                - 如果上下文提供了 SOP，只把它当作参考材料，不要机械逐步执行。
                - 调用 Catalog Skill 时，优先把 task 说成“先列出当前服务名，并从中筛出与问题最相关的候选服务；必要时继续查看候选服务拓扑”。
                - 每次只把一个明确问题委派给一个子域，避免“全面排查”。
                - 先利用文本与上下文里的 primaryService、candidateServices 判断主服务，再决定先查指标、日志、Nacos、Docker 或依赖。
                - 没有命中 SOP 也必须继续排查，不要退回草案。
                - 工具结果已经足够支撑结论时，立即 FINAL。
                - 最终回复必须包含：结论、证据、下一步动作。证据不足时直接说明缺哪条证据。

                可用委派工具（名称即函数名；子域内部会自行选择更具体的方法）：
                """
                + agentToolRegistry.buildMenu();
    }

    private static String buildAlertUserMessage(AlertEvent event, EnrichedAlertContext enrichment) {
        return """
                请根据上述 SOP 与下列告警信息排查。
                要求：
                1. 先确认告警里的 application/service 是否真实存在，再分析依赖或容量。
                2. 如果 Prometheus 没有该服务指标、Nacos 没有该服务实例、Docker 也没有该服务容器或 inspect 不存在，立即给 FINAL：服务未注册/未部署/名称不一致，别继续查数据库、缓存、MQ。
                3. 每个子域只问一个明确问题，不要让子 Agent 做泛泛排查。
                4. 最终回复必须包含：结论、证据、下一步动作。证据不足时直接说缺哪条证据。

                """
                + "服务归因:\n"
                + "primaryService=" + (enrichment == null ? "" : nullToEmpty(enrichment.primaryService())) + '\n'
                + "candidateServices=" + (enrichment == null ? List.of() : enrichment.candidateServices()) + '\n'
                + "evidence=" + (enrichment == null ? Map.of() : enrichment.evidence()) + "\n\n"
                + formatAlert(event);
    }

    private static String buildTextUserMessage(
            String text,
            AlertEvent event,
            EnrichedAlertContext enrichment,
            Map<String, Object> matchData) {
        return """
                请根据下面的文本预警自主规划排查路径。
                如果有参考 SOP，可以吸收其中方向，但允许跳过不适用步骤或调整顺序。

                额外要求：
                1. 第一步先查当前有哪些服务，不要直接把“下单链路”“支付链路”等业务词当成服务名。
                2. 先从服务清单里筛出候选服务，再根据服务去查容器、指标、Nacos、MQ、数据库。

                文本预警：
                """
                + nullToEmpty(text)
                + """

                参考 SOP 匹配：
                """
                + (matchData == null ? Map.of("matched", false) : matchData)
                + """

                服务归因：
                primaryService=
                """
                + (enrichment == null ? "" : nullToEmpty(enrichment.primaryService()))
                + """
                candidateServices=
                """
                + (enrichment == null ? List.of() : enrichment.candidateServices())
                + """
                evidence=
                """
                + (enrichment == null ? Map.of() : enrichment.evidence())
                + """

                兼容告警上下文：
                """
                + formatAlert(event);
    }

    private static Map<String, Object> baseContext(AlertEvent event) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("alert", Map.of(
                "status", event == null ? "" : nullToEmpty(event.status()),
                "alertname", event == null ? "" : nullToEmpty(event.alertname()),
                "severity", event == null ? "" : nullToEmpty(event.severity()),
                "application", event == null ? "" : nullToEmpty(event.application()),
                "labels", event == null || event.labels() == null ? Map.of() : event.labels(),
                "annotations", event == null || event.annotations() == null ? Map.of() : event.annotations()));
        return ctx;
    }

    private static String formatAlert(AlertEvent e) {
        if (e == null) {
            return "status=\nalertname=\nseverity=\napplication=\nlabels={}\nannotations={}\n";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("status=").append(e.status()).append('\n');
        sb.append("alertname=").append(e.alertname()).append('\n');
        sb.append("severity=").append(e.severity()).append('\n');
        sb.append("application=").append(e.application()).append('\n');
        sb.append("labels=").append(e.labels()).append('\n');
        sb.append("annotations=").append(e.annotations()).append('\n');
        return sb.toString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
