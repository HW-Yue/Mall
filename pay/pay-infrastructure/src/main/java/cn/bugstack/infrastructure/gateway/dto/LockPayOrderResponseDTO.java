package cn.bugstack.infrastructure.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 普通商品（无营销）锁单应答对象
 * 对应电商服务：POST /api/v1/nor/trade/lock_pay_order
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LockPayOrderResponseDTO {

    /** 订单ID */
    private String orderId;
    /** 原价 */
    private BigDecimal originalPrice;
    /** 抵扣金额 */
    private BigDecimal deductionPrice;
    /** 应付金额 */
    private BigDecimal payPrice;
    /** 交易订单状态 */
    private Integer tradeOrderStatus;
}
