package com.yue.order.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取支付 URL 请求 DTO（用户在支付页点击支付时调用）
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetPayUrlRequestDTO {

    private String userId;
    /** 订单ID（createOrder 返回的 orderId） */
    private String orderId;
}
