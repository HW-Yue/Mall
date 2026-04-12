package cn.bugstack.api.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePayRequestDTO {

    // 用户ID 【实际产生中会通过登录模块获取，不需要透彻】
    private String userId;
    //单号
    private String outTradeNo;
    // 产品编号
    private String productId;
    //产品名字
    private String productName;
    // 拼团队伍 - 队伍ID
    private String teamId;
    /** 原始价格 */
    private BigDecimal originalPrice;
    /** 折扣金额 */
    private BigDecimal deductionPrice;
    /** 支付金额 */
    private BigDecimal payPrice;
    // 营销类型：normal-普通商品、group_buy-拼团业务、seckill-秒杀业务
    private String marketType;

}
