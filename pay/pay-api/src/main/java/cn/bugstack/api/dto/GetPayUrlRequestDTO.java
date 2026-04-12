package cn.bugstack.api.dto;

import lombok.Data;

/**
 * 获取支付链接请求 DTO
 */
@Data
public class GetPayUrlRequestDTO {

    /** 用户ID */
    private String userId;
    /** 订单号 */
    private String orderId;
}
