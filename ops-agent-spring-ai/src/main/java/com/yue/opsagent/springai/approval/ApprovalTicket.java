package com.yue.opsagent.springai.approval;

import java.time.Instant;
import java.util.Map;

public record ApprovalTicket(
        String id,
        String runId,
        String skillName,
        String toolName,
        Map<String, Object> args,
        Instant createdAt,
        ApprovalStatus status,
        Instant decidedAt,
        String outcomeMessage
) {
    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED,
        FAILED
    }
}
