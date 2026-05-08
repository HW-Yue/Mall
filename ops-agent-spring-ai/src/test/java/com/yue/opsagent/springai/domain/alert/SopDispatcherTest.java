package com.yue.opsagent.springai.domain.alert;

import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SopDispatcherTest {

    @Test
    void matchesAlertnameAndCategory() {
        OpsAiProperties props = new OpsAiProperties();
        OpsAiProperties.Sop.Rule r = new OpsAiProperties.Sop.Rule();
        r.setMatchAlertname("HighCpu");
        r.setMatchCategory("infra");
        OpsAiProperties.Sop.Step step = new OpsAiProperties.Sop.Step();
        step.setSkill("metrics_ops");
        step.setTool("jvm_metrics");
        step.getArgs().put("promql", "up");
        r.setSteps(List.of(step));
        props.getSop().setRules(List.of(r));

        SopDispatcher d = new SopDispatcher(props);
        var ev = new AlertEvent(
                "firing",
                "HighCpu",
                "critical",
                "app1",
                Map.of("category", "infra", "alertname", "HighCpu"),
                Map.of());
        assertThat(d.match(ev)).isPresent();
        assertThat(d.match(ev).get()).hasSize(1);

        var evWrongCat = new AlertEvent(
                "firing",
                "HighCpu",
                "critical",
                "app1",
                Map.of("category", "app", "alertname", "HighCpu"),
                Map.of());
        assertThat(d.match(evWrongCat)).isEmpty();
    }

    @Test
    void matchRuleIncludesSopMarkdown() {
        OpsAiProperties props = new OpsAiProperties();
        OpsAiProperties.Sop.Rule r = new OpsAiProperties.Sop.Rule();
        r.setMatchAlertname("DiskFull");
        r.setSopMarkdown("## SOP\n清理日志。");
        props.getSop().setRules(List.of(r));

        SopDispatcher d = new SopDispatcher(props);
        var ev = new AlertEvent(
                "firing",
                "DiskFull",
                "warning",
                "app1",
                Map.of("alertname", "DiskFull"),
                Map.of());
        assertThat(d.matchRule(ev)).isPresent();
        assertThat(d.matchRule(ev).get().getSopMarkdown()).contains("清理日志");
    }

    @Test
    void matchesByEnrichedServiceAndTopic() {
        OpsAiProperties props = new OpsAiProperties();
        OpsAiProperties.Sop.Rule r = new OpsAiProperties.Sop.Rule();
        r.setMatchCategory("rocketmq");
        r.setMatchService("order-service");
        r.setMatchTopic("group-buy-order-create");
        props.getSop().setRules(List.of(r));

        SopDispatcher dispatcher = new SopDispatcher(props);
        AlertEvent event = new AlertEvent(
                "firing",
                "RocketMqConsumerLagHigh",
                "warning",
                "shared",
                Map.of("category", "rocketmq"),
                Map.of());
        EnrichedAlertContext enrichment = new EnrichedAlertContext(
                "order-service",
                List.of("order-service"),
                "",
                "group-buy-order-create",
                "",
                "",
                "",
                "",
                "topic 命中",
                Map.of("topic", "group-buy-order-create"),
                Map.of());

        assertThat(dispatcher.matchRule(event, enrichment)).isPresent();
    }
}
