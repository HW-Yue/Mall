package com.yue.opsagent.springai.agent.registry;

import cn.bugstack.wrench.design.framework.link.model2.chain.BusinessLinkedList;
import com.yue.opsagent.springai.agent.parent.DelegateTask;
import com.yue.opsagent.springai.agent.sub.ISubAgent;
import com.yue.opsagent.springai.domain.opsroute.OpsRunContextHolder;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AgentToolRegistry {

    private final Map<String, ISubAgent> agents;
    private final List<ISubAgent> primaryAgents;
    private final BusinessLinkedList<AgentToolCommand, AgentToolContext, String> agentToolRuleFilter;

    public AgentToolRegistry(
            Collection<ISubAgent> agents,
            BusinessLinkedList<AgentToolCommand, AgentToolContext, String> agentToolRuleFilter) {
        this.agents = buildAgentIndex(agents);
        this.primaryAgents = List.copyOf(agents);
        this.agentToolRuleFilter = agentToolRuleFilter;
    }

    public String execute(String agentToolName, String task, Map<String, Object> context) {
        AgentToolCommand command = new AgentToolCommand(agentToolName, task, context);
        AgentToolContext chainContext = new AgentToolContext(OpsRunContextHolder.get(), agents);
        try {
            return agentToolRuleFilter.apply(command, chainContext);
        } catch (Exception e) {
            return "子Agent委派失败: " + (e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    public List<ToolCallback> toolCallbacks(java.util.function.Supplier<Map<String, Object>> contextSupplier) {
        return primaryAgents.stream()
                .map(agent -> FunctionToolCallback.builder(
                                agent.parentToolName(),
                                (DelegateTask t) -> execute(
                                        agent.parentToolName(),
                                        t.task() == null ? "" : t.task(),
                                        contextSupplier.get()))
                        .description(agent.parentToolDescription())
                        .inputType(DelegateTask.class)
                        .build())
                .map(ToolCallback.class::cast)
                .toList();
    }

    public String buildMenu() {
        return primaryAgents.stream()
                .map(a -> "- " + a.parentDisplayName() + " (tool: " + a.parentToolName() + "): "
                        + a.parentToolDescription())
                .collect(Collectors.joining("\n"));
    }

    private static Map<String, ISubAgent> buildAgentIndex(Collection<ISubAgent> agents) {
        Map<String, ISubAgent> index = new LinkedHashMap<>();
        for (ISubAgent agent : agents) {
            index.put(agent.parentToolName(), agent);
            index.putIfAbsent(agent.domainId(), agent);
        }
        return Collections.unmodifiableMap(index);
    }
}
