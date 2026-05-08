package com.yue.opsagent.springai.domain.alert;

import java.util.List;

public record AlertSignals(
        List<String> applications,
        List<String> resources,
        List<String> topics,
        List<String> consumerGroups,
        List<String> tables,
        List<String> databases,
        List<String> pools,
        List<String> rawTexts
) {
}
