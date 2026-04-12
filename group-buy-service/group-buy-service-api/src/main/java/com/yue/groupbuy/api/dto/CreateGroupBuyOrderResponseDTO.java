package com.yue.groupbuy.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateGroupBuyOrderResponseDTO {

    /** 订单ID */
    private String orderId;
    /** 队伍ID */
    private String teamId;
    /** 外部单号 */
    private String outTradeNo;
    /** 原始价格 */
    private BigDecimal originalPrice;
    /** 折扣金额 */
    private BigDecimal deductionPrice;
    /** 实付金额 */
    private BigDecimal payPrice;
}
