package com.yue.opsagent.adapter.handler;

import com.yue.opsagent.domain.model.AlertEvent;
import com.yue.opsagent.domain.port.AlertHandlerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AlertHandlerPort 的默认实现：结构化日志落地。
 * <p>当前阶段仅把 Alertmanager 推送的告警按 application + alertname 聚合后打印，
 * 方便在接入 LLM / 自动改配置前先确认链路通。</p>
 */
@Component
@Order(10)
public class LoggingAlertHandler implements AlertHandlerPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingAlertHandler.class);

    @Override
    public void handle(List<AlertEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        Map<String, List<AlertEvent>> grouped = events.stream()
                .collect(Collectors.groupingBy(
                        e -> (e.application() == null ? "unknown" : e.application()) + "/" + e.alertname(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        log.info("[Alert] 收到告警批次 size={} groups={}", events.size(), grouped.size());
        grouped.forEach((key, group) -> {
            AlertEvent first = group.get(0);
            log.warn(
                    "[Alert] key={} status={} severity={} category={} resource={} summary=\"{}\" description=\"{}\" count={} startsAt={} labels={}",
                    key,
                    first.status(),
                    first.severity(),
                    first.category(),
                    first.resource(),
                    first.summary(),
                    first.description(),
                    group.size(),
                    first.startsAt(),
                    first.labels());
        });
    }
}
