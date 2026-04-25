package com.yue.opsagent.springai.alert;

import java.util.Map;

public record AlertEvent(
        String status,
        String alertname,
        String severity,
        String application,
        Map<String, String> labels,
        Map<String, String> annotations
) {}
