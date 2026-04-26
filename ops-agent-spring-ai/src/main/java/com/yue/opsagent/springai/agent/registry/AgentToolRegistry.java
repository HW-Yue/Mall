package com.yue.opsagent.springai.agent.registry;

import cn.bugstack.wrench.design.framework.link.model2.chain.BusinessLinkedList;
import com.yue.opsagent.springai.agent.react.ReactTool;
import com.yue.opsagent.springai.agent.sub.ISubAgent;
import com.yue.opsagent.springai.domain.opsroute.OpsRunContextHolder;
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

    public List<ReactTool> reactTools(java.util.function.Supplier<Map<String, Object>> contextSupplier) {
        return primaryAgents.stream()
                .<ReactTool>map(agent -> new ReactTool() {
                    @Override
                    public String name() {
                        return agent.parentToolName();
                    }

                    @Override
                    public String description() {
                        return agent.parentToolDescription();
                    }

                    @Override
                    public String execute(Map<String, Object> args, Map<String, Object> context) {
                        String task = "";
                        if (args != null && args.get("task") != null) {
                            task = String.valueOf(args.get("task"));
                        }
                        Map<String, Object> inherited = contextSupplier.get();
                        return AgentToolRegistry.this.execute(agent.parentToolName(), task, inherited);
                    }
                })
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
