package com.yue.opsagent.springai.alert;

import com.yue.opsagent.springai.config.OpsAiProperties;
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
        return props.getSop().getRules().stream()
                .filter(this::ruleHasAnyMatcher)
                .filter(r -> matchesRule(r, event))
                .findFirst();
    }

    public Optional<List<OpsAiProperties.Sop.Step>> match(AlertEvent event) {
        return matchRule(event).map(OpsAiProperties.Sop.Rule::getSteps);
    }

    private boolean ruleHasAnyMatcher(OpsAiProperties.Sop.Rule r) {
        return notBlank(r.getMatchAlertname())
                || notBlank(r.getMatchCategory())
                || notBlank(r.getMatchSeverity())
                || notBlank(r.getMatchApplication());
    }

    private boolean matchesRule(OpsAiProperties.Sop.Rule r, AlertEvent e) {
        if (notBlank(r.getMatchAlertname())
                && !r.getMatchAlertname().equalsIgnoreCase(AlertPlaceholderResolver.nullToEmpty(e.alertname()))) {
            return false;
        }
        if (notBlank(r.getMatchSeverity())
                && !r.getMatchSeverity().equalsIgnoreCase(AlertPlaceholderResolver.nullToEmpty(e.severity()))) {
            return false;
        }
        if (notBlank(r.getMatchApplication())
                && !r.getMatchApplication().equalsIgnoreCase(AlertPlaceholderResolver.nullToEmpty(e.application()))) {
            return false;
        }
        if (notBlank(r.getMatchCategory())) {
            String cat = e.labels() == null ? "" : e.labels().getOrDefault("category", "");
            if (!r.getMatchCategory().equalsIgnoreCase(cat)) {
                return false;
            }
        }
        return true;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
