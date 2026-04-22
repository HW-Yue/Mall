package com.yue.opsagent.domain.port;

import com.yue.opsagent.domain.model.AlertEvent;

import java.util.List;

/**
 * Domain Port: 告警事件处理能力抽象。
 * <p>上游（Controller）接收 Alertmanager webhook 后，将一批标准化的 {@link AlertEvent}
 * 交给该端口处理。默认实现仅结构化日志，后续可扩展为：
 * <ul>
 *     <li>调用 LLM 给出诊断建议</li>
 *     <li>触发 Nacos 配置调整（Sentinel 流控规则、DynamicTP 线程池参数）</li>
 *     <li>发送 IM 通知</li>
 * </ul>
 */
public interface AlertHandlerPort {

    /**
     * 处理一批告警事件（Alertmanager 以 group 为单位批量推送）。
     *
     * @param events 已解析的事件列表，非 null、非空
     */
    void handle(List<AlertEvent> events);
}
