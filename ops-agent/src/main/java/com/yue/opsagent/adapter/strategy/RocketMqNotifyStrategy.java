package com.yue.opsagent.adapter.strategy;

import com.yue.opsagent.config.ApprovalProperties;
import com.yue.opsagent.domain.model.AlertEvent;
import com.yue.opsagent.domain.port.ApprovalInboxPort;
import org.springframework.stereotype.Component;

/**
 * RocketMQ NotifyOnly 策略：处理 {@code category=rocketmq} 的告警。
 *
 * <p>Broker 死活、消费堆积、DLQ 出现都需要人工介入（扩容 Broker、排查消费端、死信处理），
 * 没有可自动调整的 Nacos 配置，这里直接投递到审批箱作知会。</p>
 */
@Component
public class RocketMqNotifyStrategy extends AbstractNotifyOnlyStrategy {

    public RocketMqNotifyStrategy(ApprovalProperties approvalProps, ApprovalInboxPort inbox) {
        super(approvalProps, inbox);
    }

    @Override
    public String domain() {
        return "rocketmq";
    }

    @Override
    public int order() {
        return 50;
    }

    @Override
    protected boolean matches(AlertEvent event) {
        return "rocketmq".equalsIgnoreCase(event.category());
    }

    @Override
    protected String buildNotifyMessage(AlertEvent event, String alertKey) {
        String topic = event.labels() == null ? null : event.labels().get("topic");
        String group = event.labels() == null ? null : event.labels().get("group");
        String detail = (topic == null && group == null)
                ? ""
                : String.format(" topic=%s group=%s", topic, group);
        return String.format(
                "[rocketmq] %s：%s%s%n建议排查：确认 Broker 健康；DLQ 出现请先冻结消费端再人工补偿；消费堆积可临时扩消费者实例。",
                event.alertname(),
                event.summary() == null ? event.description() : event.summary(),
                detail);
    }
}
