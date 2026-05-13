package com.yue.opsagent.springai.trigger;

import com.yue.opsagent.springai.service.OpsRouteService;
import com.yue.opsagent.springai.service.OpsRunHistoryService;
import com.yue.opsagent.springai.service.OpsRunService;
import com.yue.opsagent.springai.domain.opsroute.OpsRunSession;
import com.yue.opsagent.springai.domain.opsroute.OpsRunSummary;
import com.yue.opsagent.springai.domain.opsroute.RouteRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ops")
public class OpsRouteController {

    private final OpsRouteService opsRouteService;
    private final OpsRunService opsRunService;
    private final OpsRunHistoryService opsRunHistoryService;

    public OpsRouteController(
            OpsRouteService opsRouteService,
            OpsRunService opsRunService,
            OpsRunHistoryService opsRunHistoryService) {
        this.opsRouteService = opsRouteService;
        this.opsRunService = opsRunService;
        this.opsRunHistoryService = opsRunHistoryService;
    }

    @PostMapping(value = "/route-text", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Map<String, Object> routeText(@RequestBody String text) {
        return opsRouteService.routeAsync(RouteRequest.text(text)).toMap();
    }

    @GetMapping("/runs/{runId:[0-9a-fA-F\\-]{36}}")
    public Map<String, Object> getRun(@PathVariable String runId) {
        return opsRunService.snapshot(runId)
                .map(OpsRunSession::toMap)
                .orElseGet(() -> Map.of("status", "error", "message", "run 不存在: " + runId));
    }

    @GetMapping(value = "/runs/{runId:[0-9a-fA-F\\-]{36}}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable String runId) {
        return opsRunService.subscribe(runId);
    }

    @GetMapping("/runs/{runId:[0-9a-fA-F\\-]{36}}/timeline")
    public Map<String, Object> timeline(@PathVariable String runId) {
        List<Map<String, Object>> events = opsRunHistoryService.timeline(runId);
        return Map.of("runId", runId, "events", events, "count", events.size());
    }

    @GetMapping("/runs/recent")
    public Map<String, Object> recent(@RequestParam(defaultValue = "50") int size) {
        List<Map<String, Object>> runs = opsRunService.recentRuns(size).stream()
                .map(OpsRunSummary::toMap)
                .toList();
        return Map.of("runs", runs, "count", runs.size());
    }

    @GetMapping("/runs/history")
    public Map<String, Object> history(@RequestParam(defaultValue = "50") int size) {
        List<Map<String, Object>> runs = opsRunHistoryService.recentRuns(size).stream()
                .map(OpsRunSummary::toMap)
                .toList();
        return Map.of("runs", runs, "count", runs.size());
    }

    @PostMapping("/runs/{runId:[0-9a-fA-F\\-]{36}}/cancel")
    public Map<String, Object> cancel(@PathVariable String runId) {
        OpsRunSession session = opsRunService.cancel(runId);
        if (session == null) {
            return Map.of("status", "error", "message", "run 不存在: " + runId);
        }
        return session.toMap();
    }
}
