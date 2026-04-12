package com.yue.order.domain.order.service;

import com.yue.order.domain.order.adapter.port.IPayPort;
import com.yue.order.domain.order.adapter.repository.IOrderRepository;
import com.yue.order.domain.order.model.entity.CreateOrderCommand;
import com.yue.order.domain.order.model.entity.OrderEntity;
import com.yue.order.domain.order.model.valobj.MarketTypeVO;
import com.yue.order.domain.order.model.valobj.OrderStatusVO;
import com.yue.order.types.enums.ResponseCode;
import com.yue.order.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class OrderDomainService implements IOrderDomainService {

    @Resource
    private IOrderRepository orderRepository;

    @Resource
    private IPayPort payPort;

    /** 下游事件发布（由 infrastructure 层实现，通过 Spring 注入） */
    @Resource
    private IOrderEventPublisher eventPublisher;

    @Override
    public String createOrder(CreateOrderCommand command) {
        // 生成外部交易单号（若未提供）
        String outTradeNo = StringUtils.isNotBlank(command.getOutTradeNo())
                ? command.getOutTradeNo().trim()
                : RandomStringUtils.randomNumeric(12);
        command.setOutTradeNo(outTradeNo);

        // 构建订单实体
        OrderEntity order = OrderEntity.builder()
                .userId(command.getUserId())
                .goodsId(command.getGoodsId())
                .goodsName(command.getGoodsName())
                .goodsImageUrl(command.getGoodsImageUrl())
                .source(command.getSource())
                .channel(command.getChannel())
                .originalPrice(command.getOriginalPrice())
                .deductionPrice(command.getDeductionPrice() != null ? command.getDeductionPrice() : BigDecimal.ZERO)
                .payPrice(command.getPayPrice())
                .status(OrderStatusVO.LOCK)
                .outTradeNo(outTradeNo)
                .marketType(MarketTypeVO.fromCode(command.getMarketType()))
                .notifyType("MQ")
                .build();

        // 保存订单（对 normal 类型同时锁 SKU 库存）
        String orderId = orderRepository.saveOrder(order);
        log.info("createOrder 完成 userId:{} orderId:{} marketType:{}", command.getUserId(), orderId, command.getMarketType());
        return orderId;
    }

    @Override
    public String getPayUrl(String userId, String orderId) {
        OrderEntity order = orderRepository.queryByUserIdAndOrderId(userId, orderId);
        if (order == null) {
            throw new AppException(ResponseCode.ORDER_NOT_FOUND.getCode(), "订单不存在: " + orderId);
        }
        if (order.getStatus() != OrderStatusVO.LOCK) {
            throw new AppException(ResponseCode.ORDER_STATUS_ERROR.getCode(), "订单状态不可支付: " + order.getStatus().getCode());
        }

        // 已有 payUrl 直接返回
        if (StringUtils.isNotBlank(order.getPayUrl())) {
            return order.getPayUrl();
        }

        // 调支付服务获取 Alipay URL
        String payUrl = payPort.getPayUrl(
                order.getUserId(),
                order.getOutTradeNo(),
                order.getGoodsId(),
                order.getGoodsId(),
                order.getOriginalPrice(),
                order.getDeductionPrice(),
                order.getPayPrice(),
                order.getMarketType().getCode());

        if (StringUtils.isBlank(payUrl)) {
            throw new AppException(ResponseCode.PAY_URL_FAILED.getCode(), "获取支付链接失败");
        }

        // 回写 payUrl
        orderRepository.updatePayUrl(userId, order.getOutTradeNo(), payUrl);
        log.info("getPayUrl 完成 userId:{} orderId:{}", userId, orderId);
        return payUrl;
    }

    @Override
    public void handlePaySuccess(String outTradeNo, String marketType, Date outTradeTime) {
        OrderEntity order = orderRepository.queryByOutTradeNo(outTradeNo);
        if (order == null) {
            log.warn("handlePaySuccess 订单不存在 outTradeNo:{}", outTradeNo);
            return;
        }
        if (order.getStatus() != OrderStatusVO.LOCK) {
            log.info("handlePaySuccess 订单状态已更新，跳过 outTradeNo:{} status:{}", outTradeNo, order.getStatus());
            return;
        }

        // 更新订单状态为支付成功
        orderRepository.updatePaySuccess(outTradeNo, outTradeTime != null ? outTradeTime : new Date());
        log.info("handlePaySuccess 更新订单成功 outTradeNo:{} marketType:{}", outTradeNo, marketType);

        // 发布下游事件（order-paid-normal / order-paid-group_buy / order-paid-seckill）
        eventPublisher.publishOrderPaid(order.getUserId(), outTradeNo, marketType, outTradeTime);
    }

    @Override
    public void refund(String userId, String orderId) {
        OrderEntity order = orderRepository.queryByUserIdAndOrderId(userId, orderId);
        if (order == null) {
            throw new AppException(ResponseCode.ORDER_NOT_FOUND.getCode(), "订单不存在: " + orderId);
        }
        // 只有 LOCK 或 PAY_SUCCESS 状态可以退款
        if (order.getStatus() == OrderStatusVO.CLOSE) {
            throw new AppException(ResponseCode.ORDER_STATUS_ERROR.getCode(), "订单已关闭，无法退款");
        }
        doRefund(order);
    }

    @Override
    public void refundExecute(String userId, String orderId) {
        OrderEntity order = orderRepository.queryByUserIdAndOrderId(userId, orderId);
        if (order == null) {
            throw new AppException(ResponseCode.ORDER_NOT_FOUND.getCode(), "订单不存在: " + orderId);
        }
        doRefund(order);
    }

    @Override
    public List<OrderEntity> queryUserOrderList(String userId, Long lastId, int count) {
        return orderRepository.queryUserOrderList(userId, lastId, count);
    }

    private void doRefund(OrderEntity order) {
        // 调支付服务发起退款
        payPort.refund(order.getOutTradeNo(), order.getPayPrice(), "用户申请退款");
        // 更新订单状态
        orderRepository.updateRefund(order.getUserId(), order.getOrderId());
        log.info("退款成功 userId:{} orderId:{} outTradeNo:{}", order.getUserId(), order.getOrderId(), order.getOutTradeNo());
    }
}
