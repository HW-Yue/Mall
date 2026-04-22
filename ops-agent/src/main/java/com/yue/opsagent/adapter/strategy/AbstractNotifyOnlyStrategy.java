package com.yue.opsagent.adapter.strategy;

import com.yue.opsagent.config.ApprovalProperties;
import com.yue.opsagent.config.ApprovalProperties.StrategyProps;
import com.yue.opsagent.domain.model.AlertEvent;
import com.yue.opsagent.domain.model.ApprovalStatus;
import com.yue.opsagent.domain.model.ApprovalTask;
import com.yue.opsagent.domain.port.ApprovalInboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * "仅通知型"策略的共用基类。对应那些没有可自动调整的 Nacos 配置、或出于保守只想让人工看到
 * 的告警（JVM 堆 / HTTP 5xx / ServiceDown / Redis / RocketMQ 诊断告警等）。
 *
 * <p>子类只需要覆盖 {@link #domain()} / {@link #order()} / {@link #matches(AlertEvent)} 以及可选的
 * {@link #buildNotifyMessage(AlertEvent, String)}。基类统一负责：</p>
 * <ul>
 *   <li>{@link #supports} —— 先做 {@code strategies.<domain>.alerts} 白名单，再交给 {@link #matches}</li>
 *   <li>{@link #dispatch} —— 构造 {@link ApprovalTask}（{@code toolName=""} / {@code dataId="-"}）
 *       落入 inbox，由 {@code ApprovalService} 对 notify 域短路为 APPLIED</li>
 * </ul>
 */
public abstract class AbstractNotifyOnlyStrategy implements AlertStrategy {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final ApprovalProperties approvalProps;
    protected final ApprovalInboxPort inbox;

    protected AbstractNotifyOnlyStrategy(ApprovalProperties approvalProps, ApprovalInboxPort inbox) {
        this.approvalProps = approvalProps;
        this.inbox = inbox;
    }

    /** category 侧判定；默认"不限制"，子类可覆盖限制到 {@code category=redis} 等。 */
    protected boolean matches(AlertEvent event) {
        return true;
    }

    /** 诊断文本模板；子类可覆盖加入映射表反查、ES 提示等业务语义。 */
    protected String buildNotifyMessage(AlertEvent event, String alertKey) {
        return String.format(
                "[%s] 告警 %s，summary=%s，本域未配置自动写 Nacos，请人工介入。",
                domain(),
                event.category() == null ? "unknown" : event.category(),
                event.summary() == null ? event.description() : event.summary());
    }

    @Override
    public boolean supports(AlertEvent event) {
        StrategyProps props = approvalProps.getStrategies().get(domain());
        List<String> whitelist = props == null ? null : props.getAlerts();
        if (whitelist == null || !whitelist.contains(event.alertname())) {
            return false;
        }
        return matches(event);
    }

    @Override
    public void dispatch(AlertEvent event, String alertKey) {
        String reason = buildNotifyMessage(event, alertKey);
        ApprovalTask task = new ApprovalTask(
                UUID.randomUUID().toString(),
                alertKey,
                event,
                domain(),
                "",
                "-",
                "-",
                null,
                null,
                reason,
                ApprovalStatus.PENDING,
                null,
                OffsetDateTime.now(),
                null
        );
        inbox.save(task);
        log.info("[{}] 已投递知会型任务 alertKey={} taskId={} alertname={}",
                domain(), alertKey, task.id(), event.alertname());
    }
}
