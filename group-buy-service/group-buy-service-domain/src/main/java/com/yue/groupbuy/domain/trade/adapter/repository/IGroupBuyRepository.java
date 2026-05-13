package com.yue.groupbuy.domain.trade.adapter.repository;

import com.yue.groupbuy.domain.trade.model.entity.ActivityPricingEntity;
import com.yue.groupbuy.domain.trade.model.entity.LockOrderCommand;
import com.yue.groupbuy.domain.trade.model.entity.LockOrderResultEntity;
import com.yue.groupbuy.domain.trade.model.valobj.GroupBuyOrderStatusVO;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface IGroupBuyRepository {

    /**
     * 查询活动定价（含折扣试算）
     */
    ActivityPricingEntity queryActivityPricing(Long activityId, String goodsId);

    /**
     * 查询用户在该商品下的已参团次数
     */
    int queryUserOrderCount(String goodsId, String userId);

    /**
     * 查询队伍进度（锁单数 / 目标数）
     */
    int queryTeamLockCount(String teamId);

    /**
     * 锁定拼团订单：新建或更新 group_buy_order（事务）
     */
    LockOrderResultEntity lockGroupBuyOrder(LockOrderCommand command);

    /**
     * 保存 t_order_group（事务）
     */
    void saveOrderGroup(String orderId, String teamId, Long activityId, Date startTime, Date endTime);

    /**
     * 补偿：锁单失败时减回 lockCount
     */
    void compensateLockCount(String teamId, boolean newTeam);

    /**
     * 通过 orderId 查询 teamId + activityId（join t_order + t_order_group）
     */
    Map<String, Object> queryTeamInfoByOrderId(String orderId);

    /**
     * 结算：completeCount + 1，返回是否已达成团目标
     */
    boolean settleGroupBuyOrder(String teamId);

    /**
     * 查询队伍已完成订单（用于发 group-buy-success-notify）
     */
    List<Map<String, Object>> queryCompletedOrdersForTeam(String teamId);

    /**
     * 通过 userId + orderId 查询 teamId
     */
    String queryTeamIdByOrder(String userId, String orderId);

    /**
     * 查询队伍状态
     */
    GroupBuyOrderStatusVO queryTeamStatus(String teamId);

    /**
     * 退款：lockCount - 1，可能更新 completeCount - 1
     */
    void refundGroupBuyOrder(String teamId, boolean paid);
}
