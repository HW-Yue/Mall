package com.yue.opsagent.springai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.opsagent.springai.domain.opsroute.OpsRunEvent;
import com.yue.opsagent.springai.domain.opsroute.OpsRunSession;
import com.yue.opsagent.springai.domain.opsroute.OpsRunStatus;
import com.yue.opsagent.springai.domain.opsroute.OpsRunSummary;
import com.yue.opsagent.springai.domain.opsroute.RouteInputType;
import com.yue.opsagent.springai.domain.opsroute.RouteRequest;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class OpsRunService {

    private static final Logger log = LoggerFactory.getLogger(OpsRunService.class);

    private final ObjectMapper objectMapper;
    private final OpsRunSummaryRepository summaryRepository;
    private final Map<String, MutableRun> runs = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> subscribers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ops-run-sse-heartbeat");
        t.setDaemon(true);
        return t;
    });

    @Autowired
    public OpsRunService(ObjectMapper objectMapper, OpsRunSummaryRepository summaryRepository) {
        this.objectMapper = objectMapper;
        this.summaryRepository = summaryRepository;
    }

    public OpsRunService(ObjectMapper objectMapper) {
        this(objectMapper, OpsRunSummaryRepository.noop());
    }

    @PostConstruct
    void startHeartbeat() {
        heartbeatExecutor.scheduleAtFixedRate(this::broadcastHeartbeats, 25, 25, TimeUnit.SECONDS);
    }

    @PreDestroy
    void stopHeartbeat() {
        heartbeatExecutor.shutdownNow();
    }

    public OpsRunSession create(RouteRequest request) {
        String runId = UUID.randomUUID().toString();
        MutableRun run = new MutableRun(runId, request.inputType());
        runs.put(runId, run);
        addEvent(runId, "start", "Root", "运行已创建", Map.of("inputType", request.inputType()));
        return snapshot(runId).orElseThrow();
    }

    public Optional<OpsRunSession> snapshot(String runId) {
        MutableRun run = runs.get(runId);
        return run == null ? Optional.empty() : Optional.of(run.snapshot());
    }

    public List<OpsRunSummary> recentRuns(int size) {
        int limit = Math.max(1, Math.min(size, 200));
        return runs.values().stream()
                .map(MutableRun::snapshot)
                .sorted(Comparator.comparing(OpsRunSession::updatedAt).reversed())
                .limit(limit)
                .map(session -> JdbcOpsRunSummaryRepository.toSummary(session, "memory"))
                .toList();
    }

    public void node(String runId, String node, String message) {
        MutableRun run = runs.get(runId);
        if (run != null) {
            run.currentNode(node);
            addEvent(runId, "node", node, message, Map.of());
        }
    }

    public void complete(String runId, String node, String message, Map<String, Object> data) {
        terminal(runId, OpsRunStatus.COMPLETED, "end", node, message, data);
    }

    public void fail(String runId, String message) {
        terminal(runId, OpsRunStatus.FAILED, "failed", "Error", message, Map.of());
    }

    public void reject(String runId, String approvalId, String reason) {
        terminal(runId, OpsRunStatus.REJECTED, "approval_rejected", "Approval", reason,
                Map.of("approvalId", approvalId));
    }

    public OpsRunSession cancel(String runId) {
        terminal(runId, OpsRunStatus.CANCELLED, "cancelled", "User", "用户暂停运行", Map.of());
        return snapshot(runId).orElse(null);
    }

    public boolean isCancelled(String runId) {
        MutableRun run = runs.get(runId);
        return run != null && run.status() == OpsRunStatus.CANCELLED;
    }

    public void waitingApproval(String runId, String approvalId, Map<String, Object> data) {
        MutableRun run = runs.get(runId);
        if (run != null) {
            run.status(OpsRunStatus.WAITING_APPROVAL);
            run.currentNode("Approval");
            addEvent(runId, "waiting_approval", "Approval", "等待人工审批: " + approvalId, data);
        }
    }

    public void approvedToolResult(String runId, String approvalId, Map<String, Object> result) {
        MutableRun run = runs.get(runId);
        if (run != null && run.status() == OpsRunStatus.WAITING_APPROVAL) {
            run.status(OpsRunStatus.COMPLETED);
            run.currentNode("Approval");
            addEvent(runId, "approval_approved", "Approval", "审批通过并执行完成",
                    Map.of("approvalId", approvalId, "result", result == null ? Map.of() : result));
        }
    }

    public void toolStart(String runId, String skill, String tool, Map<String, Object> args) {
        addEvent(runId, "tool_start", skill, "调用工具 " + tool,
                Map.of("skill", skill, "tool", tool, "args", args == null ? Map.of() : args));
    }

    public void toolEnd(String runId, String skill, String tool, Map<String, Object> result) {
        addEvent(runId, "tool_end", skill, "工具返回 " + tool,
                Map.of("skill", skill, "tool", tool, "result", result == null ? Map.of() : result));
    }

    public void toolResult(String runId, String skill, String tool, Map<String, Object> event) {
        addEvent(runId, "tool_result", skill, "工具调用结果 " + tool,
                event == null ? Map.of() : event);
    }

    public void subAgentResult(String runId, String agentTool, Map<String, Object> event) {
        addEvent(runId, "sub_agent_result", agentTool, "子Agent调用结果 " + agentTool,
                event == null ? Map.of() : event);
    }

    public void event(String runId, String type, String node, String message, Map<String, Object> data) {
        addEvent(runId, type, node, message, data == null ? Map.of() : data);
    }

    public SseEmitter subscribe(String runId) {
        SseEmitter emitter = new SseEmitter(3_600_000L);
        subscribers.computeIfAbsent(runId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        Runnable remove = () -> subscribers.getOrDefault(runId, new CopyOnWriteArrayList<>()).remove(emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        snapshot(runId).ifPresent(s -> s.events().forEach(event -> send(emitter, event)));
        return emitter;
    }

    private void terminal(String runId, OpsRunStatus status, String type, String node, String message, Map<String, Object> data) {
        MutableRun run = runs.get(runId);
        if (run == null || isTerminal(run.status())) {
            return;
        }
        run.status(status);
        run.currentNode(node);
        addEvent(runId, type, node, message, data);
    }

    private void addEvent(String runId, String type, String node, String message, Map<String, Object> data) {
        MutableRun run = runs.get(runId);
        if (run == null) {
            return;
        }
        OpsRunEvent event = OpsRunEvent.of(type, node, message, data);
        run.add(event);
        persistSummary(run.snapshot());
        for (SseEmitter emitter : subscribers.getOrDefault(runId, new CopyOnWriteArrayList<>())) {
            send(emitter, event);
        }
        log.info("[OpsRunEvent] {} {} {}",
                StructuredArguments.kv("runId", runId),
                StructuredArguments.kv("eventType", type),
                StructuredArguments.kv("node", node),
                StructuredArguments.kv("eventMessage", message),
                StructuredArguments.kv("data", data == null ? Map.of() : data));
    }

    private void send(SseEmitter emitter, Object payload) {
        try {
            emitter.send(SseEmitter.event().data(toJson(payload)));
        } catch (IOException e) {
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        }
    }

    private String toJson(Object payload) throws JsonProcessingException {
        return objectMapper.writeValueAsString(payload);
    }

    private void persistSummary(OpsRunSession session) {
        try {
            summaryRepository.upsert(session);
        } catch (Exception e) {
            log.warn("[OpsRunSummary] persist failed runId={} err={}", session.runId(), e.toString());
        }
    }

    private void broadcastHeartbeats() {
        subscribers.values().forEach(list -> list.forEach(emitter -> send(emitter,
                Map.of("type", "heartbeat", "at", Instant.now()))));
    }

    private static boolean isTerminal(OpsRunStatus status) {
        return status == OpsRunStatus.COMPLETED
                || status == OpsRunStatus.FAILED
                || status == OpsRunStatus.REJECTED
                || status == OpsRunStatus.CANCELLED;
    }

    private static final class MutableRun {
        private final String runId;
        private final RouteInputType inputType;
        private final Instant createdAt = Instant.now();
        private final List<OpsRunEvent> events = new ArrayList<>();
        private OpsRunStatus status = OpsRunStatus.RUNNING;
        private String currentNode = "Root";
        private Instant updatedAt = createdAt;

        private MutableRun(String runId, RouteInputType inputType) {
            this.runId = runId;
            this.inputType = inputType;
        }

        synchronized void status(OpsRunStatus status) {
            this.status = status;
            this.updatedAt = Instant.now();
        }

        synchronized OpsRunStatus status() {
            return status;
        }

        synchronized void currentNode(String currentNode) {
            this.currentNode = currentNode;
            this.updatedAt = Instant.now();
        }

        synchronized void add(OpsRunEvent event) {
            events.add(event);
            updatedAt = event.at();
        }

        synchronized OpsRunSession snapshot() {
            return new OpsRunSession(runId, inputType, status, currentNode, createdAt, updatedAt, List.copyOf(events));
        }
    }
}
