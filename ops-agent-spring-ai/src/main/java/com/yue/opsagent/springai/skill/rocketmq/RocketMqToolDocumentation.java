package com.yue.opsagent.springai.skill.rocketmq;

import java.util.Arrays;
import java.util.stream.Collectors;

enum RocketMqToolDocumentation {

    mq_topic_stats("""
            topic (string 可选)：指定则查该 topic 路由/统计；空则列出 topic 相关摘要。"""),

    mq_consumer_status("""
            consumerGroup (string 必填), topic (string 可选)。
            消费组进度与消费统计。"""),

    mq_dead_letter("""
            topic (string 可选), consumerGroup (string 可选)。
            返回 DLQ / 死信排查提示与路由信息。""");

    private final String usage;

    RocketMqToolDocumentation(String usage) {
        this.usage = usage.trim();
    }

    static String docFor(String toolId) {
        try {
            return valueOf(toolId).usage;
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    static String aggregatePromptFragment() {
        return Arrays.stream(values())
                .map(e -> "- " + e.name() + ":\n" + e.usage)
                .collect(Collectors.joining("\n\n"))
                + "\n";
    }
}
