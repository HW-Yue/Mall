package com.yue.order.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 退款请求 DTO
 * /refund         - 普通订单，前端直接调用，包含业务规则验证
 * /refund_execute - 营销服务调用，跳过业务验证直接执行退款
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefundRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;
    private String orderId;
}
