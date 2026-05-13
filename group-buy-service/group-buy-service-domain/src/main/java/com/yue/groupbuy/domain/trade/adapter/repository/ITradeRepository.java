package com.yue.groupbuy.domain.trade.adapter.repository;

import com.yue.groupbuy.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import com.yue.groupbuy.domain.trade.model.aggregate.GroupBuyRefundAggregate;
import com.yue.groupbuy.domain.trade.model.aggregate.GroupBuyTeamSettlementAggregate;
import com.yue.groupbuy.domain.trade.model.entity.*;
import com.yue.groupbuy.domain.trade.model.valobj.GroupBuyProgressVO;

import java.util.List;

public interface ITradeRepository {

    MarketPayOrderEntity queryMarketPayOrderEntityByOrderId(String userId, String orderId);

    MarketPayOrderEntity lockMarketPayOrder(GroupBuyOrderAggregate groupBuyOrderAggregate);

    GroupBuyProgressVO queryGroupBuyProgress(String teamId);

    GroupBuyActivityEntity queryGroupBuyActivityEntityByActivityId(Long activityId);

    Integer queryOrderCountByGoodsId(String goodsId, String userId);

    GroupBuyTeamEntity queryGroupBuyTeamByTeamId(String teamId);

    NotifyTaskEntity settlementMarketPayOrder(GroupBuyTeamSettlementAggregate groupBuyTeamSettlementAggregate);

    boolean isSCBlackIntercept(String source, String channel);

    List<NotifyTaskEntity> queryUnExecutedNotifyTaskList();

    List<NotifyTaskEntity> queryUnExecutedNotifyTaskList(String teamId);

    int updateNotifyTaskStatusSuccess(NotifyTaskEntity notifyTaskEntity);

    int updateNotifyTaskStatusError(NotifyTaskEntity notifyTaskEntity);

    int updateNotifyTaskStatusRetry(NotifyTaskEntity notifyTaskEntity);

    boolean occupyTeamStock(String teamStockKey, String recoveryTeamStockKey, Integer target, Integer validTime);

    void recoveryTeamStock(String recoveryTeamStockKey, Integer validTime);

    NotifyTaskEntity unpaid2Refund(GroupBuyRefundAggregate groupBuyRefundAggregate);

    NotifyTaskEntity paid2Refund(GroupBuyRefundAggregate groupBuyRefundAggregate);

    NotifyTaskEntity paidTeam2Refund(GroupBuyRefundAggregate groupBuyRefundAggregate);

    void refund2AddRecovery(String recoveryTeamStockKey, String orderId);

    /**
     * 乐观锁更新团队状态为失败（status = 2），仅当当前状态为拼单中（0）时生效
     * @return 影响行数，1 表示更新成功，0 表示已被其他流程修改
     */
    int updateTeamStatus2Fail(String teamId);

    /**
     * 查询团队中所有已支付的个人订单
     */
    List<TeamOrderEntity> queryPaidOrdersByTeamId(String teamId);

    /**
     * 查询团队中所有未支付的个人订单
     */
    List<TeamOrderEntity> queryUnpaidOrdersByTeamId(String teamId);

    /**
     * 批量关闭团队中未支付的个人订单（status = 0 -> 3）
     * @return 影响行数
     */
    int closeUnpaidOrdersByTeamId(String teamId);

    /**
     * 将个人订单更新为退款处理中（status = 2）
     * @return 影响行数
     */
    int updateOrder2Refund(String userId, String orderId);

    /**
     * 将个人订单更新为已退款（status = 4）
     * @return 影响行数
     */
    int updateOrder2Refunded(String orderId);

    /**
     * 关闭单笔未支付订单并回退团占用库存（CAS：t_order.status 0→3，命中后再 lock_count-1）。
     * @return true 本次确实把订单从未支付翻到了已关团并扣减了 lock_count；
     *         false 订单已被处理过（非 status=0），无副作用。
     */
    boolean closeUnpaidOrderAndReleaseStock(String orderId);

}
