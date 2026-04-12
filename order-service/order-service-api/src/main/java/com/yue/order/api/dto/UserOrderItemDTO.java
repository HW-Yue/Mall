package com.yue.order.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOrderItemDTO {

    private String orderId;
    /** PAY_WAIT / PAY_SUCCESS / CLOSE */
    private String status;
    /** 商品标识（goodsId）*/
    private String productName;
    private Date orderTime;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    /** normal / group_buy / seckill */
    private String marketType;
}
