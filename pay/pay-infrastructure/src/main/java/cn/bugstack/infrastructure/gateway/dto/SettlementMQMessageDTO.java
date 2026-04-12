package cn.bugstack.infrastructure.gateway.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 支付成功结算异步消息体（发往 RocketMQ，供下游消费）
 */
@Data
public class SettlementMQMessageDTO {

    /** 渠道 */
    private String source;
    /** 来源 */
    private String channel;
    /** 用户ID */
    private String userId;
    /** 外部交易单号 */
    private String outTradeNo;
    /** 外部交易时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date outTradeTime;
    /** 营销类型：normal / group_buy / seckill */
    private String marketType;
}
