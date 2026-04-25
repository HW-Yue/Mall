package com.yue.order.domain.order.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * mall 已锁库场景：订单异步落单入队成功后的结果
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NormalOrderEnqueueResult {

    private String orderId;
    private String outTradeNo;
}
