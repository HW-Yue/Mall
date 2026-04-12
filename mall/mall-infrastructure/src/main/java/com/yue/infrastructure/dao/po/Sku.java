package com.yue.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 商品信息
 * @create 2024-12-21 10:45
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Sku {

    /** 自增 */
    private Long id;
    /** 商品ID */
    private String goodsId;
    /** 商品名称 */
    private String goodsName;
    /** 商品图片URL */
    private String goodsImageUrl;
    /** 商品详情介绍 */
    private String goodsDetail;
    /** 原始价格 */
    private BigDecimal originalPrice;
    /** 所属类目ID */
    private Integer categoryId;
    /** 总库存 */
    private Integer totalStock;
    /** 锁定量 */
    private Integer lockedStock;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;

}
