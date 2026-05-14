package com.yue.opsagent.springai.agent.react;

public record ReactRunResult(
        String answer,
        boolean converged,
        String finishReason
) {

    public static ReactRunResult finalAnswer(String answer) {
        return new ReactRunResult(answer == null ? "" : answer, true, "final");
    }

    public static ReactRunResult maxIters(String answer) {
        return new ReactRunResult(answer == null ? "" : answer, false, "max_iters");
    }
}
