package com.yue.order.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * t_order 表映射（与 mall 共享同一张表）
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderPO {

    private Long id;
    private String orderId;
    private String userId;
    private String goodsId;
    private String goodsName;
    private String goodsImageUrl;
    private String source;
    private String channel;
    private BigDecimal originalPrice;
    private BigDecimal deductionPrice;
    private BigDecimal payPrice;
    /** 0=锁单, 1=支付成功, 2=关闭, 3=待退款, 4=已退款, 5=待发货, 6=已发货, 7=已签收 */
    private Integer status;
    private String outTradeNo;
    private Date outTradeTime;
    private String bizId;
    private String notifyType;
    private String notifyUrl;
    private String payUrl;
    /** normal / group_buy / seckill */
    private String marketType;
    private Date createTime;
    private Date updateTime;
}
