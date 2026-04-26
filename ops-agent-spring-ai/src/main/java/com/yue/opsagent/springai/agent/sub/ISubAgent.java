package com.yue.opsagent.springai.agent.sub;

import java.util.Map;

public interface ISubAgent {

    /** Same id as {@link com.yue.opsagent.springai.skill.api.OpsSkillRegistry#name()}. */
    String domainId();

    /** Shown in parent system prompt / tool description. */
    String parentToolDescription();

    /** Spring AI tool name exposed to the parent agent. */
    default String parentToolName() {
        return switch (domainId()) {
            case "docker_ops" -> "docker_skill";
            case "mysql_inspect" -> "mysql_skill";
            case "rocketmq_inspect" -> "rocketmq_skill";
            case "metrics_ops" -> "prometheus_skill";
            case "elasticsearch_ops" -> "elasticsearch_skill";
            case "redis_inspect" -> "redis_skill";
            case "nacos_config" -> "nacos_skill";
            default -> domainId().replace('-', '_');
        };
    }

    default String parentDisplayName() {
        return switch (domainId()) {
            case "docker_ops" -> "Docker Skill";
            case "mysql_inspect" -> "MySQL Skill";
            case "rocketmq_inspect" -> "RocketMQ Skill";
            case "metrics_ops" -> "Prometheus Skill";
            case "elasticsearch_ops" -> "Elasticsearch Skill";
            case "redis_inspect" -> "Redis Skill";
            case "nacos_config" -> "Nacos Skill";
            default -> domainId();
        };
    }

    /**
     * Inner ReAct loop for this domain; returns summary for parent / SOP orchestrator.
     */
    String runReact(String task, Map<String, Object> context);
}
