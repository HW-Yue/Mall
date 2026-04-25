package com.yue.opsagent.springai.alert;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AlertmanagerPayload(
        String status,
        List<AlertmanagerAlert> alerts
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AlertmanagerAlert(
            String status,
            Map<String, String> labels,
            Map<String, String> annotations,
            String startsAt,
            String endsAt
    ) {}
}
