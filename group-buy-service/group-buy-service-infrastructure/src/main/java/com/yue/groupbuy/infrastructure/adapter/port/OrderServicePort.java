package com.yue.groupbuy.infrastructure.adapter.port;

import com.yue.groupbuy.domain.trade.adapter.port.IOrderServicePort;
import com.yue.groupbuy.infrastructure.dao.ITOrderGroupDao;
import com.yue.groupbuy.infrastructure.dao.po.TOrderGroup;
import com.yue.groupbuy.types.enums.ResponseCode;
import com.yue.groupbuy.types.exception.AppException;
import com.yue.order.api.IOrderDubboService;
import com.yue.order.api.dto.CreateOrderRequestDTO;
import com.yue.order.api.dto.QueryOrderByOutTradeNoRequestDTO;
import com.yue.order.api.dto.RefundRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;

@Slf4j
@Service
public class OrderServicePort implements IOrderServicePort {

    @DubboReference
    private IOrderDubboService orderDubboService;
    @Resource
    private ITOrderGroupDao tOrderGroupDao;

    @Override
    public String createOrder(String userId, String productId, String goodsName, String goodsImageUrl,
                              BigDecimal originalPrice, BigDecimal deductionPrice, BigDecimal payPrice,
                              String source, String channel, String outTradeNo,
                              String teamId, Long activityId, Date startTime, Date endTime) {
        CreateOrderRequestDTO request = CreateOrderRequestDTO.builder()
                .userId(userId)
                .productId(productId)
                .goodsName(goodsName)
                .goodsImageUrl(goodsImageUrl)
                .marketType("group_buy")
                .originalPrice(originalPrice)
                .deductionPrice(deductionPrice)
                .payPrice(payPrice)
                .source(source)
                .channel(channel)
                .outTradeNo(outTradeNo)
                .build();

        String orderId;
        try {
            var response = orderDubboService.createOrder(request);
            if (response == null || response.getOrderId() == null) {
                log.error("order-service createOrder 返回空 userId:{} outTradeNo:{}", userId, outTradeNo);
                orderId = queryOrderIdByOutTradeNo(userId, outTradeNo);
                if (orderId == null) {
                    throw new AppException(ResponseCode.CREATE_ORDER_FAILED);
                }
            } else {
                orderId = response.getOrderId();
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.warn("order-service createOrder 调用异常，开始按 outTradeNo 确认 userId:{} outTradeNo:{}",
                    userId, outTradeNo, e);
            orderId = queryOrderIdByOutTradeNo(userId, outTradeNo);
            if (orderId == null) {
                throw new AppException(ResponseCode.CREATE_ORDER_FAILED);
            }
        }

        saveOrderGroupIdempotent(TOrderGroup.builder()
                .orderId(orderId)
                .userId(userId)
                .teamId(teamId)
                .activityId(activityId)
                .goodsId(productId)
                .outTradeNo(outTradeNo)
                .originalPrice(originalPrice)
                .deductionPrice(deductionPrice)
                .payPrice(payPrice)
                .startTime(startTime)
                .endTime(endTime)
                .build());
        return orderId;
    }

    private String queryOrderIdByOutTradeNo(String userId, String outTradeNo) {
        QueryOrderByOutTradeNoRequestDTO request = new QueryOrderByOutTradeNoRequestDTO();
        request.setUserId(userId);
        request.setOutTradeNo(outTradeNo);
        try {
            var response = orderDubboService.queryOrderByOutTradeNo(request);
            if (response == null) {
                log.warn("order-service queryOrderByOutTradeNo 未查到订单 userId:{} outTradeNo:{}", userId, outTradeNo);
                return null;
            }
            return response.getOrderId();
        } catch (Exception e) {
            log.warn("order-service queryOrderByOutTradeNo 调用异常 userId:{} outTradeNo:{}", userId, outTradeNo, e);
            return null;
        }
    }

    private void saveOrderGroupIdempotent(TOrderGroup orderGroup) {
        TOrderGroup existing = tOrderGroupDao.queryByUserIdAndOutTradeNo(orderGroup.getUserId(), orderGroup.getOutTradeNo());
        if (existing != null) {
            log.info("t_order_group 幂等跳过 userId:{} orderId:{} outTradeNo:{}",
                    orderGroup.getUserId(), existing.getOrderId(), orderGroup.getOutTradeNo());
            return;
        }
        try {
            tOrderGroupDao.insert(orderGroup);
        } catch (DuplicateKeyException e) {
            log.info("t_order_group 重复插入，幂等跳过 userId:{} orderId:{} outTradeNo:{}",
                    orderGroup.getUserId(), orderGroup.getOrderId(), orderGroup.getOutTradeNo());
        }
    }

    @Override
    public void refundExecute(String userId, String orderId) {
        RefundRequestDTO request = RefundRequestDTO.builder()
                .userId(userId)
                .orderId(orderId)
                .build();

        try {
            orderDubboService.refundExecute(request);
        } catch (Exception e) {
            log.error("order-service refundExecute 失败 userId:{} orderId:{}", userId, orderId, e);
            throw new AppException("退款执行失败");
        }
    }
}
