package com.yue.opsagent.springai.service;

import com.yue.opsagent.springai.domain.opsroute.RoutePolicySnapshot;
import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class OpsRoutingPolicyService {

    private static final boolean TEXT_AUTONOMOUS_PLANNING_ENABLED = true;

    private final AtomicBoolean alertAutonomousPlanningEnabled;
    private final AtomicReference<Instant> updatedAt;

    public OpsRoutingPolicyService(OpsAiProperties props) {
        boolean initial = props.getAlert().isAutonomousPlanningEnabled()
                || "react".equalsIgnoreCase(props.getAlert().getMode());
        this.alertAutonomousPlanningEnabled = new AtomicBoolean(initial);
        this.updatedAt = new AtomicReference<>(Instant.now());
    }

    public boolean isAlertAutonomousPlanningEnabled() {
        return alertAutonomousPlanningEnabled.get();
    }

    public RoutePolicySnapshot snapshot() {
        boolean alertEnabled = alertAutonomousPlanningEnabled.get();
        return new RoutePolicySnapshot(
                alertEnabled,
                TEXT_AUTONOMOUS_PLANNING_ENABLED,
                alertEnabled ? "AUTONOMOUS_PLANNING" : "HARD_MATCH_ONLY",
                "AUTONOMOUS_PLANNING",
                updatedAt.get().toString(),
                alertEnabled
                        ? "结构化预警先硬匹配；未命中时允许参考 SOP 自主规划。"
                        : "结构化预警仅允许硬匹配；未命中时直接结束。",
                "纯文本请求固定允许参考 SOP 自主规划。");
    }

    public RoutePolicySnapshot updateAlertAutonomousPlanning(boolean enabled) {
        alertAutonomousPlanningEnabled.set(enabled);
        updatedAt.set(Instant.now());
        return snapshot();
    }
}
