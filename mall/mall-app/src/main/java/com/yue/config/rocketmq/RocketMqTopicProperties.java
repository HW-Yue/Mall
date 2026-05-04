package com.yue.config.rocketmq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 业务侧 Topic / 消费者组命名，与 YAML {@code app.rocketmq} 对齐，供消费端注解和外部集成引用。
 * mall 当前不直接发 MQ，这里只保留与集团链路对齐的 topic 常量。
 */
@Data
@ConfigurationProperties(prefix = "app.rocketmq")
public class RocketMqTopicProperties {

    private Topic topic = new Topic();
    private ConsumerGroup consumerGroup = new ConsumerGroup();

    @Data
    public static class Topic {
        /** 拼团成功通知 Topic */
        private String teamSuccess = "GROUP_BUY_TOPIC_TEAM_SUCCESS";
        /** 拼团退款通知 Topic */
        private String teamRefund = "GROUP_BUY_TOPIC_TEAM_REFUND";
        /** 支付成功通知 Topic，与 @RocketMQMessageListener 一致 */
        private String paySuccess = "pay-success";
    }

    @Data
    public static class ConsumerGroup {
        private String teamRefund = "CG_GROUP_BUY_TEAM_REFUND";
        private String paySuccess = "CG_GBM_PAY_SUCCESS";
    }
}
