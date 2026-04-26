package com.yue.opsagent.springai.service;

import com.yue.opsagent.springai.domain.approval.ApprovalPresenter;
import com.yue.opsagent.springai.domain.approval.ApprovalTicket;
import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * In-memory approval queue with optional SSE subscribers for the static web console.
 */
@Service
public class ApprovalService {

    private final Map<String, OpsSkillRegistry> skills;
    private final OpsRunService opsRunService;
    private final Map<String, ApprovalTicket> tickets = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<SseEmitter> approvalSubscribers = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "approval-sse-heartbeat");
        t.setDaemon(true);
        return t;
    });

    public ApprovalService(Collection<OpsSkillRegistry> registries, OpsRunService opsRunService) {
        this.opsRunService = opsRunService;
        this.skills = new ConcurrentHashMap<>();
        for (OpsSkillRegistry r : registries) {
            this.skills.put(r.name(), r);
        }
    }

    @PostConstruct
    void startHeartbeat() {
        heartbeatExecutor.scheduleAtFixedRate(this::broadcastHeartbeat, 25, 25, TimeUnit.SECONDS);
    }

    @PreDestroy
    void stopHeartbeat() {
        heartbeatExecutor.shutdownNow();
    }

    public ToolResult enqueue(String skillName, String toolName, Map<String, Object> args) {
        return enqueue(null, skillName, toolName, args);
    }

    public ToolResult enqueue(String runId, String skillName, String toolName, Map<String, Object> args) {
        String id = UUID.randomUUID().toString();
        var ticket = new ApprovalTicket(
                id,
                runId,
                skillName,
                toolName,
                Map.copyOf(args == null ? Map.of() : args),
                Instant.now(),
                ApprovalTicket.ApprovalStatus.PENDING,
                null,
                null);
        tickets.put(id, ticket);
        broadcast(ticket);
        if (runId != null && !runId.isBlank()) {
            opsRunService.waitingApproval(runId, id, Map.of(
                    "approvalId", id,
                    "skill", skillName,
                    "tool", toolName,
                    "args", args == null ? Map.of() : args));
        }
        return ToolResult.pending(id, "已提交审批，通过后调用 POST /api/v1/approvals/{id}/approve");
    }

    public List<ApprovalTicket> listPending() {
        return tickets.values().stream()
                .filter(t -> t.status() == ApprovalTicket.ApprovalStatus.PENDING)
                .sorted((a, b) -> a.createdAt().compareTo(b.createdAt()))
                .toList();
    }

    public List<ApprovalTicket> listAll() {
        return tickets.values().stream()
                .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
                .toList();
    }

    public Optional<ApprovalTicket> get(String id) {
        return Optional.ofNullable(tickets.get(id));
    }

    public Map<String, Object> approveAndRun(String id) {
        ApprovalTicket ticket = tickets.get(id);
        if (ticket == null) {
            return Map.of("status", "error", "message", "审批单不存在: " + id);
        }
        if (ticket.status() != ApprovalTicket.ApprovalStatus.PENDING) {
            return Map.of("status", "error", "message", "审批单状态非 PENDING: " + ticket.status());
        }
        OpsSkillRegistry reg = skills.get(ticket.skillName());
        if (reg == null) {
            var fail = replace(ticket, ApprovalTicket.ApprovalStatus.FAILED, "未知 skill: " + ticket.skillName());
            tickets.put(id, fail);
            broadcast(fail);
            notifyRunFailed(fail);
            Map<String, Object> m = new java.util.HashMap<>(ApprovalPresenter.toTaskView(fail));
            m.put("status", "error");
            m.put("message", "未知 skill: " + ticket.skillName());
            return m;
        }
        ToolResult r = reg.execute(ticket.toolName(), ticket.args());
        if (r instanceof ToolResult.Error e) {
            var fail = replace(ticket, ApprovalTicket.ApprovalStatus.FAILED, e.message());
            tickets.put(id, fail);
            broadcast(fail);
            notifyRunFailed(fail);
            Map<String, Object> m = new java.util.HashMap<>(ApprovalPresenter.toTaskView(fail));
            m.put("toolResult", r.toMap());
            return m;
        }
        var done = new ApprovalTicket(
                ticket.id(),
                ticket.runId(),
                ticket.skillName(),
                ticket.toolName(),
                ticket.args(),
                ticket.createdAt(),
                ApprovalTicket.ApprovalStatus.APPROVED,
                Instant.now(),
                r instanceof ToolResult.Ok ok ? ok.message() : "ok");
        tickets.put(id, done);
        broadcast(done);
        if (done.runId() != null && !done.runId().isBlank()) {
            opsRunService.approvedToolResult(done.runId(), done.id(), r.toMap());
        }
        Map<String, Object> m = new java.util.HashMap<>(ApprovalPresenter.toTaskView(done));
        m.put("toolResult", r.toMap());
        return m;
    }

    public Map<String, Object> reject(String id, String reason) {
        ApprovalTicket ticket = tickets.get(id);
        if (ticket == null) {
            return Map.of("status", "error", "message", "审批单不存在: " + id);
        }
        if (ticket.status() != ApprovalTicket.ApprovalStatus.PENDING) {
            return Map.of("status", "error", "message", "审批单状态非 PENDING: " + ticket.status());
        }
        String msg = reason == null || reason.isBlank() ? "已拒绝" : reason;
        var done = new ApprovalTicket(
                ticket.id(),
                ticket.runId(),
                ticket.skillName(),
                ticket.toolName(),
                ticket.args(),
                ticket.createdAt(),
                ApprovalTicket.ApprovalStatus.REJECTED,
                Instant.now(),
                msg);
        tickets.put(id, done);
        broadcast(done);
        if (done.runId() != null && !done.runId().isBlank()) {
            opsRunService.reject(done.runId(), done.id(), msg);
        }
        return ApprovalPresenter.toTaskView(done);
    }

    private static ApprovalTicket replace(ApprovalTicket ticket, ApprovalTicket.ApprovalStatus st, String msg) {
        return new ApprovalTicket(
                ticket.id(),
                ticket.runId(),
                ticket.skillName(),
                ticket.toolName(),
                ticket.args(),
                ticket.createdAt(),
                st,
                Instant.now(),
                msg);
    }

    private void notifyRunFailed(ApprovalTicket ticket) {
        if (ticket.runId() != null && !ticket.runId().isBlank()) {
            opsRunService.fail(ticket.runId(), ticket.outcomeMessage());
        }
    }

    public SseEmitter subscribeApprovalsStream() {
        SseEmitter emitter = new SseEmitter(3_600_000L);
        approvalSubscribers.add(emitter);
        Runnable remove = () -> approvalSubscribers.remove(emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        sendRaw(emitter, heartbeatJson());
        return emitter;
    }

    private void broadcast(ApprovalTicket t) {
        broadcastRaw(ApprovalPresenter.toJson(t));
    }

    private void broadcastHeartbeat() {
        broadcastRaw(heartbeatJson());
    }

    private void broadcastRaw(String json) {
        for (SseEmitter emitter : approvalSubscribers) {
            sendRaw(emitter, json);
        }
    }

    private void sendRaw(SseEmitter emitter, String json) {
        try {
            emitter.send(SseEmitter.event().data(json));
        } catch (IOException e) {
            approvalSubscribers.remove(emitter);
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        }
    }

    private static String heartbeatJson() {
        return "{\"id\":\"__heartbeat__\",\"status\":\"HEARTBEAT\"}";
    }
}
