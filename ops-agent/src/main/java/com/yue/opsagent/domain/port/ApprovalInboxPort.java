package com.yue.opsagent.domain.port;

import com.yue.opsagent.domain.model.ApprovalStatus;
import com.yue.opsagent.domain.model.ApprovalTask;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

/**
 * Domain Port：审批收件箱能力抽象。
 *
 * <p>上游（Tool / Hook / Handler）把待审批任务写入；下游（Controller / 前端）通过 {@link #stream()}
 * 订阅新任务事件并决策 approve/reject。实现可用内存、Redis、DB 等。</p>
 */
public interface ApprovalInboxPort {

    /** 新建一条 {@link ApprovalStatus#PENDING} 任务并广播给订阅者。 */
    ApprovalTask save(ApprovalTask task);

    /** 更新任务状态（APPROVED / REJECTED / APPLIED / FAILED / EXPIRED），并广播变更。 */
    ApprovalTask updateStatus(String id, ApprovalStatus status, String errorMessage);

    /**
     * 更新任务的 {@code reasoning} 字段，并广播变更。
     * <p>诊断子 Agent 场景下：策略先用占位 reasoning（如 {@code [诊断中...]}）落一条 PENDING
     * 任务到 inbox，随后异步 Agent 跑完再把最终诊断文本回填进来。</p>
     *
     * @return 最新的 {@link ApprovalTask}；id 不存在时返回 null
     */
    ApprovalTask updateReasoning(String id, String reasoning);

    Optional<ApprovalTask> findById(String id);

    /** status=null 时返回全部。 */
    List<ApprovalTask> list(ApprovalStatus status);

    /** SSE 广播流：订阅后收到所有新增和状态变更事件。 */
    Flux<ApprovalTask> stream();
}
