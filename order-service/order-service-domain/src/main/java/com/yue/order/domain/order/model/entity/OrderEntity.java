package com.yue.order.domain.order.model.entity;

import com.yue.order.domain.order.model.valobj.MarketTypeVO;
import com.yue.order.domain.order.model.valobj.OrderStatusVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderEntity {

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
    private OrderStatusVO status;
    private String outTradeNo;
    private Date outTradeTime;
    private String payUrl;
    private MarketTypeVO marketType;
    private String bizId;
    private String notifyType;
    private String notifyUrl;
    private Date createTime;
}
