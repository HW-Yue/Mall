package com.yue.opsagent.adapter.handler;

import com.yue.opsagent.adapter.strategy.AlertStrategy;
import com.yue.opsagent.config.ApprovalProperties;
import com.yue.opsagent.config.ApprovalProperties.StrategyProps;
import com.yue.opsagent.domain.model.AlertEvent;
import com.yue.opsagent.domain.port.AlertHandlerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警 → 策略路由器（精简版）。
 *
 * <p>职责只剩两件：</p>
 * <ol>
 *   <li>对 {@code firing} 且命中总白名单的告警按 {@code alertname|application|resource} 做节流
 *       （窗口 {@link ApprovalProperties#getThrottleWindow()}）</li>
 *   <li>按 {@link AlertStrategy#order()} 排好的策略列表，挑第一个 {@code supports} 的策略
 *       调用其 {@code dispatch}，让策略自行决定"读哪个配置、调哪个 Agent、用什么 prompt、是否直接写 inbox"</li>
 * </ol>
 *
 * <p>总白名单由所有策略的 alerts 并集构成（再叠加 {@code @Deprecated} 的旧 {@code alertWhitelist}
 * 作兜底），启动时计算并在每次处理时读取 —— 配置热更新场景下也能正确跟随。</p>
 */
@Component
@Order(20)
public class AgentTriggeringAlertHandler implements AlertHandlerPort {

    private static final Logger log = LoggerFactory.getLogger(AgentTriggeringAlertHandler.class);

    private final List<AlertStrategy> strategies;
    private final ApprovalProperties properties;

    /** alertKey -> 下次放行时间 (epoch ms) */
    private final ConcurrentHashMap<String, Long> throttleTable = new ConcurrentHashMap<>();

    public AgentTriggeringAlertHandler(List<AlertStrategy> strategies,
                                       ApprovalProperties properties) {
        List<AlertStrategy> sorted = new ArrayList<>(strategies);
        sorted.sort(Comparator.comparingInt(AlertStrategy::order));
        this.strategies = List.copyOf(sorted);
        this.properties = properties;

        log.info("[Alert] 已加载 {} 个策略：{}", this.strategies.size(),
                this.strategies.stream().map(s -> s.domain() + "@order=" + s.order()).toList());
    }

    @Override
    public void handle(List<AlertEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        Set<String> whitelist = buildWhitelist();
        long now = System.currentTimeMillis();
        long throttleMs = properties.getThrottleWindow().toMillis();

        for (AlertEvent event : events) {
            if (!"firing".equalsIgnoreCase(event.status())) {
                continue;
            }
            if (event.alertname() == null || !whitelist.contains(event.alertname())) {
                continue;
            }
            String alertKey = alertKey(event);
            Long next = throttleTable.get(alertKey);
            if (next != null && next > now) {
                log.info("[Alert] 告警 {} 在节流窗口内，跳过 (剩余 {} ms)", alertKey, next - now);
                continue;
            }
            throttleTable.put(alertKey, now + throttleMs);
            route(event, alertKey);
        }
    }

    private void route(AlertEvent event, String alertKey) {
        for (AlertStrategy strategy : strategies) {
            if (strategy.supports(event)) {
                log.info("[Alert] route alertKey={} -> strategy domain={}",
                        alertKey, strategy.domain());
                try {
                    strategy.dispatch(event, alertKey);
                } catch (RuntimeException ex) {
                    log.error("[Alert] strategy {} dispatch 失败 alertKey={}: {}",
                            strategy.domain(), alertKey, ex.toString(), ex);
                }
                return;
            }
        }
        log.debug("[Alert] 无策略可处理 alertKey={} alertname={}", alertKey, event.alertname());
    }

    /** 所有策略 alerts 的并集（再合并旧 alertWhitelist 兜底）。 */
    private Set<String> buildWhitelist() {
        Set<String> set = new HashSet<>();
        for (StrategyProps props : properties.getStrategies().values()) {
            if (props.getAlerts() != null) {
                set.addAll(props.getAlerts());
            }
        }
        if (properties.getAlertWhitelist() != null) {
            set.addAll(properties.getAlertWhitelist());
        }
        return set;
    }

    private String alertKey(AlertEvent event) {
        return (event.alertname() == null ? "?" : event.alertname())
                + "|" + (event.application() == null ? "?" : event.application())
                + "|" + (event.resource() == null ? "?" : event.resource());
    }

    // Visible for tests / ops
    long nextAllowedAt(String alertKey) {
        return throttleTable.getOrDefault(alertKey, 0L);
    }
}
