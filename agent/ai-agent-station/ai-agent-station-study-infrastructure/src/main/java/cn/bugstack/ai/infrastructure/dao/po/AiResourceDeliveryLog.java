package cn.bugstack.ai.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI资源下发/充值流水表 PO 对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiResourceDeliveryLog {

    /** 自增主键 */
    private Long id;

    /** 商城侧唯一订单ID (用于幂等校验) */
    private String orderId;

    /** 用户唯一标识 */
    private String userId;

    /** 业务类型 */
    private String bizType;

    /** 业务关联ID */
    private String bizId;

    /** 资源大类 */
    private String goodsType;

    /** 资源Key */
    private String resKey;

    /** 下发的资源数量/额度 */
    private java.math.BigDecimal resValue;

    /** 单位 */
    private String unit;

    /** 状态: 1-处理中, 2-成功, 3-失败 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}

