package com.yue.opsagent.springai.service;

import com.yue.opsagent.springai.agent.parent.OpsAgent;
import com.yue.opsagent.springai.domain.alert.AlertEvent;
import com.yue.opsagent.springai.domain.alert.SopDispatcher;
import com.yue.opsagent.springai.domain.opsroute.OpsRunContextHolder;
import com.yue.opsagent.springai.domain.opsroute.OpsRunSession;
import com.yue.opsagent.springai.domain.opsroute.RouteInputType;
import com.yue.opsagent.springai.domain.opsroute.RouteRequest;
import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class OpsRouteService {

    private static final Logger log = LoggerFactory.getLogger(OpsRouteService.class);

    private final OpsRunService opsRunService;
    private final SopDispatcher sopDispatcher;
    private final SopAiMatcherService sopAiMatcherService;
    private final SopStepRunner sopStepRunner;
    private final OpsAgent opsAgent;
    private final OpsAiProperties opsAiProperties;
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "ops-route-worker");
        t.setDaemon(true);
        return t;
    });

    public OpsRouteService(
            OpsRunService opsRunService,
            SopDispatcher sopDispatcher,
            SopAiMatcherService sopAiMatcherService,
            SopStepRunner sopStepRunner,
            OpsAgent opsAgent,
            OpsAiProperties opsAiProperties) {
        this.opsRunService = opsRunService;
        this.sopDispatcher = sopDispatcher;
        this.sopAiMatcherService = sopAiMatcherService;
        this.sopStepRunner = sopStepRunner;
        this.opsAgent = opsAgent;
        this.opsAiProperties = opsAiProperties;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    public OpsRunSession routeAsync(RouteRequest request) {
        OpsRunSession session = opsRunService.create(request);
        executor.submit(() -> process(session.runId(), request));
        return session;
    }

    private void process(String runId, RouteRequest request) {
        OpsRunContextHolder.set(runId);
        try {
            if (opsRunService.isCancelled(runId)) {
                return;
            }
            if (request.inputType() == RouteInputType.TEXT) {
                routeText(runId, request.text());
            } else {
                routeAlert(runId, request.alertEvent());
            }
        } catch (Exception e) {
            log.warn("[OpsRoute] run failed runId={} err={}", runId, e.toString(), e);
            opsRunService.fail(runId, e.getMessage() == null ? e.toString() : e.getMessage());
        } finally {
            OpsRunContextHolder.clear();
        }
    }

    private void routeAlert(String runId, AlertEvent event) {
        if (event == null) {
            opsRunService.fail(runId, "告警事件为空");
            return;
        }
        opsRunService.node(runId, "HardSopMatch", "按 alertname/category/severity/application 匹配 SOP");
        Optional<OpsAiProperties.Sop.Rule> hard = sopDispatcher.matchRule(event);
        OpsAiProperties.Sop.Rule rule;
        Map<String, Object> matchData;
        if (hard.isPresent()) {
            rule = hard.get();
            matchData = Map.of("source", "hard", "matchAlertname", nullToEmpty(rule.getMatchAlertname()));
            opsRunService.node(runId, "HardSopMatch", "命中硬匹配 SOP");
        } else {
            opsRunService.node(runId, "AiSopMatch", "硬匹配未命中，使用 AI 在已有 SOP 中选择");
            Optional<SopAiMatcherService.MatchResult> ai = sopAiMatcherService.matchEvent(event);
            if (ai.isEmpty()) {
                opsRunService.complete(runId, "End", "未找到匹配 SOP，运行结束",
                        Map.of("matched", false, "alertname", nullToEmpty(event.alertname())));
                return;
            }
            rule = ai.get().rule();
            matchData = ai.get().toMap();
        }
        executeMatchedRule(runId, event, rule, matchData);
    }

    private void routeText(String runId, String text) {
        if (text == null || text.isBlank()) {
            opsRunService.fail(runId, "纯文本输入为空");
            return;
        }
        opsRunService.node(runId, "AiSopMatch", "根据纯文本在已有 SOP 中选择");
        Optional<SopAiMatcherService.MatchResult> ai = sopAiMatcherService.matchText(text);
        if (ai.isEmpty()) {
            opsRunService.complete(runId, "PlanDraft", "未命中已有 SOP，生成排查草案",
                    Map.of("matched", false, "draft", buildDraft(text)));
            return;
        }
        OpsAiProperties.Sop.Rule rule = ai.get().rule();
        AlertEvent event = new AlertEvent(
                "firing",
                firstNonBlank(rule.getMatchAlertname(), "PlainTextOpsRequest"),
                firstNonBlank(rule.getMatchSeverity(), "unknown"),
                firstNonBlank(rule.getMatchApplication(), ""),
                Map.of("category", firstNonBlank(rule.getMatchCategory(), "text")),
                Map.of("summary", text));
        executeMatchedRule(runId, event, rule, ai.get().toMap());
    }

    private void executeMatchedRule(
            String runId,
            AlertEvent event,
            OpsAiProperties.Sop.Rule rule,
            Map<String, Object> matchData) {
        if (opsRunService.isCancelled(runId)) {
            return;
        }
        opsRunService.node(runId, "ReactExecute", "开始执行命中的 SOP");
        String mode = opsAiProperties.getAlert().getMode();
        if ("deterministic".equalsIgnoreCase(mode) && rule.getSteps() != null && !rule.getSteps().isEmpty()) {
            var result = sopStepRunner.run(event, rule.getSteps());
            if (!opsRunService.isCancelled(runId)) {
                opsRunService.complete(runId, "End", "SOP 步骤执行完成",
                        Map.of("match", matchData, "result", result));
            }
            return;
        }
        String summary = opsAgent.runForAlert(event, rule);
        if (!opsRunService.isCancelled(runId)) {
            opsRunService.complete(runId, "End", "ReAct 编排完成",
                    Map.of("match", matchData, "summary", summary == null ? "" : summary));
        }
    }

    private static String buildDraft(String text) {
        return "建议先确认服务是否存在并有健康实例，再按指标、日志、依赖组件、最近变更的顺序排查。输入摘要: " + text;
    }

    private static String firstNonBlank(String a, String b) {
        return a == null || a.isBlank() ? b : a;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
