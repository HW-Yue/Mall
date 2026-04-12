package com.yue.groupbuy.infrastructure.dao;

import com.yue.groupbuy.infrastructure.dao.po.TOrderGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ITOrderGroupDao {

    void insert(TOrderGroup tOrderGroup);

    /** 通过 outTradeNo 查询订单（含价格、状态） */
    TOrderGroup queryByUserIdAndOutTradeNo(@Param("userId") String userId, @Param("outTradeNo") String outTradeNo);

    /** 通过 userId + orderId 查询订单 */
    TOrderGroup queryByUserIdAndOrderId(@Param("userId") String userId, @Param("orderId") String orderId);

    /** 通过 outTradeNo 查询 teamId + activityId */
    Map<String, Object> queryTeamInfoByOutTradeNo(@Param("outTradeNo") String outTradeNo);

    /** 通过 userId + orderId 查询 teamId */
    String queryTeamIdByOrder(@Param("userId") String userId, @Param("orderId") String orderId);

    /** 查询队伍已完成订单（status=1） */
    List<Map<String, Object>> queryCompletedOrdersForTeam(@Param("teamId") String teamId);

    /** 查询用户在某商品的参团次数（未退单） */
    int queryUserOrderCount(@Param("goodsId") String goodsId, @Param("userId") String userId);

    /** 查询队伍已完成订单的 outTradeNo 列表（status=1） */
    List<String> queryCompleteOutTradeNoListByTeamId(@Param("teamId") String teamId);

    /** 查询指定商品进行中（status=0）的用户参团记录 */
    List<Map<String, Object>> queryInProgressOrdersByActivityAndUser(
            @Param("goodsId") String goodsId,
            @Param("userId") String userId,
            @Param("count") Integer count);

    /** 查询指定商品进行中（status=0）的所有参团记录 */
    List<Map<String, Object>> queryInProgressOrdersByActivity(@Param("goodsId") String goodsId);

    /** 支付成功：status = 1，记录支付时间 */
    int updateStatus2Complete(@Param("outTradeNo") String outTradeNo, @Param("outTradeTime") java.util.Date outTradeTime);

    /** 退单：status = 2 */
    int update2Refund(@Param("userId") String userId, @Param("orderId") String orderId);
}
