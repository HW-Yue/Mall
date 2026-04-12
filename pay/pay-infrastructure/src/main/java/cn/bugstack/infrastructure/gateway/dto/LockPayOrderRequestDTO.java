package cn.bugstack.infrastructure.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 普通商品（无营销）锁单请求对象
 * 对应电商服务：POST /api/v1/nor/trade/lock_pay_order
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LockPayOrderRequestDTO {

    /** 用户ID */
    private String userId;
    /** 商品ID */
    private String goodsId;
    /** 活动ID（可选） */
    private Long activityId;
    /** 渠道 */
    private String source;
    /** 来源 */
    private String channel;
    /** 外部订单号 */
    private String outTradeNo;
    /** 回调配置 */
    private NotifyConfigVO notifyConfigVO;

    /**
     * 便捷配置：HTTP 回调
     */
    public void setNotifyUrl(String url) {
        NotifyConfigVO notifyConfigVO = new NotifyConfigVO();
        notifyConfigVO.setNotifyType("HTTP");
        notifyConfigVO.setNotifyUrl(url);
        this.notifyConfigVO = notifyConfigVO;
    }

    /**
     * 便捷配置：MQ 回调，传入 MQ 配置描述（如 topic_normal_refund）
     */
    public void setNotifyMQ(String notifyMQ) {
        NotifyConfigVO notifyConfigVO = new NotifyConfigVO();
        notifyConfigVO.setNotifyType("MQ");
        notifyConfigVO.setNotifyMQ(notifyMQ);
        this.notifyConfigVO = notifyConfigVO;
    }

    /**
     * 回调配置对象
     */
    @Data
    public static class NotifyConfigVO {
        /** 回调方式：HTTP 或 MQ */
        private String notifyType;
        /** MQ 相关配置描述 */
        private String notifyMQ;
        /** HTTP 回调地址 */
        private String notifyUrl;
    }
}
