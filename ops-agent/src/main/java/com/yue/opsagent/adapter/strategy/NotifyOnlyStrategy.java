package com.yue.opsagent.adapter.strategy;

import com.yue.opsagent.config.ApprovalProperties;
import com.yue.opsagent.domain.port.ApprovalInboxPort;
import org.springframework.stereotype.Component;

/**
 * 通用 NotifyOnly 兜底策略：处理所有没有专属 domain 的通知型告警
 * （JVM 堆 / HTTP 5xx / ServiceDown ...），{@code domain="notify"} / {@code order=100}，
 * 放在策略链末尾兜底。
 *
 * <p>扩展：需要新增专门的 NotifyOnly 域（如 {@code redis} / {@code rocketmq}）时，
 * 新建 {@link AbstractNotifyOnlyStrategy} 的子类并声明更小的 {@code order()} 即可。</p>
 */
@Component
public class NotifyOnlyStrategy extends AbstractNotifyOnlyStrategy {

    public NotifyOnlyStrategy(ApprovalProperties approvalProps, ApprovalInboxPort inbox) {
        super(approvalProps, inbox);
    }

    @Override
    public String domain() {
        return "notify";
    }

    @Override
    public int order() {
        return 100;
    }
}
