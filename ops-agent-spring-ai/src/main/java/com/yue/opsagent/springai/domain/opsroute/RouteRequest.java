package com.yue.opsagent.springai.domain.opsroute;

import com.yue.opsagent.springai.domain.alert.AlertEvent;

public record RouteRequest(
        RouteInputType inputType,
        AlertEvent alertEvent,
        String text
) {
    public static RouteRequest alert(AlertEvent event) {
        return new RouteRequest(RouteInputType.ALERT, event, null);
    }

    public static RouteRequest text(String text) {
        return new RouteRequest(RouteInputType.TEXT, null, text);
    }
}
