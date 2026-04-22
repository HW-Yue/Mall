package com.yue.opsagent.adapter.strategy;

import com.yue.opsagent.agent.NacosManagerAgent;
import com.yue.opsagent.config.AgentSystemPromptProperties;
import com.yue.opsagent.config.ApprovalProperties;
import com.yue.opsagent.config.ApprovalProperties.StrategyProps;
import com.yue.opsagent.domain.model.AlertEvent;
import com.yue.opsagent.domain.port.NacosConfigPort;
import org.springframework.stereotype.Component;

/**
 * Sentinel 流控规则策略：处理 {@code category=sentinel} 的告警。
 *
 * <ul>
 *   <li>dataId：{@code ${app}-flow-rules.json}，group：{@code SENTINEL_GROUP}</li>
 *   <li>Agent name：{@code sentinel-ops}，使用 {@code ops-agent.prompts.sentinel}</li>
 *   <li>Agent 最终调用 {@code publishConfig} 进审批队列，人工同意后才会真正写入 Nacos</li>
 * </ul>
 */
@Component
public class SentinelFlowRuleStrategy extends AbstractNacosWriteStrategy {

    public SentinelFlowRuleStrategy(ApprovalProperties approvalProps,
                                    AgentSystemPromptProperties promptProps,
                                    NacosConfigPort configPort,
                                    NacosManagerAgent agentFactory) {
        super(approvalProps, promptProps, configPort, agentFactory);
    }

    @Override
    public String domain() {
        return "sentinel";
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    protected boolean matches(AlertEvent event) {
        return "sentinel".equalsIgnoreCase(event.category());
    }

    @Override
    protected String resolveDataId(AlertEvent event, StrategyProps props) {
        String template = props.getDataIdTemplate();
        if (template == null || template.isBlank()) {
            template = "${app}-flow-rules.json";
        }
        return template.replace("${app}", event.application());
    }

    @Override
    protected String buildPromptBody(AlertEvent event,
                                     String alertKey,
                                     String dataId,
                                     String group,
                                     String currentContent) {
        String alertJson = toJson(event);
        String rules = currentContent == null
                ? "[]  // 当前 Nacos 里未找到该 dataId，首次下发请包含完整规则集"
                : currentContent;

        return """
                现在收到一条 Prometheus 告警：

                %s

                该告警属于 Sentinel 限流类，触发键（alertKey）=「%s」。
                当前 Nacos 中的流控规则（dataId=%s, group=%s）：

                %s

                请严格按以下步骤处理：
                1) 判断该 resource 的当前 QPS 阈值是否需要临时放大，或是否需要新增 / 调整 rule；
                2) 给出建议值，遵循"保守原则"：提升 ≤ 2x，单次提升 ≤ 此前值 + 500；
                3) 调用 publishConfig，参数：
                     - dataId   = "%s"
                     - group    = "%s"
                     - content  = 完整新 JSON 数组（保留其他规则不变）
                     - reasoning      = before/after 对比 + 告警里的 block QPS
                     - source_alert_key = "%s"
                4) publishConfig 会进入人工审批（不会立刻生效），框架会自动把本轮对话挂起。
                   不要因"等不到确认"就重复调用 publishConfig，也不要调用 deleteConfig。

                只给出最终工具调用，不需要额外解释。
                """.formatted(
                        alertJson,
                        alertKey,
                        dataId,
                        group,
                        rules,
                        dataId,
                        group,
                        alertKey);
    }
}
