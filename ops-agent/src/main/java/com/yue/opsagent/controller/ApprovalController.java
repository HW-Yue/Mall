package com.yue.opsagent.controller;

import com.yue.opsagent.app.ApprovalService;
import com.yue.opsagent.domain.model.ApprovalStatus;
import com.yue.opsagent.domain.model.ApprovalTask;
import com.yue.opsagent.domain.port.ApprovalInboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 审批收件箱 REST + SSE。
 *
 * <ul>
 *   <li>GET  /api/v1/approvals           列表（query: status）</li>
 *   <li>GET  /api/v1/approvals/{id}      详情</li>
 *   <li>POST /api/v1/approvals/{id}/approve  同意并 apply 到 Nacos</li>
 *   <li>POST /api/v1/approvals/{id}/reject   拒绝</li>
 *   <li>GET  /api/v1/approvals/stream    SSE；新任务和状态变更都会推送</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalController {

    private static final Logger log = LoggerFactory.getLogger(ApprovalController.class);

    private final ApprovalInboxPort inbox;
    private final ApprovalService approvalService;

    public ApprovalController(ApprovalInboxPort inbox, ApprovalService approvalService) {
        this.inbox = inbox;
        this.approvalService = approvalService;
    }

    @GetMapping
    public List<ApprovalTask> list(@RequestParam(value = "status", required = false) String status) {
        ApprovalStatus parsed = null;
        if (status != null && !status.isBlank()) {
            try {
                parsed = ApprovalStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusExceptionShim("非法 status: " + status);
            }
        }
        return inbox.list(parsed);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApprovalTask> detail(@PathVariable String id) {
        return inbox.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/approve")
    public ApprovalTask approve(@PathVariable String id) {
        log.info("[Approval] APPROVE id={}", id);
        return approvalService.approve(id);
    }

    @PostMapping("/{id}/reject")
    public ApprovalTask reject(@PathVariable String id,
                               @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        log.info("[Approval] REJECT id={} reason={}", id, reason);
        return approvalService.reject(id, reason);
    }

    /**
     * SSE 订阅：<b>初始快照 + 后续增量</b>。浏览器打开 index.html#inbox 后立即渲染已有 PENDING 任务，
     * 然后实时接收新增 / 状态变更事件。
     */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ApprovalTask> stream() {
        Flux<ApprovalTask> snapshot = Flux.fromIterable(inbox.list(null));
        Flux<ApprovalTask> incremental = inbox.stream();
        Flux<ApprovalTask> heartbeat = Flux.interval(Duration.ofSeconds(15))
                .map(i -> HEARTBEAT);
        return Flux.concat(snapshot, Flux.merge(incremental, heartbeat));
    }

    /** 心跳哨兵（前端过滤掉 id=__heartbeat__） */
    private static final ApprovalTask HEARTBEAT = new ApprovalTask(
            "__heartbeat__", null, null, null, null, null, null, null, null, null,
            ApprovalStatus.PENDING, null, null, null);

    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    static class ResponseStatusExceptionShim extends RuntimeException {
        ResponseStatusExceptionShim(String msg) { super(msg); }
    }
}
