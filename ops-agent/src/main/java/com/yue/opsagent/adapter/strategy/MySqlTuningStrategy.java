package com.yue.opsagent.adapter.strategy;

import com.yue.opsagent.agent.NacosManagerAgent;
import com.yue.opsagent.config.AgentSystemPromptProperties;
import com.yue.opsagent.config.ApprovalProperties;
import com.yue.opsagent.config.ApprovalProperties.StrategyProps;
import com.yue.opsagent.domain.model.AlertEvent;
import com.yue.opsagent.domain.port.NacosConfigPort;
import org.springframework.stereotype.Component;

/**
 * MySQL 实例级调优策略：处理 {@code category=mysql} 的告警（由 mysqld_exporter 产生，
 * {@code application=shared}）。
 *
 * <ul>
 *   <li>dataId：{@code shared-mysql-tuning.yml}（固定，不按服务区分）</li>
 *   <li>group：{@code DEFAULT_GROUP}</li>
 *   <li>Agent name：{@code mysql-ops}，使用 {@code ops-agent.prompts.mysql}</li>
 * </ul>
 */
@Component
public class MySqlTuningStrategy extends AbstractNacosWriteStrategy {

    private static final String DEFAULT_DATA_ID = "shared-mysql-tuning.yml";

    public MySqlTuningStrategy(ApprovalProperties approvalProps,
                               AgentSystemPromptProperties promptProps,
                               NacosConfigPort configPort,
                               NacosManagerAgent agentFactory) {
        super(approvalProps, promptProps, configPort, agentFactory);
    }

    @Override
    public String domain() {
        return "mysql";
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    protected boolean matches(AlertEvent event) {
        return "mysql".equalsIgnoreCase(event.category());
    }

    @Override
    protected String resolveDataId(AlertEvent event, StrategyProps props) {
        String template = props.getDataIdTemplate();
        return (template == null || template.isBlank()) ? DEFAULT_DATA_ID : template;
    }

    @Override
    protected String buildPromptBody(AlertEvent event,
                                     String alertKey,
                                     String dataId,
                                     String group,
                                     String currentContent) {
        String alertJson = toJson(event);
        String yaml = currentContent == null
                ? """
                  # Nacos 里尚未创建 shared-mysql-tuning.yml，请按以下骨架下发：
                  # tuning:
                  #   innodb_buffer_pool_size: "2G"
                  #   max_connections: 500
                  #   slow_query_threshold_ms: 500
                  # 根据本次告警新增/调整相关 key
                  """
                : currentContent;

        return """
                现在收到一条 Prometheus 告警：

                %s

                该告警属于 MySQL 实例调优类（mysqld_exporter，application=shared），
                触发键（alertKey）=「%s」。
                当前 Nacos 中的共享调优档案（dataId=%s, group=%s，YAML）：

                %s

                请严格按以下步骤处理：
                1) 只修改与本次告警相关的实例级参数（innodb_buffer_pool_size / max_connections /
                   innodb_flush_log_at_trx_commit / slow_query_threshold_ms 等），
                   不要碰任何服务的 Hikari 连接池（那是 hikari 域的事）；
                2) 若告警是 MySqlTooManyConnections，请综合当前 max_connections 与告警触发值给出
                   新的 max_connections，单次提升 ≤ 2x；
                3) 调用 publishConfig，参数：
                     - dataId   = "%s"
                     - group    = "%s"
                     - content  = 完整新 YAML 原文
                     - reasoning      = 告警 before 估算 + 建议 after + 依据
                     - source_alert_key = "%s"
                4) publishConfig 会进入人工审批；不要重复调用；不要调用 deleteConfig。

                只给出最终工具调用，不需要额外解释。
                """.formatted(
                        alertJson,
                        alertKey,
                        dataId,
                        group,
                        yaml,
                        dataId,
                        group,
                        alertKey);
    }
}
