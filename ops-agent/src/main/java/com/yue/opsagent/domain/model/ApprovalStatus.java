package com.yue.opsagent.domain.model;

/**
 * 审批任务生命周期状态。
 *
 * <pre>
 *                  ┌── REJECTED
 *   PENDING ──────┤
 *                  └── APPROVED ──► APPLIED
 *                          │
 *                          └─────► FAILED
 *
 *   PENDING ──(超时)──► EXPIRED
 * </pre>
 */
public enum ApprovalStatus {
    /** 审批已创建，等待人工处置 */
    PENDING,
    /** 人工已同意，等待 apply（短暂中间态） */
    APPROVED,
    /** 人工拒绝 */
    REJECTED,
    /** 已成功写入 Nacos */
    APPLIED,
    /** apply 过程失败（Nacos 写入异常或版本冲突） */
    FAILED,
    /** 超过有效期未被处理，自动作废 */
    EXPIRED
}
