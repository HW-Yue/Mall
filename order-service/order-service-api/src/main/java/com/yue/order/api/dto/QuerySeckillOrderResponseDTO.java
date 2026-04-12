package com.yue.order.api.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 秒杀建单结果查询响应
 */
@Data
@Builder
public class QuerySeckillOrderResponseDTO {

    /** 0=建单中，1=建单成功 */
    private Integer status;

    /** status=1 时返回订单号，供前端调用 get_pay_url */
    private String orderId;

}
