/**
 * @description:
 * @author: 29874
 * @date: 2026/3/10 22:22
 */

package cn.bugstack.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum MarketType {
    Normal("normal", "普通商品"),
    GroupBuyMarket("group_buy", "拼团业务"),
    Voucher("seckill", "秒杀业务"),
    ;

    private String code;
    private String info;
}
