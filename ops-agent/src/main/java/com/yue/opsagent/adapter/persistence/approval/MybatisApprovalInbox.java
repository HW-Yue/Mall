package com.yue.opsagent.adapter.persistence.approval;

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
import java.util.List;
import java.util.Optional;

/**
 * 出站适配器：审批收件箱 MyBatis 持久化实现。
 *
 * <ul>
 *   <li>真相源：MySQL {@code ops_agent_db.approval_task}</li>
 *   <li>SSE 广播：{@link Sinks.Many} multicast，每次 save / updateStatus 后 emit 最新 PO.toDomain()</li>
 *   <li>{@code expireStaleTasks}：批量 UPDATE 后回查变更行，再批量 emit（不占长事务）</li>
 * </ul>
 *
 * <p>配置开关 {@code ops-agent.approval.storage.type=mysql}（默认生效）。</p>
 */
@Component
@ConditionalOnProperty(prefix = "ops-agent.approval.storage",
        name = "type", havingValue = "mysql", matchIfMissing = true)
public class MybatisApprovalInbox implements ApprovalInboxPort {

    private static final Logger log = LoggerFactory.getLogger(MybatisApprovalInbox.class);

    private final ApprovalTaskMapper mapper;
    private final ApprovalProperties properties;
    private final Sinks.Many<ApprovalTask> sink = Sinks.many().multicast().onBackpressureBuffer();

    public MybatisApprovalInbox(ApprovalTaskMapper mapper, ApprovalProperties properties) {
        this.mapper = mapper;
        this.properties = properties;
    }

    @Override
    public ApprovalTask save(ApprovalTask task) {
        mapper.insert(ApprovalTaskPO.from(task));
        log.info("[Inbox/MySQL] saved task id={} status={} dataId={} group={} alertKey={}",
                task.id(), task.status(), task.dataId(), task.group(), task.alertKey());
        emit(task);
        return task;
    }

    @Override
    public ApprovalTask updateStatus(String id, ApprovalStatus status, String errorMessage) {
        OffsetDateTime decidedAt = OffsetDateTime.now();
        int rows = mapper.updateStatus(id, status.name(), errorMessage, decidedAt);
        if (rows == 0) {
            log.warn("[Inbox/MySQL] updateStatus miss id={}", id);
            return null;
        }
        ApprovalTaskPO po = mapper.findById(id);
        if (po == null) {
            log.warn("[Inbox/MySQL] updateStatus succeeded but refetch miss id={}", id);
            return null;
        }
        ApprovalTask updated = po.toDomain();
        log.info("[Inbox/MySQL] updated task id={} -> status={}{}",
                id, status, errorMessage == null ? "" : " errorMessage=" + errorMessage);
        emit(updated);
        return updated;
    }

    @Override
    public ApprovalTask updateReasoning(String id, String reasoning) {
        int rows = mapper.updateReasoning(id, reasoning);
        if (rows == 0) {
            log.warn("[Inbox/MySQL] updateReasoning miss id={}", id);
            return null;
        }
        ApprovalTaskPO po = mapper.findById(id);
        if (po == null) {
            log.warn("[Inbox/MySQL] updateReasoning succeeded but refetch miss id={}", id);
            return null;
        }
        ApprovalTask updated = po.toDomain();
        log.info("[Inbox/MySQL] updated reasoning id={} bytes={}", id,
                reasoning == null ? 0 : reasoning.length());
        emit(updated);
        return updated;
    }

    @Override
    public Optional<ApprovalTask> findById(String id) {
        ApprovalTaskPO po = mapper.findById(id);
        return Optional.ofNullable(po).map(ApprovalTaskPO::toDomain);
    }

    @Override
    public List<ApprovalTask> list(ApprovalStatus status) {
        return mapper.list(status == null ? null : status.name())
                .stream()
                .map(ApprovalTaskPO::toDomain)
                .toList();
    }

    @Override
    public Flux<ApprovalTask> stream() {
        return sink.asFlux();
    }

    /**
     * 定期把过期的 PENDING 任务置 EXPIRED。
     * 实现：先选出 id 列表 → 批量 UPDATE → 回查变更行 → 逐条 emit。
     */
    @Scheduled(fixedDelay = 30_000L, initialDelay = 30_000L)
    public void expireStaleTasks() {
        OffsetDateTime threshold = OffsetDateTime.now().minus(properties.getPendingTtl());
        List<String> ids = mapper.selectExpiredPendingIds(threshold);
        if (ids.isEmpty()) {
            return;
        }
        String errMsg = "PENDING 超过 " + properties.getPendingTtl();
        int affected = mapper.expirePendingBefore(threshold, errMsg, OffsetDateTime.now());
        if (affected == 0) {
            return;
        }
        List<ApprovalTaskPO> pos = mapper.findByIds(ids);
        for (ApprovalTaskPO po : pos) {
            emit(po.toDomain());
        }
        log.info("[Inbox/MySQL] expireStaleTasks expired={} ttl={}", affected, properties.getPendingTtl());
    }

    private void emit(ApprovalTask task) {
        Sinks.EmitResult result = sink.tryEmitNext(task);
        if (result.isFailure()) {
            log.warn("[Inbox/MySQL] sink emit failure={} taskId={}", result, task.id());
        }
    }
}
