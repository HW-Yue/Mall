package com.yue.opsagent.springai.skill.rocketmq;

import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import com.yue.opsagent.springai.skill.api.ToolResult;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * RocketMQ 只读管理接口（需可达 NameServer）。
 */
@Component
public class RocketMqToolkit {

    private final OpsAiProperties props;

    public RocketMqToolkit(OpsAiProperties props) {
        this.props = props;
    }

    private DefaultMQAdminExt createAdmin() {
        var r = props.getRocketmq();
        DefaultMQAdminExt admin = new DefaultMQAdminExt();
        admin.setNamesrvAddr(r.getNameServer());
        admin.setInstanceName("ops-ai-admin-" + System.nanoTime());
        return admin;
    }

    public ToolResult topicStats(String topic) {
        if (!props.getRocketmq().isEnabled()) {
            return ToolResult.error("RocketMQ 未启用（ops-ai.rocketmq.enabled=false）");
        }
        DefaultMQAdminExt admin = createAdmin();
        try {
            admin.start();
            if (topic == null || topic.isBlank()) {
                Set<String> topics = new TreeSet<>(admin.fetchAllTopicList().getTopicList());
                return ToolResult.ok("topics", topics.stream().limit(80).toList());
            }
            var route = admin.examineTopicRouteInfo(topic.trim());
            return ToolResult.ok("topicRoute", route == null ? "" : route.toString());
        } catch (Exception e) {
            return ToolResult.error("mq_topic_stats 失败: " + e.getMessage());
        } finally {
            admin.shutdown();
        }
    }

    public ToolResult consumerStatus(String topic, String consumerGroup) {
        if (!props.getRocketmq().isEnabled()) {
            return ToolResult.error("RocketMQ 未启用");
        }
        if (consumerGroup == null || consumerGroup.isBlank()) {
            return ToolResult.error("consumerGroup 不能为空");
        }
        DefaultMQAdminExt admin = createAdmin();
        try {
            admin.start();
            var stats = topic != null && !topic.isBlank()
                    ? admin.examineConsumeStats(consumerGroup.trim(), topic.trim())
                    : admin.examineConsumeStats(consumerGroup.trim());
            Map<String, Object> out = new HashMap<>();
            out.put("consumeStats", stats == null ? "" : stats.toString());
            return ToolResult.ok("consumer", out);
        } catch (Exception e) {
            return ToolResult.error("mq_consumer_status 失败: " + e.getMessage());
        } finally {
            admin.shutdown();
        }
    }

    public ToolResult deadLetterHint(String topic, String consumerGroup) {
        if (!props.getRocketmq().isEnabled()) {
            return ToolResult.error("RocketMQ 未启用");
        }
        return ToolResult.ok("dead_letter", Map.of(
                "hint", "请结合 topic / consumerGroup 在控制台或 Broker 查看 %DLQ% 队列；此处仅返回路由信息。",
                "topicRoute",
                topic == null || topic.isBlank() ? "" : safeTopicRoute(topic)));
    }

    private String safeTopicRoute(String topic) {
        DefaultMQAdminExt admin = createAdmin();
        try {
            admin.start();
            var route = admin.examineTopicRouteInfo(topic.trim());
            return route == null ? "" : route.toString();
        } catch (Exception e) {
            return e.getMessage();
        } finally {
            admin.shutdown();
        }
    }
}
