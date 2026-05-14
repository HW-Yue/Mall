package com.yue.opsagent.springai.service;

import com.yue.opsagent.springai.agent.parent.OpsAgent;
import com.yue.opsagent.springai.domain.alert.AlertEvent;
import com.yue.opsagent.springai.domain.alert.AlertEnrichmentService;
import com.yue.opsagent.springai.domain.alert.EnrichedAlertContext;
import com.yue.opsagent.springai.domain.alert.SopDispatcher;
import com.yue.opsagent.springai.domain.opsroute.OpsRunContextHolder;
import com.yue.opsagent.springai.domain.opsroute.OpsRunSession;
import com.yue.opsagent.springai.domain.opsroute.RoutePolicySnapshot;
import com.yue.opsagent.springai.domain.opsroute.RouteInputType;
import com.yue.opsagent.springai.domain.opsroute.RouteRequest;
import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
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
    private final OpsRoutingPolicyService opsRoutingPolicyService;
    private final AlertEnrichmentService alertEnrichmentService;
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
            OpsRoutingPolicyService opsRoutingPolicyService,
            AlertEnrichmentService alertEnrichmentService) {
        this.opsRunService = opsRunService;
        this.sopDispatcher = sopDispatcher;
        this.sopAiMatcherService = sopAiMatcherService;
        this.sopStepRunner = sopStepRunner;
        this.opsAgent = opsAgent;
        this.opsRoutingPolicyService = opsRoutingPolicyService;
        this.alertEnrichmentService = alertEnrichmentService;
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
        opsRunService.event(runId, "signal_extract", "SignalExtract", "提取告警信号",
                Map.of("labels", event.labels() == null ? Map.of() : event.labels()));
        EnrichedAlertContext enrichment = alertEnrichmentService.enrich(event);
        opsRunService.event(runId, "alert_enrich", "AlertEnrich", "完成服务归因与上下文增强",
                Map.of(
                        "primaryService", enrichment.primaryService(),
                        "candidateServices", enrichment.candidateServices(),
                        "resolvedLabels", enrichment.resolvedLabels(),
                        "evidence", enrichment.evidence()));
        RoutePolicySnapshot policy = opsRoutingPolicyService.snapshot();
        opsRunService.event(runId, "route_policy", "RoutePolicy",
                policy.alertAutonomousPlanningEnabled()
                        ? "当前预警策略：允许参考 SOP 自主规划"
                        : "当前预警策略：仅允许硬匹配",
                policy.toMap());
        opsRunService.node(runId, "HardSopMatch", "按 alertname/category/severity/application 匹配 SOP");
        Optional<OpsAiProperties.Sop.Rule> hard = sopDispatcher.matchRule(event, enrichment);
        OpsAiProperties.Sop.Rule rule;
        Map<String, Object> matchData;
        if (hard.isPresent()) {
            rule = hard.get();
            matchData = Map.of(
                    "source", "hard",
                    "matchAlertname", nullToEmpty(rule.getMatchAlertname()),
                    "primaryService", nullToEmpty(enrichment.primaryService()));
            opsRunService.node(runId, "HardSopMatch", "命中硬匹配 SOP");
        } else {
            if (!policy.alertAutonomousPlanningEnabled()) {
                opsRunService.complete(runId, "End", "未命中硬匹配 SOP，运行结束",
                        Map.of(
                                "matched", false,
                                "source", "hard_only",
                                "reason", "预警自主规划开关关闭，仅允许硬匹配",
                                "alertname", nullToEmpty(event.alertname()),
                                "enrichment", enrichment,
                                "policy", policy.toMap()));
                return;
            }
            opsRunService.node(runId, "AiSopMatch", "硬匹配未命中，使用 AI 在已有 SOP 中选择");
            Optional<SopAiMatcherService.MatchResult> ai = sopAiMatcherService.matchEvent(event, enrichment);
            if (ai.isEmpty()) {
                opsRunService.complete(runId, "End", "未找到匹配 SOP，运行结束",
                        Map.of(
                                "matched", false,
                                "alertname", nullToEmpty(event.alertname()),
                                "enrichment", enrichment,
                                "policy", policy.toMap()));
                return;
            }
            rule = ai.get().rule();
            matchData = ai.get().toMap();
        }
        executeMatchedRule(runId, event, enrichment, rule, matchData, policy);
    }

    private void routeText(String runId, String text) {
        if (text == null || text.isBlank()) {
            opsRunService.fail(runId, "纯文本输入为空");
            return;
        }
        AlertEvent event = buildTextEvent(text);
        EnrichedAlertContext enrichment = alertEnrichmentService.enrich(event);
        opsRunService.event(runId, "alert_enrich", "AlertEnrich", "根据文本推断服务上下文",
                Map.of(
                        "primaryService", enrichment.primaryService(),
                        "candidateServices", enrichment.candidateServices(),
                        "resolvedLabels", enrichment.resolvedLabels(),
                        "evidence", enrichment.evidence()));
        RoutePolicySnapshot policy = opsRoutingPolicyService.snapshot();
        opsRunService.event(runId, "route_policy", "RoutePolicy", "纯文本请求固定允许自主规划", policy.toMap());
        opsRunService.node(runId, "AiSopMatch", "根据纯文本与增强上下文在已有 SOP 中选择");
        Optional<SopAiMatcherService.MatchResult> ai = sopAiMatcherService.matchEvent(event, enrichment);

        Map<String, Object> matchData = ai.map(SopAiMatcherService.MatchResult::toMap)
                .orElseGet(OpsRouteService::unmatchedTextMatchData);
        String sopMarkdown = ai.map(SopAiMatcherService.MatchResult::rule)
                .map(OpsAiProperties.Sop.Rule::getSopMarkdown)
                .orElse("");

        String matchMessage = ai.isPresent()
                ? "已找到可参考 SOP，进入文本 ReAct 编排"
                : "未命中 SOP，直接进入文本 ReAct 编排";
        opsRunService.node(runId, "ReactExecute", matchMessage);
        executeTextReact(runId, text, event, enrichment, matchData, sopMarkdown, policy);
    }

    private void executeMatchedRule(
            String runId,
            AlertEvent event,
            EnrichedAlertContext enrichment,
            OpsAiProperties.Sop.Rule rule,
            Map<String, Object> matchData,
            RoutePolicySnapshot policy) {
        if (opsRunService.isCancelled(runId)) {
            return;
        }
        if (!policy.alertAutonomousPlanningEnabled()) {
            if (rule.getSteps() == null || rule.getSteps().isEmpty()) {
                opsRunService.complete(runId, "End", "命中硬匹配 SOP，但未配置固定步骤，运行结束",
                        Map.of("match", matchData, "enrichment", enrichment, "policy", policy.toMap()));
                return;
            }
            opsRunService.node(runId, "LockedSopExecute", "预警自主规划关闭，按硬匹配 SOP 固定执行");
            var result = sopStepRunner.run(event, enrichment, rule.getSteps());
            if (!opsRunService.isCancelled(runId)) {
                opsRunService.complete(runId, "End", "SOP 步骤执行完成",
                        Map.of("match", matchData, "enrichment", enrichment, "result", result, "policy", policy.toMap()));
            }
            return;
        }
        opsRunService.node(runId, "ReactExecute", "预警自主规划开启，按参考 SOP 进入 ReAct 编排");
        String summary = opsAgent.runForAlert(event, enrichment, rule);
        if (!opsRunService.isCancelled(runId)) {
            opsRunService.complete(runId, "End", "ReAct 编排完成",
                    Map.of("match", matchData, "enrichment", enrichment, "summary", summary == null ? "" : summary, "policy", policy.toMap()));
        }
    }

    private void executeTextReact(
            String runId,
            String text,
            AlertEvent event,
            EnrichedAlertContext enrichment,
            Map<String, Object> matchData,
            String sopMarkdown,
            RoutePolicySnapshot policy) {
        if (opsRunService.isCancelled(runId)) {
            return;
        }
        String summary = opsAgent.runForText(text, event, enrichment, matchData, sopMarkdown);
        if (!opsRunService.isCancelled(runId)) {
            opsRunService.complete(runId, "End", "文本 ReAct 编排完成",
                    Map.of("match", matchData, "enrichment", enrichment, "summary", summary == null ? "" : summary, "policy", policy.toMap()));
        }
    }

    private static AlertEvent buildTextEvent(String text) {
        return new AlertEvent(
                "firing",
                "PlainTextOpsRequest",
                "unknown",
                "",
                Map.of("category", "text"),
                Map.of("summary", text));
    }

    private static Map<String, Object> unmatchedTextMatchData() {
        Map<String, Object> matchData = new HashMap<>();
        matchData.put("matched", false);
        matchData.put("reason", "未命中可参考 SOP，改走通用文本 ReAct");
        return Map.copyOf(matchData);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
