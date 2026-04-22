package com.yue.opsagent.adapter.strategy;

import com.yue.opsagent.agent.NacosManagerAgent;
import com.yue.opsagent.config.AgentSystemPromptProperties;
import com.yue.opsagent.config.ApprovalProperties;
import com.yue.opsagent.config.ApprovalProperties.StrategyProps;
import com.yue.opsagent.config.MappingProperties;
import com.yue.opsagent.config.MappingProperties.PoolTarget;
import com.yue.opsagent.domain.model.AlertEvent;
import com.yue.opsagent.domain.port.NacosConfigPort;
import org.springframework.stereotype.Component;

/**
 * Hikari 连接池调优策略：处理 {@code category=hikari} 的告警。
 *
 * <p>核心思路：业务服务的 {@code spring.datasource.hikari.pool-name} 全局唯一（命名表见
 * {@code ops-agent.mapping.pool-name-to-app}），Prometheus 上报时会以 {@code pool} 标签
 * 透传。策略用 {@code labels.pool} 反查映射表，精确定位要改的 Nacos dataId。</p>
 *
 * <ul>
 *   <li>dataId：来自 {@link MappingProperties.PoolTarget#getDataId()}</li>
 *   <li>group：strategies.hikari.group（默认 DEFAULT_GROUP），与 mapping 一致</li>
 *   <li>Agent name：{@code hikari-ops}，使用 {@code ops-agent.prompts.hikari}</li>
 * </ul>
 */
@Component
public class HikariTuningStrategy extends AbstractNacosWriteStrategy {

    private final MappingProperties mappingProps;

    public HikariTuningStrategy(ApprovalProperties approvalProps,
                                AgentSystemPromptProperties promptProps,
                                NacosConfigPort configPort,
                                NacosManagerAgent agentFactory,
                                MappingProperties mappingProps) {
        super(approvalProps, promptProps, configPort, agentFactory);
        this.mappingProps = mappingProps;
    }

    @Override
    public String domain() {
        return "hikari";
    }

    @Override
    public int order() {
        return 35;
    }

    @Override
    protected boolean matches(AlertEvent event) {
        return "hikari".equalsIgnoreCase(event.category());
    }

    /** Hikari 告警走 pool→app 反查，alert rule 里没有 application 标签，这里跳过校验。 */
    @Override
    protected boolean requireApplicationLabel() {
        return false;
    }

    @Override
    protected String resolveDataId(AlertEvent event, StrategyProps props) {
        String pool = event.labels() == null ? null : event.labels().get("pool");
        if (pool == null || pool.isBlank()) {
            log.warn("[hikari] 告警缺少 labels.pool，无法定位 Nacos dataId alertname={}", event.alertname());
            return null;
        }
        PoolTarget target = mappingProps.getPoolNameToApp().get(pool);
        if (target == null || target.getDataId() == null) {
            log.warn("[hikari] pool={} 未在 ops-agent.mapping.pool-name-to-app 中配置，跳过", pool);
            return null;
        }
        return target.getDataId();
    }

    @Override
    protected String buildPromptBody(AlertEvent event,
                                     String alertKey,
                                     String dataId,
                                     String group,
                                     String currentContent) {
        String alertJson = toJson(event);
        String pool = event.labels() == null ? "?" : event.labels().getOrDefault("pool", "?");
        PoolTarget target = mappingProps.getPoolNameToApp().get(pool);
        String app = target == null ? "?" : target.getApplication();
        String yaml = currentContent == null
                ? "# 当前 Nacos 里未找到该 dataId；请先调用 getConfig 确认结构，再补全完整 YAML"
                : currentContent;

        return """
                现在收到一条 Prometheus 告警：

                %s

                该告警属于 HikariCP 连接池类，触发键（alertKey）=「%s」。
                连接池名（labels.pool）= %s，已反查到责任服务 application = %s。
                当前 Nacos 中该服务的 datasource 配置（dataId=%s, group=%s，YAML）：

                %s

                请严格按以下步骤处理：
                1) 只调整 spring.datasource.hikari 下的 minimum-idle / maximum-pool-size /
                   connection-timeout / max-lifetime 等池参数；
                   url / username / password / driver-class-name 一律保持不变；
                2) 扩容次序：先 max-pool-size（单次 ≤ 2x，且 ≤ 之前值 + 10），再 minimum-idle；
                   timeout 参数不要轻易加大；
                3) 若 YAML 结构不完整或缺省，请按 HikariCP 常见模板补全后下发；
                4) 调用 publishConfig，参数：
                     - dataId   = "%s"
                     - group    = "%s"
                     - content  = 完整新 YAML 原文
                     - reasoning      = pool 名 + before/after 数值 + 告警活跃率或等待数
                     - source_alert_key = "%s"
                5) publishConfig 会进入人工审批；不要重复调用；不要调用 deleteConfig。

                只给出最终工具调用，不需要额外解释。
                """.formatted(
                        alertJson,
                        alertKey,
                        pool,
                        app,
                        dataId,
                        group,
                        yaml,
                        dataId,
                        group,
                        alertKey);
    }
}
