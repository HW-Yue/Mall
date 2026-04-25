package com.yue.opsagent.springai.trigger;

import com.yue.opsagent.springai.service.ApprovalService;
import com.yue.opsagent.springai.domain.approval.ApprovalPresenter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    /** 全量（含已处理），供前端收件箱同步 */
    @GetMapping
    public List<Map<String, Object>> all() {
        return approvalService.listAll().stream()
                .map(ApprovalPresenter::toTaskView)
                .collect(Collectors.toList());
    }

    @GetMapping("/pending")
    public List<Map<String, Object>> pending() {
        return approvalService.listPending().stream()
                .map(ApprovalPresenter::toTaskView)
                .collect(Collectors.toList());
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return approvalService.subscribeApprovalsStream();
    }

    @PostMapping("/{id}/approve")
    public Map<String, Object> approve(@PathVariable String id) {
        return approvalService.approveAndRun(id);
    }

    @PostMapping("/{id}/reject")
    public Map<String, Object> reject(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null && body.get("reason") != null ? body.get("reason") : "前端手工拒绝";
        return approvalService.reject(id, reason);
    }
}
