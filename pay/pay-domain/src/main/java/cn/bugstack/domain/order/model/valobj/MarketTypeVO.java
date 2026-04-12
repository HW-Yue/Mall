package cn.bugstack.domain.order.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 营销类型（与 types.enums.MarketType 对齐）
 * @create 2025-02-06 15:04
 */
@Getter
@AllArgsConstructor
public enum MarketTypeVO {

    Normal("normal", "普通商品"),
    GroupBuyMarket("group_buy", "拼团业务"),
    Voucher("seckill", "秒杀业务"),
    ;

    private final String code;
    private final String desc;

    /** 根据 code 字符串解析：normal/group_buy/seckill */
    public static MarketTypeVO fromCode(String code) {
        if (code == null) {
            throw new RuntimeException("marketType code is null!");
        }
        switch (code) {
            case "normal":
                return Normal;
            case "group_buy":
                return GroupBuyMarket;
            case "seckill":
                return Voucher;
            default:
                throw new RuntimeException("err code not exist! code=" + code);
        }
    }

    /** 兼容旧接口：Integer 0=normal, 1=group_buy, 2=seckill */
    public static MarketTypeVO valueOf(Integer code) {
        if (code == null) {
            throw new RuntimeException("marketType code is null!");
        }
        switch (code) {
            case 0:
                return Normal;
            case 1:
                return GroupBuyMarket;
            case 2:
                return Voucher;
            default:
                throw new RuntimeException("err code not exist! code=" + code);
        }
    }
}
