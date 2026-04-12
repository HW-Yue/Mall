package com.yue.order.infrastructure.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SKU 库存锁定/释放请求（发送给 mall 服务）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkuStockRequestDTO {

    private String goodsId;
    private Integer count;
}
