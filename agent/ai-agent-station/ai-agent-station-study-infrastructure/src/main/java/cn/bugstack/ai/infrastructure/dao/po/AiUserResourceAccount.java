package cn.bugstack.ai.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户资源账户表 PO 对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiUserResourceAccount {

    /** 主键 */
    private Long id;

    /** 用户唯一标识 */
    private String userId;

    /** 资源大类 */
    private String goodsType;

    /** 资源Key */
    private String resKey;

    /** 总剩余可用额度 */
    private BigDecimal totalBalance;

    /** 已消耗额度 */
    private BigDecimal usedAmount;

    /** 冻结中额度 */
    private BigDecimal frozenAmount;

    /** 单位 */
    private String unit;

    /** 最后一次变更的orderId/流水ID */
    private String lastTxId;

    /** 更新时间 */
    private LocalDateTime updateTime;
}

