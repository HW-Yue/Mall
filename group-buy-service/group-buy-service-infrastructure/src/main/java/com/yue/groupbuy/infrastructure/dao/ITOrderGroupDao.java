package com.yue.groupbuy.infrastructure.dao;

import com.yue.groupbuy.infrastructure.dao.po.TOrderGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ITOrderGroupDao {

    void insert(TOrderGroup tOrderGroup);

    /** 通过 userId + orderId 查询订单 */
    TOrderGroup queryByUserIdAndOrderId(@Param("userId") String userId, @Param("orderId") String orderId);

    /** 通过 orderId 查询 teamId + activityId */
    Map<String, Object> queryTeamInfoByOrderId(@Param("orderId") String orderId);

    /** 通过 userId + orderId 查询 teamId */
    String queryTeamIdByOrder(@Param("userId") String userId, @Param("orderId") String orderId);

    /** 查询队伍已完成订单（status=1） */
    List<Map<String, Object>> queryCompletedOrdersForTeam(@Param("teamId") String teamId);

    /** 查询用户在某商品的参团次数（未退款完成） */
    int queryUserOrderCount(@Param("goodsId") String goodsId, @Param("userId") String userId);

    /** 查询队伍已完成订单的 orderId 列表（status=1） */
    List<String> queryCompleteOrderIdListByTeamId(@Param("teamId") String teamId);

    /** 查询指定商品进行中（status=0）的用户参团记录 */
    List<Map<String, Object>> queryInProgressOrdersByActivityAndUser(
            @Param("goodsId") String goodsId,
            @Param("userId") String userId,
            @Param("count") Integer count);

    /** 查询指定商品进行中（status=0）的所有参团记录 */
    List<Map<String, Object>> queryInProgressOrdersByActivity(@Param("goodsId") String goodsId);

    /** 支付成功：status = 1，记录支付时间 */
    int updateStatus2Complete(@Param("orderId") String orderId, @Param("outTradeTime") java.util.Date outTradeTime);

    /** 标记退款处理中：status = 2 */
    int update2Refund(@Param("userId") String userId, @Param("orderId") String orderId);

    /** 标记退款完成：status = 4 */
    int update2Refunded(@Param("orderId") String orderId);

    /** 查询团队中已支付的个人订单（status = 1） */
    List<TOrderGroup> queryPaidOrdersByTeamId(@Param("teamId") String teamId);

    /** 查询团队中未支付的个人订单（status = 0） */
    List<TOrderGroup> queryUnpaidOrdersByTeamId(@Param("teamId") String teamId);

    /** 批量关闭团队中未支付的个人订单（status = 0 -> 3） */
    int closeUnpaidOrdersByTeamId(@Param("teamId") String teamId);

    /** 按 orderId 关闭单笔未支付订单（status = 0 -> 3），返回受影响行数 */
    int closeUnpaidByOrderId(@Param("orderId") String orderId);
}
