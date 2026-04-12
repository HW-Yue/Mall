package com.yue.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 商品详情响应
 */
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class GoodsDetailResponseDTO extends GoodsItemDTO {

    /** 商品详情介绍 */
    private String goodsDetail;

}

