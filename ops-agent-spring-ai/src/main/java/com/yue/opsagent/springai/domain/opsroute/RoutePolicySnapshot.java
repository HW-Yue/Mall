package com.yue.opsagent.springai.domain.opsroute;

import java.util.Map;

public record RoutePolicySnapshot(
        boolean alertAutonomousPlanningEnabled,
        boolean textAutonomousPlanningEnabled,
        String alertMode,
        String textMode,
        String updatedAt,
        String alertDescription,
        String textDescription
) {
    public Map<String, Object> toMap() {
        return Map.of(
                "alertAutonomousPlanningEnabled", alertAutonomousPlanningEnabled,
                "textAutonomousPlanningEnabled", textAutonomousPlanningEnabled,
                "alertMode", alertMode,
                "textMode", textMode,
                "updatedAt", updatedAt == null ? "" : updatedAt,
                "alertDescription", alertDescription == null ? "" : alertDescription,
                "textDescription", textDescription == null ? "" : textDescription);
    }
}
