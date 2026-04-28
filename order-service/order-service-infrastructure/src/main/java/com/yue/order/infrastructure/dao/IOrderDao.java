package com.yue.order.infrastructure.dao;

import com.yue.order.infrastructure.dao.po.OrderPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 订单主表 DAO（t_order）
 */
@Mapper
public interface IOrderDao {

    void insert(OrderPO po);

    OrderPO queryByUserIdAndOrderId(@Param("userId") String userId, @Param("orderId") String orderId);

    OrderPO queryByOutTradeNo(@Param("outTradeNo") String outTradeNo);

    List<String> queryTimeoutCloseOrderList();

    int updatePayUrl(@Param("userId") String userId,
                     @Param("outTradeNo") String outTradeNo,
                     @Param("payUrl") String payUrl);

    int updatePaySuccess(@Param("outTradeNo") String outTradeNo,
                         @Param("outTradeTime") Date outTradeTime);

    int updateWaitShipByOrderId(@Param("userId") String userId,
                                @Param("orderId") String orderId);

    int updateShippedByOutTradeNo(@Param("outTradeNo") String outTradeNo);

    int updateDeliveredByOutTradeNo(@Param("outTradeNo") String outTradeNo);

    int updateWaitRefundByOutTradeNo(@Param("outTradeNo") String outTradeNo);

    int updateCloseByOutTradeNo(@Param("outTradeNo") String outTradeNo);

    int updateRefundedByOutTradeNo(@Param("outTradeNo") String outTradeNo);

    List<OrderPO> queryWaitShipOrderList(@Param("count") int count);

    List<OrderPO> queryUserOrderList(@Param("userId") String userId,
                                     @Param("lastId") Long lastId,
                                     @Param("count") int count);
}
