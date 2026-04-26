package com.yue.opsagent.springai.agent.registry;

import cn.bugstack.wrench.design.framework.link.model2.DynamicContext;
import com.yue.opsagent.springai.agent.sub.ISubAgent;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class AgentToolContext extends DynamicContext {

    private final String runId;
    private final Map<String, ISubAgent> agents;
    private final Instant startedAt = Instant.now();
    private ISubAgent agent;
    private String result;
    private String phase;
    private String outcome;
    private String errorMessage;

    public AgentToolContext(String runId, Map<String, ISubAgent> agents) {
        this.runId = runId;
        this.agents = Map.copyOf(agents);
    }

    public String getRunId() {
        return runId;
    }

    public Map<String, ISubAgent> getAgents() {
        return agents;
    }

    public ISubAgent getAgent() {
        return agent;
    }

    public void setAgent(ISubAgent agent) {
        this.agent = agent;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public boolean hasResult() {
        return result != null;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long durationMs() {
        return Duration.between(startedAt, Instant.now()).toMillis();
    }

    public boolean hasRunId() {
        return runId != null && !runId.isBlank();
    }
}
