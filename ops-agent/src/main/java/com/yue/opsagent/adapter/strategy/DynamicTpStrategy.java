package com.yue.opsagent.adapter.strategy;

import com.yue.opsagent.agent.NacosManagerAgent;
import com.yue.opsagent.config.AgentSystemPromptProperties;
import com.yue.opsagent.config.ApprovalProperties;
import com.yue.opsagent.config.ApprovalProperties.StrategyProps;
import com.yue.opsagent.domain.model.AlertEvent;
import com.yue.opsagent.domain.port.NacosConfigPort;
import org.springframework.stereotype.Component;

/**
 * DynamicTP 线程池策略：处理 {@code category=dynamictp} 的告警。
 *
 * <ul>
 *   <li>dataId：{@code ${app}-dtp-${profile}.yml}（profile 由 {@code strategies.dtp.extra.profile} 决定，默认 {@code dev}）</li>
 *   <li>group：{@code DEFAULT_GROUP}</li>
 *   <li>Agent name：{@code dtp-ops}，使用 {@code ops-agent.prompts.dtp}</li>
 * </ul>
 */
@Component
public class DynamicTpStrategy extends AbstractNacosWriteStrategy {

    private static final String DEFAULT_PROFILE = "dev";

    public DynamicTpStrategy(ApprovalProperties approvalProps,
                             AgentSystemPromptProperties promptProps,
                             NacosConfigPort configPort,
                             NacosManagerAgent agentFactory) {
        super(approvalProps, promptProps, configPort, agentFactory);
    }

    @Override
    public String domain() {
        return "dtp";
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    protected boolean matches(AlertEvent event) {
        return "dynamictp".equalsIgnoreCase(event.category());
    }

    @Override
    protected String resolveDataId(AlertEvent event, StrategyProps props) {
        String template = props.getDataIdTemplate();
        if (template == null || template.isBlank()) {
            template = "${app}-dtp-${profile}.yml";
        }
        String profile = props.getExtra() == null
                ? DEFAULT_PROFILE
                : props.getExtra().getOrDefault("profile", DEFAULT_PROFILE);
        return template
                .replace("${app}", event.application())
                .replace("${profile}", profile);
    }

    @Override
    protected String buildPromptBody(AlertEvent event,
                                     String alertKey,
                                     String dataId,
                                     String group,
                                     String currentContent) {
        String alertJson = toJson(event);
        String yaml = currentContent == null
                ? "# 当前 Nacos 里未找到该 dataId；请先调用 getConfig 确认，再下发完整 YAML"
                : currentContent;
        String executor = event.labels() == null ? null : event.labels().get("thread_pool_name");
        String executorLine = executor == null
                ? "labels.thread_pool_name 未提供，请根据 alertname / summary 推断"
                : "目标 executor = " + executor;

        return """
                现在收到一条 Prometheus 告警：

                %s

                该告警属于 DynamicTP 线程池类，触发键（alertKey）=「%s」。
                %s

                当前 Nacos 中的线程池配置（dataId=%s, group=%s，YAML）：

                %s

                请严格按以下步骤处理：
                1) 只修改该 executor 的 corePoolSize / maximumPoolSize / queueCapacity，
                   其他 executor 与结构字段保持不变；
                2) 扩容次序：先扩 queueCapacity（最多翻倍且 ≤ 2000），不足再提 maximumPoolSize
                   （单次 ≤ 2x，且 ≤ 之前值 + 20），corePoolSize 谨慎变动；
                3) 若 YAML 结构不确定，**先** 调用 getConfig 读取后再决定 content；
                4) 调用 publishConfig，参数：
                     - dataId   = "%s"
                     - group    = "%s"
                     - content  = 完整新 YAML 原文
                     - reasoning      = 涉及 executor 名 + before/after 数值 + 告警 active/queue 占用率
                     - source_alert_key = "%s"
                5) publishConfig 会进入人工审批（不会立刻生效），本轮对话会被自动挂起；
                   不要重复调用 publishConfig，也不要调用 deleteConfig。

                只给出最终工具调用，不需要额外解释。
                """.formatted(
                        alertJson,
                        alertKey,
                        executorLine,
                        dataId,
                        group,
                        yaml,
                        dataId,
                        group,
                        alertKey);
    }
}
