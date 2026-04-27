package com.yue.seckill.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderResultEntity {

    private String seckillToken;
    private String orderId;
    private String outTradeNo;
}
