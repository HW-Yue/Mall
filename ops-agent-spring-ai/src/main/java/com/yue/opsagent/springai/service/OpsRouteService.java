package com.yue.opsagent.springai.service;

import com.yue.opsagent.springai.agent.parent.OpsAgent;
import com.yue.opsagent.springai.agent.react.ReactRunResult;
import com.yue.opsagent.springai.domain.alert.AlertEvent;
import com.yue.opsagent.springai.domain.alert.AlertEnrichmentService;
import com.yue.opsagent.springai.domain.alert.EnrichedAlertContext;
import com.yue.opsagent.springai.domain.alert.SopDispatcher;
import com.yue.opsagent.springai.domain.opsroute.OpsRunEvent;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
        ReactRunResult react = opsAgent.runForAlert(event, enrichment, rule);
        if (!opsRunService.isCancelled(runId)) {
            opsRunService.complete(runId, "End", "ReAct 编排完成",
                    buildReactCompletionData(runId, eventSummary(event), matchData, enrichment, policy, react));
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
        ReactRunResult react = opsAgent.runForText(text, event, enrichment, matchData, sopMarkdown);
        if (!opsRunService.isCancelled(runId)) {
            opsRunService.complete(runId, "End", "文本 ReAct 编排完成",
                    buildReactCompletionData(runId, text, matchData, enrichment, policy, react));
        }
    }

    private Map<String, Object> buildReactCompletionData(
            String runId,
            String requestText,
            Map<String, Object> matchData,
            EnrichedAlertContext enrichment,
            RoutePolicySnapshot policy,
            ReactRunResult react) {
        SummaryPayload payload = resolveSummaryPayload(runId, requestText, enrichment, react);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("match", matchData == null ? Map.of() : matchData);
        data.put("enrichment", enrichment);
        data.put("summary", payload.summary());
        data.put("summarySource", payload.summarySource());
        data.put("converged", payload.converged());
        data.put("finishReason", payload.finishReason());
        data.put("policy", policy.toMap());
        return data;
    }

    private SummaryPayload resolveSummaryPayload(
            String runId,
            String requestText,
            EnrichedAlertContext enrichment,
            ReactRunResult react) {
        if (react != null && react.converged()) {
            return new SummaryPayload(
                    nullToEmpty(react.answer()),
                    "agent_final",
                    true,
                    nullToEmpty(react.finishReason()));
        }
        String finishReason = react == null ? "unknown" : nullToEmpty(react.finishReason());
        return new SummaryPayload(
                buildFallbackSummary(runId, requestText, enrichment),
                "fallback_on_max_iters",
                false,
                finishReason);
    }

    private String buildFallbackSummary(String runId, String requestText, EnrichedAlertContext enrichment) {
        OpsRunSession snapshot = opsRunService.snapshot(runId).orElse(null);
        List<OpsRunEvent> events = snapshot == null ? List.of() : snapshot.events();
        List<String> relatedServices = collectRelatedServices(enrichment, events);
        boolean listedServices = hasSuccessfulTool(events, "catalog_ops", "catalog_list_services");
        List<String> evidence = collectFallbackEvidence(events);

        String conclusion;
        if (!relatedServices.isEmpty()) {
            conclusion = "当前更值得优先排查的服务是 " + joinWithComma(relatedServices, 3) + "。";
        } else if (listedServices) {
            conclusion = "当前文本描述比较模糊，系统已先查询服务清单，但本轮还没有稳定收敛到唯一服务。";
        } else {
            conclusion = "当前文本描述比较模糊，本轮还没有稳定定位到明确服务。";
        }

        String evidenceText = evidence.isEmpty()
                ? defaultFallbackEvidence(requestText, listedServices)
                : String.join("；", evidence);
        String nextStep = relatedServices.isEmpty()
                ? "继续使用 Catalog Skill 从服务清单里筛出候选服务，并对候选服务补查 application、容器名、Topic、数据库名。"
                : "继续围绕 " + relatedServices.getFirst()
                + " 补查 application、Docker 容器、Prometheus 指标和 Nacos 实例；若仍无异常，再沿相关 Topic / 数据库继续排查。";

        return "结论：" + conclusion
                + "\n证据：" + evidenceText
                + "\n下一步：" + nextStep
                + "\n补充：主 Agent 达到最大轮次，以上为基于当前事件生成的兜底总结。";
    }

    private List<String> collectRelatedServices(EnrichedAlertContext enrichment, List<OpsRunEvent> events) {
        LinkedHashSet<String> services = new LinkedHashSet<>();
        if (enrichment != null) {
            addIfNotBlank(services, enrichment.primaryService());
            if (enrichment.candidateServices() != null) {
                enrichment.candidateServices().forEach(service -> addIfNotBlank(services, service));
            }
        }
        for (OpsRunEvent event : events) {
            if (!"tool_result".equals(event.type())) {
                continue;
            }
            Map<String, Object> data = mapValue(event.data());
            String tool = stringValue(data.get("tool"));
            Map<String, Object> result = mapValue(data.get("result"));
            Map<String, Object> resultData = mapValue(result.get("data"));
            if ("catalog_describe_service".equals(tool)) {
                addIfNotBlank(services, stringValue(resultData.get("service")));
            } else if ("catalog_resolve_service".equals(tool)) {
                addIfNotBlank(services, stringValue(resultData.get("primaryService")));
                listValue(resultData.get("candidateServices")).forEach(service -> addIfNotBlank(services, service));
            }
        }
        return List.copyOf(services);
    }

    private boolean hasSuccessfulTool(List<OpsRunEvent> events, String skill, String tool) {
        for (OpsRunEvent event : events) {
            if (!"tool_result".equals(event.type())) {
                continue;
            }
            Map<String, Object> data = mapValue(event.data());
            if (skill.equals(stringValue(data.get("skill")))
                    && tool.equals(stringValue(data.get("tool")))
                    && "success".equals(stringValue(data.get("outcome")))) {
                return true;
            }
        }
        return false;
    }

    private List<String> collectFallbackEvidence(List<OpsRunEvent> events) {
        List<String> evidence = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (int i = events.size() - 1; i >= 0 && evidence.size() < 3; i--) {
            String line = summarizeEvent(events.get(i));
            if (line == null || line.isBlank() || !seen.add(line)) {
                continue;
            }
            evidence.add(line);
        }
        return evidence;
    }

    private String summarizeEvent(OpsRunEvent event) {
        if (event == null) {
            return "";
        }
        if ("sub_agent_result".equals(event.type())) {
            String result = normalizeInlineText(stringValue(mapValue(event.data()).get("result")), 120);
            return result.isBlank() ? "" : "子域返回：" + result;
        }
        if ("tool_result".equals(event.type())) {
            Map<String, Object> data = mapValue(event.data());
            String tool = stringValue(data.get("tool"));
            String outcome = stringValue(data.get("outcome"));
            Map<String, Object> result = mapValue(data.get("result"));
            Map<String, Object> resultData = mapValue(result.get("data"));
            if (!"success".equals(outcome)) {
                String message = normalizeInlineText(stringValue(data.get("message")), 100);
                return message.isBlank() ? "" : "工具结果：" + message;
            }
            return switch (tool) {
                case "catalog_list_services" -> "已获取当前服务清单（" + listValue(resultData.get("services")).size() + " 个服务）";
                case "catalog_list_topics" -> "已获取当前 Topic 清单（" + listValue(resultData.get("topics")).size() + " 个 Topic）";
                case "catalog_describe_service" -> summarizeDescribeService(resultData);
                case "catalog_resolve_service" -> {
                    String primary = stringValue(resultData.get("primaryService"));
                    yield primary.isBlank() ? "Catalog 已执行服务归因" : "Catalog 归因候选服务：" + primary;
                }
                default -> {
                    String message = normalizeInlineText(stringValue(data.get("message")), 100);
                    yield message.isBlank() ? "" : "已完成 " + tool + "：" + message;
                }
            };
        }
        if ("waiting_approval".equals(event.type())) {
            return "存在待审批操作：" + normalizeInlineText(event.message(), 100);
        }
        if ("failed".equals(event.type())) {
            return "运行失败：" + normalizeInlineText(event.message(), 100);
        }
        return "";
    }

    private String summarizeDescribeService(Map<String, Object> resultData) {
        if (!Boolean.parseBoolean(stringValue(resultData.get("found")))) {
            String input = stringValue(resultData.get("input"));
            return input.isBlank()
                    ? "Catalog 未命中标准服务名"
                    : "Catalog 未命中标准服务名（input=" + input + "）";
        }
        String service = stringValue(resultData.get("service"));
        Map<String, Object> profile = mapValue(resultData.get("profile"));
        String application = stringValue(profile.get("application"));
        String container = stringValue(profile.get("containerName"));
        int configEntryCount = listValue(profile.get("configEntries")).size();
        if (service.isBlank()) {
            return "Catalog 返回的服务拓扑缺少标准服务名";
        }
        List<String> details = new ArrayList<>();
        if (!application.isBlank()) {
            details.add("application=" + application);
        }
        if (!container.isBlank()) {
            details.add("container=" + container);
        }
        if (configEntryCount > 0) {
            details.add("configEntries=" + configEntryCount);
        }
        if (details.isEmpty()) {
            return "已获取 " + service + " 的静态拓扑";
        }
        return "已获取 " + service + " 的静态拓扑（" + String.join(", ", details) + "）";
    }

    private String defaultFallbackEvidence(String requestText, boolean listedServices) {
        if (listedServices) {
            return "已先根据模糊文本查询当前服务清单，但本轮工具结果还不足以收敛到唯一服务。";
        }
        if (requestText == null || requestText.isBlank()) {
            return "当前输入没有给出足够的结构化线索。";
        }
        return "当前文本是“" + normalizeInlineText(requestText, 48) + "”，但还缺少明确服务名或可直接归因的静态线索。";
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

    private static String eventSummary(AlertEvent event) {
        if (event == null) {
            return "";
        }
        String summary = event.annotations() == null ? "" : nullToEmpty(event.annotations().get("summary"));
        if (!summary.isBlank()) {
            return summary;
        }
        return nullToEmpty(event.alertname());
    }

    private static Map<String, Object> mapValue(Object value) {
        if (!(value instanceof Map<?, ?> raw)) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (entry.getKey() != null) {
                out.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return out;
    }

    private static List<String> listValue(Object value) {
        if (!(value instanceof List<?> raw)) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Object item : raw) {
            if (item == null) {
                continue;
            }
            String text = String.valueOf(item).trim();
            if (!text.isBlank()) {
                out.add(text);
            }
        }
        return List.copyOf(out);
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static void addIfNotBlank(LinkedHashSet<String> values, String value) {
        if (value != null && !value.isBlank()) {
            values.add(value);
        }
    }

    private static String joinWithComma(List<String> values, int limit) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        List<String> target = values.size() <= limit ? values : values.subList(0, limit);
        return String.join("、", target);
    }

    private static String normalizeInlineText(String value, int maxChars) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxChars ? normalized : normalized.substring(0, maxChars) + "...";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record SummaryPayload(
            String summary,
            String summarySource,
            boolean converged,
            String finishReason
    ) {
    }
}
