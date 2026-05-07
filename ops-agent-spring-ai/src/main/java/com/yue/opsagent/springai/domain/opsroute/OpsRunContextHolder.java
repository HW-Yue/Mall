package com.yue.opsagent.springai.domain.opsroute;

import org.slf4j.MDC;

public final class OpsRunContextHolder {

    public static final String MDC_KEY = "runId";

    private static final ThreadLocal<String> RUN_ID = new ThreadLocal<>();

    private OpsRunContextHolder() {
    }

    public static void set(String runId) {
        if (runId == null || runId.isBlank()) {
            RUN_ID.remove();
            MDC.remove(MDC_KEY);
        } else {
            RUN_ID.set(runId);
            MDC.put(MDC_KEY, runId);
        }
    }

    public static String get() {
        return RUN_ID.get();
    }

    public static void clear() {
        RUN_ID.remove();
        MDC.remove(MDC_KEY);
    }
}
