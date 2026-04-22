package com.yue.opsagent.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yue.opsagent.domain.model.AlertEvent;
import com.yue.opsagent.domain.port.AlertHandlerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 入站适配器：接收 Alertmanager Webhook（v4 schema）。
 * <p>流程：
 * <ol>
 *     <li>反序列化为 {@link AlertmanagerPayload}</li>
 *     <li>映射为 Domain 层 {@link AlertEvent}</li>
 *     <li>依次委托给所有 {@link AlertHandlerPort} 实现（按 {@link org.springframework.core.annotation.Order} 排序）</li>
 * </ol>
 * Alertmanager 参考：<a href="https://prometheus.io/docs/alerting/latest/configuration/#webhook_config">webhook_config</a>
 */
@RestController
@RequestMapping("/api/v1/alert")
public class AlertReceiveController {

    private static final Logger log = LoggerFactory.getLogger(AlertReceiveController.class);

    private final List<AlertHandlerPort> handlers;

    public AlertReceiveController(List<AlertHandlerPort> handlers) {
        this.handlers = handlers;
    }

    @PostMapping("/receive")
    public Map<String, Object> receive(@RequestBody AlertmanagerPayload payload) {
        if (payload == null || payload.alerts() == null || payload.alerts().isEmpty()) {
            log.warn("[Alert] 收到空 payload");
            return Map.of("status", "ok", "received", 0);
        }

        List<AlertEvent> events = new ArrayList<>(payload.alerts().size());
        for (AlertmanagerAlert raw : payload.alerts()) {
            events.add(toDomain(raw, payload));
        }

        for (AlertHandlerPort handler : handlers) {
            try {
                handler.handle(events);
            } catch (Exception ex) {
                log.error("[Alert] handler {} 处理异常", handler.getClass().getSimpleName(), ex);
            }
        }
        return Map.of("status", "ok", "received", events.size());
    }

    private AlertEvent toDomain(AlertmanagerAlert raw, AlertmanagerPayload payload) {
        Map<String, String> labels = raw.labels() == null ? Map.of() : raw.labels();
        Map<String, String> annotations = raw.annotations() == null ? Map.of() : raw.annotations();

        String application = labels.getOrDefault("application", labels.get("app"));

        return new AlertEvent(
                raw.status() == null ? payload.status() : raw.status(),
                labels.get("alertname"),
                labels.get("severity"),
                labels.get("category"),
                application,
                labels.get("resource"),
                annotations.get("summary"),
                annotations.get("description"),
                raw.startsAt(),
                raw.endsAt(),
                labels,
                annotations
        );
    }

    /**
     * Alertmanager webhook v4 顶层结构。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AlertmanagerPayload(
            String version,
            String groupKey,
            Integer truncatedAlerts,
            String status,
            String receiver,
            Map<String, String> groupLabels,
            Map<String, String> commonLabels,
            Map<String, String> commonAnnotations,
            String externalURL,
            List<AlertmanagerAlert> alerts
    ) {
    }

    /**
     * Alertmanager webhook 中的单条告警。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AlertmanagerAlert(
            String status,
            Map<String, String> labels,
            Map<String, String> annotations,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            String generatorURL,
            String fingerprint
    ) {
    }
}
