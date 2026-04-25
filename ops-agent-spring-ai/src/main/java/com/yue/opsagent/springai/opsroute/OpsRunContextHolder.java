package com.yue.opsagent.springai.opsroute;

public final class OpsRunContextHolder {

    private static final ThreadLocal<String> RUN_ID = new ThreadLocal<>();

    private OpsRunContextHolder() {
    }

    public static void set(String runId) {
        if (runId == null || runId.isBlank()) {
            RUN_ID.remove();
        } else {
            RUN_ID.set(runId);
        }
    }

    public static String get() {
        return RUN_ID.get();
    }

    public static void clear() {
        RUN_ID.remove();
    }
}
