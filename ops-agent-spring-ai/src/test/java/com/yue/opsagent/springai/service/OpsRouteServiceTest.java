package com.yue.opsagent.springai.service;

import com.yue.opsagent.springai.agent.parent.OpsAgent;
import com.yue.opsagent.springai.domain.alert.AlertEnrichmentService;
import com.yue.opsagent.springai.domain.alert.AlertEvent;
import com.yue.opsagent.springai.domain.alert.EnrichedAlertContext;
import com.yue.opsagent.springai.domain.alert.SopDispatcher;
import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OpsRouteServiceTest {

    @Test
    void routeTextUsesReactWhenSopMatchesEvenIfAlertModeIsDeterministic() {
        OpsRunService opsRunService = mock(OpsRunService.class);
        SopDispatcher sopDispatcher = mock(SopDispatcher.class);
        SopAiMatcherService sopAiMatcherService = mock(SopAiMatcherService.class);
        SopStepRunner sopStepRunner = mock(SopStepRunner.class);
        OpsAgent opsAgent = mock(OpsAgent.class);
        AlertEnrichmentService alertEnrichmentService = mock(AlertEnrichmentService.class);
        OpsAiProperties props = new OpsAiProperties();
        OpsRoutingPolicyService opsRoutingPolicyService = new OpsRoutingPolicyService(props);

        OpsRouteService service = new OpsRouteService(
                opsRunService,
                sopDispatcher,
                sopAiMatcherService,
                sopStepRunner,
                opsAgent,
                opsRoutingPolicyService,
                alertEnrichmentService);

        EnrichedAlertContext enrichment = EnrichedAlertContext.empty();
        OpsAiProperties.Sop.Rule rule = new OpsAiProperties.Sop.Rule();
        rule.setSopMarkdown("参考 SOP");
        OpsAiProperties.Sop.Step step = new OpsAiProperties.Sop.Step();
        step.setType("delegate_subagent");
        step.setSubAgentId("metrics_ops");
        step.setTask("查询 consumer 错误率");
        rule.setSteps(List.of(step));
        SopAiMatcherService.MatchResult match = new SopAiMatcherService.MatchResult(
                0, 0.82d, "命中 Dubbo 文本 SOP", rule);

        when(alertEnrichmentService.enrich(any(AlertEvent.class))).thenReturn(enrichment);
        when(sopAiMatcherService.matchEvent(any(AlertEvent.class), eq(enrichment))).thenReturn(Optional.of(match));
        when(opsAgent.runForText(anyString(), any(AlertEvent.class), eq(enrichment), anyMap(), eq("参考 SOP")))
                .thenReturn("text react summary");

        ReflectionTestUtils.invokeMethod(service, "routeText", "run-text-match", "下单接口 Dubbo 调用失败");

        verifyNoInteractions(sopDispatcher);
        verify(sopStepRunner, never()).run(any(AlertEvent.class), any(EnrichedAlertContext.class), anyList());
        verify(opsAgent).runForText(eq("下单接口 Dubbo 调用失败"), any(AlertEvent.class), eq(enrichment), anyMap(), eq("参考 SOP"));
        verify(opsRunService).node("run-text-match", "ReactExecute", "已找到可参考 SOP，进入文本 ReAct 编排");

        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(opsRunService).complete(eq("run-text-match"), eq("End"), eq("文本 ReAct 编排完成"), dataCaptor.capture());
        Map<String, Object> data = dataCaptor.getValue();
        assertThat(data).containsEntry("summary", "text react summary");
        assertThat(data).containsEntry("enrichment", enrichment);
        @SuppressWarnings("unchecked")
        Map<String, Object> matchData = (Map<String, Object>) data.get("match");
        assertThat(matchData).containsEntry("matched", true);
        assertThat(matchData).containsEntry("reason", "命中 Dubbo 文本 SOP");
    }

    @Test
    void routeTextWithoutSopStillRunsGenericReact() {
        OpsRunService opsRunService = mock(OpsRunService.class);
        SopDispatcher sopDispatcher = mock(SopDispatcher.class);
        SopAiMatcherService sopAiMatcherService = mock(SopAiMatcherService.class);
        SopStepRunner sopStepRunner = mock(SopStepRunner.class);
        OpsAgent opsAgent = mock(OpsAgent.class);
        AlertEnrichmentService alertEnrichmentService = mock(AlertEnrichmentService.class);

        OpsRouteService service = new OpsRouteService(
                opsRunService,
                sopDispatcher,
                sopAiMatcherService,
                sopStepRunner,
                opsAgent,
                new OpsRoutingPolicyService(new OpsAiProperties()),
                alertEnrichmentService);

        EnrichedAlertContext enrichment = EnrichedAlertContext.empty();
        when(alertEnrichmentService.enrich(any(AlertEvent.class))).thenReturn(enrichment);
        when(sopAiMatcherService.matchEvent(any(AlertEvent.class), eq(enrichment))).thenReturn(Optional.empty());
        when(opsAgent.runForText(anyString(), any(AlertEvent.class), eq(enrichment), anyMap(), eq("")))
                .thenReturn("generic react summary");

        ReflectionTestUtils.invokeMethod(service, "routeText", "run-text-generic", "下单失败，需要排查日志和依赖");

        verifyNoInteractions(sopDispatcher);
        verify(sopStepRunner, never()).run(any(AlertEvent.class), any(EnrichedAlertContext.class), anyList());
        verify(opsAgent).runForText(eq("下单失败，需要排查日志和依赖"), any(AlertEvent.class), eq(enrichment), anyMap(), eq(""));
        verify(opsRunService).node("run-text-generic", "ReactExecute", "未命中 SOP，直接进入文本 ReAct 编排");

        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(opsRunService).complete(eq("run-text-generic"), eq("End"), eq("文本 ReAct 编排完成"), dataCaptor.capture());
        Map<String, Object> data = dataCaptor.getValue();
        assertThat(data).containsEntry("summary", "generic react summary");
        @SuppressWarnings("unchecked")
        Map<String, Object> matchData = (Map<String, Object>) data.get("match");
        assertThat(matchData).containsEntry("matched", false);
        assertThat(matchData).containsEntry("reason", "未命中可参考 SOP，改走通用文本 ReAct");
    }

    @Test
    void routeAlertKeepsLockedExecutionWhenAutonomousPlanningIsDisabled() {
        OpsRunService opsRunService = mock(OpsRunService.class);
        SopDispatcher sopDispatcher = mock(SopDispatcher.class);
        SopAiMatcherService sopAiMatcherService = mock(SopAiMatcherService.class);
        SopStepRunner sopStepRunner = mock(SopStepRunner.class);
        OpsAgent opsAgent = mock(OpsAgent.class);
        AlertEnrichmentService alertEnrichmentService = mock(AlertEnrichmentService.class);
        OpsAiProperties props = new OpsAiProperties();
        OpsRoutingPolicyService opsRoutingPolicyService = new OpsRoutingPolicyService(props);

        OpsRouteService service = new OpsRouteService(
                opsRunService,
                sopDispatcher,
                sopAiMatcherService,
                sopStepRunner,
                opsAgent,
                opsRoutingPolicyService,
                alertEnrichmentService);

        AlertEvent event = new AlertEvent(
                "firing",
                "DubboConsumerErrorRateHigh",
                "critical",
                "order-service",
                Map.of("category", "dubbo"),
                Map.of("summary", "consumer error rate high"));
        EnrichedAlertContext enrichment = new EnrichedAlertContext(
                "order-service",
                List.of("order-service"),
                "",
                "",
                "",
                "",
                "",
                "",
                "application/app 直接命中",
                Map.of("application", "order-service"),
                Map.of());
        OpsAiProperties.Sop.Rule rule = new OpsAiProperties.Sop.Rule();
        rule.setMatchAlertname("DubboConsumerErrorRateHigh");
        OpsAiProperties.Sop.Step step = new OpsAiProperties.Sop.Step();
        step.setType("delegate_subagent");
        step.setSubAgentId("metrics_ops");
        step.setTask("查询 consumer 失败率");
        rule.setSteps(List.of(step));

        when(alertEnrichmentService.enrich(event)).thenReturn(enrichment);
        when(sopDispatcher.matchRule(event, enrichment)).thenReturn(Optional.of(rule));
        when(sopStepRunner.run(eq(event), eq(enrichment), anyList()))
                .thenReturn(List.of(Map.of("type", "delegate_subagent", "summary", "metrics ok")));

        ReflectionTestUtils.invokeMethod(service, "routeAlert", "run-alert-det", event);

        verify(sopStepRunner).run(eq(event), eq(enrichment), anyList());
        verify(opsAgent, never()).runForAlert(any(AlertEvent.class), any(EnrichedAlertContext.class), any(OpsAiProperties.Sop.Rule.class));
        verify(opsAgent, never()).runForText(anyString(), any(AlertEvent.class), any(EnrichedAlertContext.class), anyMap(), anyString());
        verify(opsRunService).node("run-alert-det", "LockedSopExecute", "预警自主规划关闭，按硬匹配 SOP 固定执行");

        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(opsRunService).complete(eq("run-alert-det"), eq("End"), eq("SOP 步骤执行完成"), dataCaptor.capture());
        assertThat(dataCaptor.getValue()).containsKey("result");
        assertThat((List<?>) dataCaptor.getValue().get("result")).hasSize(1);
    }

    @Test
    void routeAlertStopsImmediatelyWhenHardMatchMissesAndAutonomousPlanningIsDisabled() {
        OpsRunService opsRunService = mock(OpsRunService.class);
        SopDispatcher sopDispatcher = mock(SopDispatcher.class);
        SopAiMatcherService sopAiMatcherService = mock(SopAiMatcherService.class);
        SopStepRunner sopStepRunner = mock(SopStepRunner.class);
        OpsAgent opsAgent = mock(OpsAgent.class);
        AlertEnrichmentService alertEnrichmentService = mock(AlertEnrichmentService.class);
        OpsRoutingPolicyService opsRoutingPolicyService = new OpsRoutingPolicyService(new OpsAiProperties());

        OpsRouteService service = new OpsRouteService(
                opsRunService,
                sopDispatcher,
                sopAiMatcherService,
                sopStepRunner,
                opsAgent,
                opsRoutingPolicyService,
                alertEnrichmentService);

        AlertEvent event = new AlertEvent("firing", "UnknownAlert", "warning", "mall-service", Map.of("category", "http"), Map.of());
        EnrichedAlertContext enrichment = EnrichedAlertContext.empty();
        when(alertEnrichmentService.enrich(event)).thenReturn(enrichment);
        when(sopDispatcher.matchRule(event, enrichment)).thenReturn(Optional.empty());

        ReflectionTestUtils.invokeMethod(service, "routeAlert", "run-alert-stop", event);

        verify(sopAiMatcherService, never()).matchEvent(any(AlertEvent.class), any(EnrichedAlertContext.class));
        verifyNoInteractions(sopStepRunner);
        verifyNoInteractions(opsAgent);
        verify(opsRunService).complete(eq("run-alert-stop"), eq("End"), eq("未命中硬匹配 SOP，运行结束"), anyMap());
    }

    @Test
    void routeAlertUsesAutonomousPlanningWhenSwitchIsEnabled() {
        OpsRunService opsRunService = mock(OpsRunService.class);
        SopDispatcher sopDispatcher = mock(SopDispatcher.class);
        SopAiMatcherService sopAiMatcherService = mock(SopAiMatcherService.class);
        SopStepRunner sopStepRunner = mock(SopStepRunner.class);
        OpsAgent opsAgent = mock(OpsAgent.class);
        AlertEnrichmentService alertEnrichmentService = mock(AlertEnrichmentService.class);
        OpsAiProperties props = new OpsAiProperties();
        props.getAlert().setAutonomousPlanningEnabled(true);
        OpsRoutingPolicyService opsRoutingPolicyService = new OpsRoutingPolicyService(props);

        OpsRouteService service = new OpsRouteService(
                opsRunService,
                sopDispatcher,
                sopAiMatcherService,
                sopStepRunner,
                opsAgent,
                opsRoutingPolicyService,
                alertEnrichmentService);

        AlertEvent event = new AlertEvent(
                "firing",
                "Http5xxErrorRateHigh",
                "critical",
                "mall-service",
                Map.of("category", "http"),
                Map.of("summary", "5xx high"));
        EnrichedAlertContext enrichment = EnrichedAlertContext.empty();
        OpsAiProperties.Sop.Rule rule = new OpsAiProperties.Sop.Rule();
        rule.setSopMarkdown("HTTP 5xx 参考 SOP");

        when(alertEnrichmentService.enrich(event)).thenReturn(enrichment);
        when(sopDispatcher.matchRule(event, enrichment)).thenReturn(Optional.of(rule));
        when(opsAgent.runForAlert(event, enrichment, rule)).thenReturn("autonomous summary");

        ReflectionTestUtils.invokeMethod(service, "routeAlert", "run-alert-react", event);

        verify(sopStepRunner, never()).run(any(AlertEvent.class), any(EnrichedAlertContext.class), anyList());
        verify(opsAgent).runForAlert(event, enrichment, rule);
        verify(opsRunService).node("run-alert-react", "ReactExecute", "预警自主规划开启，按参考 SOP 进入 ReAct 编排");
        verify(opsRunService).complete(eq("run-alert-react"), eq("End"), eq("ReAct 编排完成"), anyMap());
    }
}
