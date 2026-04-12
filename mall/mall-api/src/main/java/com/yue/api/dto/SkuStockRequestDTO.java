package com.yue.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * SKU 库存锁定/释放请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkuStockRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品ID */
    private String goodsId;

    /** 数量（通常为1） */
    private Integer count;
}
