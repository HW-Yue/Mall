package com.yue.opsagent.adapter.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.opsagent.agent.DiagnoseAgent;
import com.yue.opsagent.config.AgentSystemPromptProperties;
import com.yue.opsagent.config.ApprovalProperties;
import com.yue.opsagent.config.MappingProperties;
import com.yue.opsagent.domain.model.AlertEvent;
import com.yue.opsagent.domain.port.ApprovalInboxPort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Redis 诊断策略：处理 {@code category=redis} 的告警。
 *
 * <p>Redis 根因多在业务代码（热点 key / 大 key / 慢命令 / 连接泄漏），
 * 不适合直接改 Nacos 配置。本策略借 {@link AbstractDiagnoseAgentStrategy}
 * 把告警异步转给 {@link DiagnoseAgent}，子 Agent 通过 ES MCP 查 {@code nexus-<app>-*}
 * 索引里的 traceId / Redis command 关键字，产出四段式诊断文本（根因假设 /
 * 责任服务 / 排查动作 / 风险等级），最终以 reasoning 字段回填到审批 inbox。</p>
 *
 * <p>白名单在 {@code ops-agent.approval.strategies.redis.alerts} 配置；
 * system prompt 在 {@code ops-agent.prompts.redis-diagnose}（由基类按
 * {@link #diagnosePromptKey()} 读取）。</p>
 */
@Component
public class RedisNotifyStrategy extends AbstractDiagnoseAgentStrategy {

    private final MappingProperties mappingProps;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RedisNotifyStrategy(ApprovalProperties approvalProps,
                               ApprovalInboxPort inbox,
                               AgentSystemPromptProperties promptProps,
                               DiagnoseAgent diagnoseAgent,
                               MappingProperties mappingProps) {
        super(approvalProps, inbox, promptProps, diagnoseAgent);
        this.mappingProps = mappingProps;
    }

    @Override
    public String domain() {
        return "redis";
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    protected boolean matches(AlertEvent event) {
        return "redis".equalsIgnoreCase(event.category());
    }

    @Override
    protected String buildDiagnosePrompt(AlertEvent event, String alertKey) {
        Map<String, String> m = mappingProps.getClientNameToApp();
        String clientMapping = (m == null || m.isEmpty())
                ? "（未配置 client-name 映射；请按 nexus-*-YYYY.MM.dd 全索引搜索）"
                : m.entrySet().stream()
                        .map(e -> "  - " + e.getKey() + " → " + e.getValue())
                        .collect(Collectors.joining("\n"));

        String alertJson = toJson(event);

        return """
                请对以下 Redis 告警做根因定位：

                【告警】alertKey=%s
                ```json
                %s
                ```

                【client-name → 服务名 反查表】
                %s

                【工具使用建议】
                1. 如果 labels 带 {service}/{application}，优先用索引 nexus-{service}-YYYY.MM.dd；
                   否则按 nexus-*-YYYY.MM.dd 模式配合 list_indices 枚举。
                2. 时间窗建议：告警 startsAt 的前 5 分钟 ~ 告警触发时刻。
                3. 关键字段过滤：level=ERROR/WARN、message 含 "redis" / "Redisson" / client-name，
                   同时带回 trace-id 用于跨服务追踪。
                4. 如需结构化聚合，可改用 esql 统计 service x message 的 top 10。

                请输出结论，严格遵循【根因假设 | 责任服务 | 排查动作 | 风险等级】四段结构。
                """.formatted(alertKey, alertJson, clientMapping);
    }

    private String toJson(AlertEvent event) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(event);
        } catch (Exception ex) {
            return String.valueOf(event);
        }
    }
}
