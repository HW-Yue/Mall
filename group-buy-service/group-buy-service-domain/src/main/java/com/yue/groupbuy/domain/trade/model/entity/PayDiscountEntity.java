package com.yue.groupbuy.domain.trade.model.entity;

import com.yue.groupbuy.domain.trade.model.valobj.NotifyConfigVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PayDiscountEntity {

    /** 来源 */
    private String source;
    /** 渠道 */
    private String channel;
    /** 商品ID */
    private String goodsId;
    /** 商品名称 */
    private String goodsName;
    /** 原始价格 */
    private BigDecimal originalPrice;
    /** 折扣金额 */
    private BigDecimal deductionPrice;
    /** 支付金额 */
    private BigDecimal payPrice;
    /** 回调配置 */
    private NotifyConfigVO notifyConfigVO;

}
