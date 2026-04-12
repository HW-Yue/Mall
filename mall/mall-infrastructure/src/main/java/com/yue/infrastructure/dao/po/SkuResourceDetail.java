package com.yue.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品资源详情表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SkuResourceDetail {

    /** 自增ID */
    private Integer id;
    /** 关联sku表的goods_id */
    private String goodsId;
    /** 资源类型: model_name, storage_size */
    private String resKey;
    /** 资源值: gpt-4, 1000000, 1024, 30 */
    private String resValue;
    /** 单位: token, MB, GB */
    private String unit;
}
