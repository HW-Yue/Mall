package com.yue.opsagent.springai.alert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.opsagent.springai.observability.OpsLogFormatter;
import com.yue.opsagent.springai.opsroute.OpsRouteService;
import com.yue.opsagent.springai.opsroute.RouteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/alert")
public class AlertReceiveController {

    private static final Logger log = LoggerFactory.getLogger(AlertReceiveController.class);

    private final OpsRouteService opsRouteService;
    private final ObjectMapper objectMapper;

    public AlertReceiveController(
            OpsRouteService opsRouteService,
            ObjectMapper objectMapper) {
        this.opsRouteService = opsRouteService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/receive")
    public Map<String, Object> receive(@RequestBody AlertmanagerPayload payload) {
        if (payload == null || payload.alerts() == null || payload.alerts().isEmpty()) {
            log.info("[Alert] webhook 收到空 alerts，跳过");
            return Map.of("status", "ok", "received", 0, "runs", List.of());
        }
        List<String> names = new ArrayList<>();
        for (AlertmanagerPayload.AlertmanagerAlert a : payload.alerts()) {
            if (a.labels() != null && a.labels().get("alertname") != null) {
                names.add(a.labels().get("alertname"));
            } else {
                names.add("(no alertname)");
            }
        }
        log.info("[Alert] webhook 收到 alerts={} 条, alertname 列表={}",
                payload.alerts().size(), names);
        try {
            String rawJson = objectMapper.writeValueAsString(payload);
            log.info("[Alert] webhook 原始 JSON（Alertmanager 全量，截断）↓↓\n{}",
                    OpsLogFormatter.truncate(rawJson, OpsLogFormatter.ALERT_JSON_MAX));
        } catch (JsonProcessingException e) {
            log.warn("[Alert] webhook body 序列化失败: {}", e.toString());
        }
        List<Map<String, Object>> runs = new ArrayList<>();
        for (AlertmanagerPayload.AlertmanagerAlert raw : payload.alerts()) {
            log.info("[Alert] 单条告警详情 status={} startsAt={} endsAt={} labels={} annotations={}",
                    raw.status(),
                    raw.startsAt(),
                    raw.endsAt(),
                    raw.labels(),
                    raw.annotations());
            var event = toEvent(raw, payload);
            var session = opsRouteService.routeAsync(RouteRequest.alert(event));
            Map<String, Object> row = new HashMap<>();
            row.put("runId", session.runId());
            row.put("status", session.status());
            row.put("alertname", event.alertname());
            runs.add(row);
        }
        log.info("[Alert] webhook 处理结束 runs={} 条", runs.size());
        return Map.of("status", "ok", "received", payload.alerts().size(), "runs", runs);
    }

    public static AlertEvent toEvent(AlertmanagerPayload.AlertmanagerAlert raw, AlertmanagerPayload payload) {
        Map<String, String> labels = raw.labels() == null ? Map.of() : raw.labels();
        Map<String, String> annotations = raw.annotations() == null ? Map.of() : raw.annotations();
        String app = labels.getOrDefault("application", labels.getOrDefault("app", ""));
        String status = raw.status() != null ? raw.status() : payload.status();
        return new AlertEvent(
                status,
                labels.get("alertname"),
                labels.get("severity"),
                app,
                labels,
                annotations);
    }
}
