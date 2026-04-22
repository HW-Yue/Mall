package com.yue.opsagent.domain.model;

import java.time.OffsetDateTime;

/**
 * 审批任务 —— Agent 产出的敏感变更方案 + 人工审批流水。
 * <p>不可变 record。状态流转通过 {@link #withStatus(ApprovalStatus, String)} 生成新实例。</p>
 *
 * @param id             任务 ID（UUID）
 * @param alertKey       触发本次变更的告警聚合键，形如 {@code alertname|application|resource}；手动触发时为 null
 * @param alert          触发本次变更的原始告警（可能为 null）
 * @param domain         任务所属策略域：{@code sentinel} / {@code dtp} / {@code notify} / {@code manual}；用于前端徽章与 approve 分支
 * @param toolName       被拦截的 tool 名，例如 {@code publishConfig}；notify 类任务为空串
 * @param dataId         Nacos dataId，notify 类任务为 {@code "-"}
 * @param group          Nacos group，notify 类任务为 {@code "-"}
 * @param contentBefore  审批创建时 Nacos 中的原值（用于 diff、防并发覆盖）；notify 类任务为 null
 * @param contentAfter   Agent 拟写入的新值；notify 类任务为 null
 * @param reasoning      Agent 给出的修改理由 / notify 类任务的说明文案
 * @param status         审批状态
 * @param errorMessage   FAILED / REJECTED 时记录的原因
 * @param createdAt      创建时间
 * @param decidedAt      人工决定（approve/reject）或 apply 完成时间
 */
public record ApprovalTask(
        String id,
        String alertKey,
        AlertEvent alert,
        String domain,
        String toolName,
        String dataId,
        String group,
        String contentBefore,
        String contentAfter,
        String reasoning,
        ApprovalStatus status,
        String errorMessage,
        OffsetDateTime createdAt,
        OffsetDateTime decidedAt
) {

    public ApprovalTask withStatus(ApprovalStatus newStatus, String errorMessage) {
        return new ApprovalTask(
                id,
                alertKey,
                alert,
                domain,
                toolName,
                dataId,
                group,
                contentBefore,
                contentAfter,
                reasoning,
                newStatus,
                errorMessage,
                createdAt,
                OffsetDateTime.now()
        );
    }

    /**
     * 覆盖 {@code reasoning} 生成新实例，用于诊断子 Agent 异步回填诊断文本。
     * 不改变 status / decidedAt 等字段，避免误把 PENDING 置为已决策。
     */
    public ApprovalTask withReasoning(String newReasoning) {
        return new ApprovalTask(
                id,
                alertKey,
                alert,
                domain,
                toolName,
                dataId,
                group,
                contentBefore,
                contentAfter,
                newReasoning,
                status,
                errorMessage,
                createdAt,
                decidedAt
        );
    }
}
