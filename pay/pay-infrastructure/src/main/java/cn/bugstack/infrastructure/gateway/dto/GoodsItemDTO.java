package cn.bugstack.infrastructure.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品基础信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品ID */
    private String id;
    /** 商品图片URL */
    private String imageUrl;
    /** 商品名称 */
    private String name;
    /** 价格 */
    private BigDecimal price;
    /** 渠道 source */
    private String source;
    /** 来源 channel */
    private String channel;

}

