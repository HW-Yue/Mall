package com.yue.opsagent.app;

import com.yue.opsagent.domain.model.ApprovalStatus;
import com.yue.opsagent.domain.model.ApprovalTask;
import com.yue.opsagent.domain.port.ApprovalInboxPort;
import com.yue.opsagent.domain.port.NacosConfigPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * Application 层：审批决策服务。
 * <p>
 * Controller 只负责 HTTP，具体的 approve / reject / apply 原子逻辑集中在这里，
 * 方便后续给 CLI / MQ 等入口复用。
 */
@Service
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);

    private final ApprovalInboxPort inbox;
    private final NacosConfigPort configPort;

    public ApprovalService(ApprovalInboxPort inbox, NacosConfigPort configPort) {
        this.inbox = inbox;
        this.configPort = configPort;
    }

    /**
     * 同意审批并执行 apply。
     *
     * <p>apply 前会再次拉 Nacos 最新原值，与创建任务时记录的 {@code contentBefore} 做指纹比对；
     * 若服务端已被他人修改，本次 apply 会置 {@link ApprovalStatus#FAILED}，避免并发覆盖。</p>
     */
    public synchronized ApprovalTask approve(String id) {
        ApprovalTask task = inbox.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("审批任务不存在: " + id));
        if (task.status() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("任务状态不是 PENDING，当前=" + task.status());
        }

        // notify 型任务（或任何不带 tool 的任务）只是知会，不走 Nacos。
        // 直接从 PENDING -> APPLIED 收尾，兼容前端"知道了"按钮。
        if ("notify".equals(task.domain())
                || task.toolName() == null || task.toolName().isBlank()) {
            log.info("[Approval] notify 型任务直接标记为 APPLIED id={}", id);
            return inbox.updateStatus(id, ApprovalStatus.APPLIED, null);
        }

        inbox.updateStatus(id, ApprovalStatus.APPROVED, null);

        String serverCurrent;
        try {
            serverCurrent = configPort.getConfig(task.dataId(), task.group());
        } catch (RuntimeException ex) {
            log.error("[Approval] apply 前校验拉配置失败 id={}", id, ex);
            return inbox.updateStatus(id, ApprovalStatus.FAILED,
                    "apply 前校验拉配置失败: " + ex.getMessage());
        }

        if (!Objects.equals(normalize(serverCurrent), normalize(task.contentBefore()))) {
            log.warn("[Approval] apply 前发现 Nacos 服务端原值已变动 id={}，拒绝覆盖", id);
            return inbox.updateStatus(id, ApprovalStatus.FAILED,
                    "Nacos 原值在审批期间被他人修改，请重新触发");
        }

        boolean ok;
        try {
            ok = configPort.publishConfig(task.dataId(), task.group(), task.contentAfter());
        } catch (RuntimeException ex) {
            log.error("[Approval] apply 失败 id={}", id, ex);
            return inbox.updateStatus(id, ApprovalStatus.FAILED,
                    "publishConfig 异常: " + ex.getMessage());
        }

        if (!ok) {
            return inbox.updateStatus(id, ApprovalStatus.FAILED,
                    "Nacos publishConfig 返回 false");
        }

        log.info("[Approval] applied id={} dataId={} group={}",
                id, task.dataId(), task.group());
        return inbox.updateStatus(id, ApprovalStatus.APPLIED, null);
    }

    public ApprovalTask reject(String id, String reason) {
        ApprovalTask task = inbox.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("审批任务不存在: " + id));
        if (task.status() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("任务状态不是 PENDING，当前=" + task.status());
        }
        return inbox.updateStatus(id, ApprovalStatus.REJECTED,
                reason == null ? "人工拒绝" : reason);
    }

    public Optional<ApprovalTask> find(String id) {
        return inbox.findById(id);
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim();
    }
}
