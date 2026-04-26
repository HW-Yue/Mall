package com.yue.opsagent.springai.skill.registry;

import cn.bugstack.wrench.design.framework.link.model2.LinkArmory;
import cn.bugstack.wrench.design.framework.link.model2.chain.BusinessLinkedList;
import com.yue.opsagent.springai.skill.api.ToolResult;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Assembles the tool execution rule chain with Wrench, matching the group-buy chain style.
 */
@Component
public class ToolExecutionRuleFilterFactory {

    @Bean("toolExecutionRuleFilter")
    public BusinessLinkedList<ToolExecutionCommand, ToolExecutionContext, ToolResult> toolExecutionRuleFilter(
            List<ToolExecutionRuleFilter> filters) {
        List<ToolExecutionRuleFilter> ordered = filters.stream()
                .sorted(AnnotationAwareOrderComparator.INSTANCE)
                .toList();
        LinkArmory<ToolExecutionCommand, ToolExecutionContext, ToolResult> linkArmory =
                new LinkArmory<>(
                        "Agent工具执行规则链",
                        ordered.toArray(ToolExecutionRuleFilter[]::new));
        return linkArmory.getLogicLink();
    }
}
