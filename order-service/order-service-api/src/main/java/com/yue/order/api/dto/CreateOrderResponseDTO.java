package com.yue.order.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建订单响应 DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderResponseDTO {

    /** 订单ID（返回给前端，用于后续支付/查询） */
    private String orderId;
    /** 外部交易单号 */
    private String outTradeNo;
}
