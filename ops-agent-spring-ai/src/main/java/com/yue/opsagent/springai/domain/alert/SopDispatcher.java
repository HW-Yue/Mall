package com.yue.opsagent.springai.domain.alert;

import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SopDispatcher {

    private final OpsAiProperties props;

    public SopDispatcher(OpsAiProperties props) {
        this.props = props;
    }

    public Optional<OpsAiProperties.Sop.Rule> matchRule(AlertEvent event) {
        return matchRule(event, EnrichedAlertContext.empty());
    }

    public Optional<OpsAiProperties.Sop.Rule> matchRule(AlertEvent event, EnrichedAlertContext enrichment) {
        return props.getSop().getRules().stream()
                .filter(this::ruleHasAnyMatcher)
                .filter(r -> matchesRule(r, event, enrichment))
                .findFirst();
    }

    public Optional<List<OpsAiProperties.Sop.Step>> match(AlertEvent event) {
        return match(event, EnrichedAlertContext.empty());
    }

    public Optional<List<OpsAiProperties.Sop.Step>> match(AlertEvent event, EnrichedAlertContext enrichment) {
        return matchRule(event, enrichment).map(OpsAiProperties.Sop.Rule::getSteps);
    }

    private boolean ruleHasAnyMatcher(OpsAiProperties.Sop.Rule r) {
        return notBlank(r.getMatchAlertname())
                || notBlank(r.getMatchCategory())
                || notBlank(r.getMatchSeverity())
                || notBlank(r.getMatchApplication())
                || notBlank(r.getMatchService())
                || notBlank(r.getMatchResourcePrefix())
                || notBlank(r.getMatchTopic())
                || notBlank(r.getMatchConsumerGroup())
                || notBlank(r.getMatchTable())
                || notBlank(r.getMatchDb());
    }

    private boolean matchesRule(OpsAiProperties.Sop.Rule r, AlertEvent e, EnrichedAlertContext enrichment) {
        if (notBlank(r.getMatchAlertname())
                && !r.getMatchAlertname().equalsIgnoreCase(AlertPlaceholderResolver.nullToEmpty(e.alertname()))) {
            return false;
        }
        if (notBlank(r.getMatchSeverity())
                && !r.getMatchSeverity().equalsIgnoreCase(AlertPlaceholderResolver.nullToEmpty(e.severity()))) {
            return false;
        }
        if (notBlank(r.getMatchApplication())
                && !r.getMatchApplication().equalsIgnoreCase(AlertPlaceholderResolver.nullToEmpty(e.application()))
                && !r.getMatchApplication().equalsIgnoreCase(AlertPlaceholderResolver.nullToEmpty(enrichment.primaryService()))) {
            return false;
        }
        if (notBlank(r.getMatchService())
                && !r.getMatchService().equalsIgnoreCase(AlertPlaceholderResolver.nullToEmpty(enrichment.primaryService()))) {
            return false;
        }
        if (notBlank(r.getMatchCategory())) {
            String cat = e.labels() == null ? "" : e.labels().getOrDefault("category", "");
            if (!r.getMatchCategory().equalsIgnoreCase(cat)) {
                return false;
            }
        }
        if (notBlank(r.getMatchResourcePrefix())
                && !AlertPlaceholderResolver.nullToEmpty(enrichment.resource()).startsWith(r.getMatchResourcePrefix())) {
            return false;
        }
        if (notBlank(r.getMatchTopic())
                && !r.getMatchTopic().equalsIgnoreCase(AlertPlaceholderResolver.nullToEmpty(enrichment.topic()))) {
            return false;
        }
        if (notBlank(r.getMatchConsumerGroup())
                && !r.getMatchConsumerGroup().equalsIgnoreCase(AlertPlaceholderResolver.nullToEmpty(enrichment.consumerGroup()))) {
            return false;
        }
        if (notBlank(r.getMatchTable())
                && !r.getMatchTable().equalsIgnoreCase(AlertPlaceholderResolver.nullToEmpty(enrichment.table()))) {
            return false;
        }
        if (notBlank(r.getMatchDb())
                && !r.getMatchDb().equalsIgnoreCase(AlertPlaceholderResolver.nullToEmpty(enrichment.database()))) {
            return false;
        }
        return true;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
