package com.yue.opsagent.adapter.strategy;

import com.yue.opsagent.domain.model.AlertEvent;

/**
 * 告警策略：按 "领域（sentinel / dtp / mysql / redis / rocketmq / notify ...）" 拆分的处理单元。
 *
 * <p>{@code AgentTriggeringAlertHandler} 在节流放行后，会把告警按 {@link #order()} 顺序
 * 喂给所有策略，选中第一个 {@link #supports(AlertEvent)} 返回 true 的策略执行
 * {@link #dispatch(AlertEvent, String)}。不同域用不同 system prompt / dataId / group /
 * Agent name，不同 exporter 只需新增一个实现类即可扩展。</p>
 *
 * @see AbstractNacosWriteStrategy 用于 "读当前配置 → Agent 生成变更 → 入审批队列" 的通用基类
 */
public interface AlertStrategy {

    /**
     * 策略所在领域，例如 {@code "sentinel"}、{@code "dtp"}、{@code "notify"}。
     * <p>必须与 {@code ops-agent.prompts.<domain>} 以及
     * {@code ops-agent.approval.strategies.<domain>} 的配置 key 一致。</p>
     */
    String domain();

    /** 匹配优先级：数值越小越先判定。同域写策略建议 10~50，兜底策略 100。 */
    int order();

    /** 该策略是否能处理该告警。基类会先做 alertname 白名单校验，子类只处理 {@code matches} 逻辑。 */
    boolean supports(AlertEvent event);

    /**
     * 处理告警：拉当前配置 → 构造 prompt → 调 Agent → 入审批队列（或仅写通知）。
     * <p>由策略自行实现，{@code AgentTriggeringAlertHandler} 只做节流 + 路由。</p>
     *
     * @param event    告警
     * @param alertKey 节流键 {@code alertname|application|resource}
     */
    void dispatch(AlertEvent event, String alertKey);
}
