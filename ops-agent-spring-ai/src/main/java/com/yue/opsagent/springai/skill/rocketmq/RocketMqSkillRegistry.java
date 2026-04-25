package com.yue.opsagent.springai.skill.rocketmq;

import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class RocketMqSkillRegistry implements OpsSkillRegistry {

    public static final String SKILL_NAME = "rocketmq_inspect";

    private final RocketMqToolkit toolkit;

    public RocketMqSkillRegistry(RocketMqToolkit toolkit) {
        this.toolkit = toolkit;
    }

    @Override
    public String name() {
        return SKILL_NAME;
    }

    @Override
    public String description() {
        return "RocketMQ 只读：Topic 路由、消费进度、死信排查提示";
    }

    @Override
    public String promptFragment() {
        return """
                - mq_topic_stats: topic(string 可选，空则列 topic)
                - mq_consumer_status: consumerGroup(string 必填), topic(string 可选)
                - mq_dead_letter: topic(string 可选), consumerGroup(string 可选)，返回 DLQ 排查提示与路由
                """;
    }

    @Override
    public String toolMenuBrief() {
        return """
                - mq_topic_stats: Topic 列表或路由
                - mq_consumer_status: 消费统计
                - mq_dead_letter: DLQ 提示
                """;
    }

    @Override
    public String toolSpecification(String toolName) {
        return switch (toolName) {
            case "mq_topic_stats" -> "mq_topic_stats: topic 可选";
            case "mq_consumer_status" -> "mq_consumer_status: consumerGroup 必填, topic 可选";
            case "mq_dead_letter" -> "mq_dead_letter: topic/group 可选";
            default -> OpsSkillRegistry.super.toolSpecification(toolName);
        };
    }

    @Override
    public Set<String> toolNames() {
        return Set.of("mq_topic_stats", "mq_consumer_status", "mq_dead_letter");
    }

    @Override
    public ToolResult execute(String toolName, Map<String, Object> args) {
        Map<String, Object> a = args == null ? Map.of() : args;
        return switch (toolName) {
            case "mq_topic_stats" -> toolkit.topicStats(str(a, "topic"));
            case "mq_consumer_status" -> toolkit.consumerStatus(str(a, "topic"), str(a, "consumerGroup"));
            case "mq_dead_letter" -> toolkit.deadLetterHint(str(a, "topic"), str(a, "consumerGroup"));
            default -> ToolResult.error("unknown tool: " + toolName);
        };
    }

    private static String str(Map<String, Object> a, String key) {
        Object v = a.get(key);
        return v == null ? "" : String.valueOf(v);
    }
}
