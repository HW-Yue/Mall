package com.yue.opsagent.domain.model;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 领域对象：Alertmanager Webhook 中的单条告警事件。
 * <p>仅保留 Port/Handler 关心的字段，原始 payload 里的其他元信息忽略。</p>
 *
 * @param status       firing / resolved
 * @param alertname    告警名（来自 labels.alertname）
 * @param severity     warning / critical / info
 * @param category     规则文件里打的 category 标签（sentinel / dynamictp / system / http / datasource）
 * @param application  业务服务名（Sentinel 指标打的 app 标签或 Micrometer common tag）
 * @param resource     Sentinel 资源名，若非 Sentinel 告警为空
 * @param summary      annotations.summary
 * @param description  annotations.description
 * @param startsAt     告警首次触发时间
 * @param endsAt       告警结束时间（仅 resolved 时有意义）
 * @param labels       原始 labels，用于补充诊断
 * @param annotations  原始 annotations
 */
public record AlertEvent(
        String status,
        String alertname,
        String severity,
        String category,
        String application,
        String resource,
        String summary,
        String description,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        Map<String, String> labels,
        Map<String, String> annotations
) {
}
