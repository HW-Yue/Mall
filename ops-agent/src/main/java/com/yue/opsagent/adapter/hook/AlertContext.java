package com.yue.opsagent.adapter.hook;

import com.yue.opsagent.domain.model.AlertEvent;

/**
 * 告警上下文：透传给 Tool 层的 "当前正在处理哪条告警 + 路由到了哪个域"。
 *
 * <p>由策略层在调用 {@code agent.call(...)} 前写入，tool 层（{@code NacosConfigTool.publishConfig}）
 * 在同线程里读取，用于回填 {@code ApprovalTask} 的 {@code alert} 与 {@code domain} 字段。</p>
 *
 * @param event  原始告警；手动调用时为 null
 * @param domain 路由到的策略域，例如 sentinel / dtp / notify；手动调用时为 "manual"
 */
public record AlertContext(AlertEvent event, String domain) {
}
