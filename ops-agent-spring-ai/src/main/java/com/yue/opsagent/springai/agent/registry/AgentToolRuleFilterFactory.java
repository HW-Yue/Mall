package com.yue.opsagent.springai.agent.registry;

import cn.bugstack.wrench.design.framework.link.model2.LinkArmory;
import cn.bugstack.wrench.design.framework.link.model2.chain.BusinessLinkedList;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AgentToolRuleFilterFactory {

    @Bean("agentToolRuleFilter")
    public BusinessLinkedList<AgentToolCommand, AgentToolContext, String> agentToolRuleFilter(
            List<AgentToolRuleFilter> filters) {
        List<AgentToolRuleFilter> ordered = filters.stream()
                .sorted(AnnotationAwareOrderComparator.INSTANCE)
                .toList();
        LinkArmory<AgentToolCommand, AgentToolContext, String> linkArmory =
                new LinkArmory<>(
                        "父Agent委派子Agent规则链",
                        ordered.toArray(AgentToolRuleFilter[]::new));
        return linkArmory.getLogicLink();
    }
}
