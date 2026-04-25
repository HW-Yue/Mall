package com.yue.opsagent.springai.trigger;

import com.yue.opsagent.springai.service.OpsRouteService;
import com.yue.opsagent.springai.service.OpsRunService;
import com.yue.opsagent.springai.domain.opsroute.OpsRunSession;
import com.yue.opsagent.springai.domain.opsroute.RouteRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ops")
public class OpsRouteController {

    private final OpsRouteService opsRouteService;
    private final OpsRunService opsRunService;

    public OpsRouteController(OpsRouteService opsRouteService, OpsRunService opsRunService) {
        this.opsRouteService = opsRouteService;
        this.opsRunService = opsRunService;
    }

    @PostMapping(value = "/route-text", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Map<String, Object> routeText(@RequestBody String text) {
        return opsRouteService.routeAsync(RouteRequest.text(text)).toMap();
    }

    @GetMapping("/runs/{runId}")
    public Map<String, Object> getRun(@PathVariable String runId) {
        return opsRunService.snapshot(runId)
                .map(OpsRunSession::toMap)
                .orElseGet(() -> Map.of("status", "error", "message", "run 不存在: " + runId));
    }

    @GetMapping(value = "/runs/{runId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable String runId) {
        return opsRunService.subscribe(runId);
    }

    @PostMapping("/runs/{runId}/cancel")
    public Map<String, Object> cancel(@PathVariable String runId) {
        OpsRunSession session = opsRunService.cancel(runId);
        if (session == null) {
            return Map.of("status", "error", "message", "run 不存在: " + runId);
        }
        return session.toMap();
    }
}
