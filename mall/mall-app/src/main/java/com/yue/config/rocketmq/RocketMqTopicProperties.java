package com.yue.config.rocketmq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 业务侧 Topic / 消费者组命名，与 YAML {@code app.rocketmq} 对齐，供生产者与消费者注解引用。
 */
@Data
@ConfigurationProperties(prefix = "app.rocketmq")
public class RocketMqTopicProperties {

    private Topic topic = new Topic();
    private ConsumerGroup consumerGroup = new ConsumerGroup();

    @Data
    public static class Topic {
        /** 与拼团成功等业务对应的 Topic */
        private String teamSuccess = "GROUP_BUY_TOPIC_TEAM_SUCCESS";
        private String teamRefund = "GROUP_BUY_TOPIC_TEAM_REFUND";
        /** 支付成功通知，驱动营销组队结算（与 @RocketMQMessageListener 一致） */
        private String paySuccess = "pay-success";
    }

    @Data
    public static class ConsumerGroup {
        private String teamRefund = "CG_GROUP_BUY_TEAM_REFUND";
        private String paySuccess = "CG_GBM_PAY_SUCCESS";
    }
}
