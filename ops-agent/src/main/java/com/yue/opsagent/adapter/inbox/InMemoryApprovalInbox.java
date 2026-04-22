package com.yue.opsagent.adapter.inbox;

import com.yue.opsagent.config.ApprovalProperties;
import com.yue.opsagent.domain.model.ApprovalStatus;
import com.yue.opsagent.domain.model.ApprovalTask;
import com.yue.opsagent.domain.port.ApprovalInboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 出站适配器：审批收件箱内存实现（调试降级用）。
 *
 * <p>默认不启用；当 {@code ops-agent.approval.storage.type=memory} 时才会接管
 * {@link ApprovalInboxPort}，适合本地排查 / 单测 / MySQL 不可用的临时场景。
 * 正式运行时走 {@code MybatisApprovalInbox}。</p>
 *
 * <ul>
 *   <li>{@link ConcurrentHashMap} 保存所有任务（重启会丢）</li>
 *   <li>{@link Sinks.Many} multicast，SSE 广播给所有订阅者</li>
 *   <li>{@link Scheduled} 定时把 PENDING 超过 {@link ApprovalProperties#getPendingTtl()} 的置 EXPIRED</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(prefix = "ops-agent.approval.storage",
        name = "type", havingValue = "memory")
public class InMemoryApprovalInbox implements ApprovalInboxPort {

    private static final Logger log = LoggerFactory.getLogger(InMemoryApprovalInbox.class);

    private final ApprovalProperties properties;
    private final ConcurrentHashMap<String, ApprovalTask> tasks = new ConcurrentHashMap<>();
    private final Sinks.Many<ApprovalTask> sink = Sinks.many().multicast().onBackpressureBuffer();

    public InMemoryApprovalInbox(ApprovalProperties properties) {
        this.properties = properties;
    }

    @Override
    public ApprovalTask save(ApprovalTask task) {
        tasks.put(task.id(), task);
        log.info("[Inbox] saved task id={} status={} dataId={} group={} alertKey={}",
                task.id(), task.status(), task.dataId(), task.group(), task.alertKey());
        emit(task);
        return task;
    }

    @Override
    public ApprovalTask updateStatus(String id, ApprovalStatus status, String errorMessage) {
        ApprovalTask updated = tasks.computeIfPresent(id,
                (k, v) -> v.withStatus(status, errorMessage));
        if (updated == null) {
            log.warn("[Inbox] updateStatus miss id={}", id);
            return null;
        }
        log.info("[Inbox] updated task id={} -> status={}{}",
                id, status,
                errorMessage == null ? "" : " errorMessage=" + errorMessage);
        emit(updated);
        return updated;
    }

    @Override
    public ApprovalTask updateReasoning(String id, String reasoning) {
        ApprovalTask updated = tasks.computeIfPresent(id, (k, v) -> v.withReasoning(reasoning));
        if (updated == null) {
            log.warn("[Inbox] updateReasoning miss id={}", id);
            return null;
        }
        log.info("[Inbox] updated reasoning id={} bytes={}", id,
                reasoning == null ? 0 : reasoning.length());
        emit(updated);
        return updated;
    }

    @Override
    public Optional<ApprovalTask> findById(String id) {
        return Optional.ofNullable(tasks.get(id));
    }

    @Override
    public List<ApprovalTask> list(ApprovalStatus status) {
        List<ApprovalTask> out = new ArrayList<>();
        for (ApprovalTask t : tasks.values()) {
            if (status == null || t.status() == status) {
                out.add(t);
            }
        }
        out.sort(Comparator.comparing(ApprovalTask::createdAt).reversed());
        return out;
    }

    @Override
    public Flux<ApprovalTask> stream() {
        return sink.asFlux();
    }

    /**
     * 定期把过期的 PENDING 任务置 EXPIRED。
     */
    @Scheduled(fixedDelay = 30_000L, initialDelay = 30_000L)
    public void expireStaleTasks() {
        OffsetDateTime threshold = OffsetDateTime.now().minus(properties.getPendingTtl());
        int expired = 0;
        for (ApprovalTask t : tasks.values()) {
            if (t.status() == ApprovalStatus.PENDING && t.createdAt().isBefore(threshold)) {
                updateStatus(t.id(), ApprovalStatus.EXPIRED, "PENDING 超过 " + properties.getPendingTtl());
                expired++;
            }
        }
        if (expired > 0) {
            log.info("[Inbox] expireStaleTasks expired={} ttl={}", expired, properties.getPendingTtl());
        }
    }

    private void emit(ApprovalTask task) {
        Sinks.EmitResult result = sink.tryEmitNext(task);
        if (result.isFailure()) {
            log.warn("[Inbox] sink emit failure={} taskId={}", result, task.id());
        }
    }
}
