package com.yue.opsagent.adapter.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.opsagent.adapter.hook.AlertContext;
import com.yue.opsagent.adapter.hook.AlertContextHolder;
import com.yue.opsagent.agent.NacosManagerAgent;
import com.yue.opsagent.config.AgentSystemPromptProperties;
import com.yue.opsagent.config.ApprovalProperties;
import com.yue.opsagent.config.ApprovalProperties.StrategyProps;
import com.yue.opsagent.domain.model.AlertEvent;
import com.yue.opsagent.domain.port.NacosConfigPort;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * "读当前配置 → Agent 生成变更 → 入审批队列" 类策略的共用基类。
 *
 * <p>子类只需关心 4 个钩子（见下文），通用流程（拉配置 / 构造 Msg / 建 Agent /
 * 设置 {@link AlertContext} / 异步订阅）都在 {@link #dispatch} 里封装好。未来接 MySQL /
 * Redis / RocketMQ 等 exporter 都继承本类即可。</p>
 *
 * <h3>子类需要实现的钩子：</h3>
 * <ul>
 *   <li>{@link #domain()} —— 与 YAML 里 {@code prompts.<domain>} / {@code strategies.<domain>} 对齐</li>
 *   <li>{@link #matches(AlertEvent)} —— 按 category 判定，白名单由基类处理</li>
 *   <li>{@link #resolveDataId(AlertEvent, StrategyProps)} —— 由 {@code data-id-template} 展开</li>
 *   <li>{@link #buildPromptBody(AlertEvent, String, String, String, String)} —— 领域专属 prompt</li>
 * </ul>
 */
public abstract class AbstractNacosWriteStrategy implements AlertStrategy {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final ApprovalProperties approvalProps;
    protected final AgentSystemPromptProperties promptProps;
    protected final NacosConfigPort configPort;
    protected final NacosManagerAgent agentFactory;
    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected AbstractNacosWriteStrategy(ApprovalProperties approvalProps,
                                         AgentSystemPromptProperties promptProps,
                                         NacosConfigPort configPort,
                                         NacosManagerAgent agentFactory) {
        this.approvalProps = approvalProps;
        this.promptProps = promptProps;
        this.configPort = configPort;
        this.agentFactory = agentFactory;
    }

    /** 领域判定：子类通常写 {@code "sentinel".equals(event.category())} 之类。 */
    protected abstract boolean matches(AlertEvent event);

    /** 解析本次告警应读写的 Nacos dataId。通常基于 {@code props.getDataIdTemplate()} + 变量替换。 */
    protected abstract String resolveDataId(AlertEvent event, StrategyProps props);

    /**
     * 构造喂给 Agent 的用户 prompt。基类已经把 alert / dataId / group / 当前配置都读好了。
     *
     * @param event          告警
     * @param alertKey       节流键
     * @param dataId         已解析的 dataId
     * @param group          Nacos group
     * @param currentContent Nacos 中当前原文（可能为 null）
     */
    protected abstract String buildPromptBody(AlertEvent event,
                                              String alertKey,
                                              String dataId,
                                              String group,
                                              String currentContent);

    @Override
    public boolean supports(AlertEvent event) {
        StrategyProps props = strategyProps();
        if (props == null || props.getAlerts() == null || !props.getAlerts().contains(event.alertname())) {
            return false;
        }
        return matches(event);
    }

    @Override
    public void dispatch(AlertEvent event, String alertKey) {
        StrategyProps props = strategyProps();
        if (props == null) {
            log.warn("[{}] strategies.{} 配置缺失，跳过 alert={}", domain(), domain(), event.alertname());
            return;
        }
        if (requireApplicationLabel() && event.application() == null) {
            log.warn("[{}] 告警缺少 application 标签，无法定位 Nacos dataId alertKey={}", domain(), alertKey);
            return;
        }

        String dataId = resolveDataId(event, props);
        if (dataId == null || dataId.isBlank()) {
            // 子类（如 HikariTuningStrategy 按 pool 反查）判断无法定位时返回 null，这里统一跳过
            log.warn("[{}] 未能解析 dataId，跳过 alertKey={}", domain(), alertKey);
            return;
        }
        String group = props.getGroup();
        String current = safeGetConfig(dataId, group);

        String promptBody = buildPromptBody(event, alertKey, dataId, group, current);
        String sysPrompt = promptProps.getPrompts().getOrDefault(domain(), promptProps.getSystemPrompt());

        log.info("[{}] agent run started alertKey={} dataId={} group={} promptBytes={}",
                domain(), alertKey, dataId, group, promptBody.length());

        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .textContent(promptBody)
                .build();

        AlertContextHolder.set(new AlertContext(event, domain()));
        ReActAgent agent = agentFactory.createAgent(sysPrompt, domain() + "-ops");
        agent.call(userMsg)
                .doFinally(sig -> AlertContextHolder.clear())
                .subscribe(
                        result -> log.info("[{}] agent run finished alertKey={} reason={}",
                                domain(), alertKey,
                                result == null ? "null" : String.valueOf(result.getGenerateReason())),
                        err -> log.error("[{}] agent run failed alertKey={}: {}",
                                domain(), alertKey, err.toString(), err));
    }

    /** 读取该域的策略配置；子类必要时可复写。 */
    protected StrategyProps strategyProps() {
        return approvalProps.getStrategies().get(domain());
    }

    /**
     * 是否强制校验 {@code event.application()} 非空。
     * 绝大多数 per-service 告警需要；而 Hikari / shared 类告警（pool 反查 or 全局 dataId）不需要。
     */
    protected boolean requireApplicationLabel() {
        return true;
    }

    /** 包一层，拉 Nacos 失败时返回 null，不阻断 prompt 构造。 */
    protected String safeGetConfig(String dataId, String group) {
        try {
            return configPort.getConfig(dataId, group);
        } catch (RuntimeException ex) {
            log.warn("[{}] 拉取 Nacos 当前配置失败 dataId={} group={}: {}",
                    domain(), dataId, group, ex.getMessage());
            return null;
        }
    }

    /** 把 AlertEvent 序列化成 pretty JSON，供 prompt 嵌入；失败时返回 {@code event.toString()}。 */
    protected String toJson(AlertEvent event) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(event);
        } catch (Exception ex) {
            return String.valueOf(event);
        }
    }
}
