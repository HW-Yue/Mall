package com.yue.order.domain.order.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yue.order.domain.order.adapter.port.IGroupBuyOrderPendingPublisher;
import com.yue.order.domain.order.adapter.port.INormalOrderPendingPublisher;
import com.yue.order.domain.order.adapter.port.IOrderClosePublisher;
import com.yue.order.domain.order.adapter.port.IOrderPaidPublisher;
import com.yue.order.domain.order.adapter.port.IOrderRefundPublisher;
import com.yue.order.domain.order.adapter.port.IOrderShipTaskPublisher;
import com.yue.order.domain.order.adapter.port.IPayPort;
import com.yue.order.domain.order.adapter.repository.IOrderCacheRepository;
import com.yue.order.domain.order.adapter.repository.IOrderRepository;
import com.yue.order.domain.order.model.entity.CreateOrderCommand;
import com.yue.order.domain.order.model.entity.NormalOrderEnqueueResult;
import com.yue.order.domain.order.model.entity.OrderEntity;
import com.yue.order.domain.order.model.valobj.MarketTypeVO;
import com.yue.order.domain.order.model.valobj.OrderEvent;
import com.yue.order.domain.order.model.valobj.OrderStateMachine;
import com.yue.order.domain.order.model.valobj.OrderStatusVO;
import com.yue.order.domain.order.service.support.OrderIdGenerator;
import com.yue.order.types.enums.ResponseCode;
import com.yue.order.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OrderDomainService implements IOrderDomainService {

    private final OrderIdGenerator orderIdGenerator = new OrderIdGenerator();

    @Resource
    private IOrderRepository orderRepository;

    @Resource
    private IPayPort payPort;

    /** 下游事件发布（由 infrastructure 层实现，通过 Spring 注入） */
    @Resource
    private IOrderPaidPublisher orderPaidPublisher;

    @Resource
    private IOrderClosePublisher orderClosePublisher;

    /** 退款事件发布（由 infrastructure 层实现） */
    @Resource
    private IOrderRefundPublisher refundPublisher;

    @Resource
    private INormalOrderPendingPublisher normalOrderPendingPublisher;

    @Resource
    private IGroupBuyOrderPendingPublisher groupBuyOrderPendingPublisher;

    @Resource
    private IOrderCacheRepository orderCacheRepository;

    @Resource
    private IOrderShipTaskPublisher orderShipTaskPublisher;

    @Value("${app.order.allow-direct-normal-create-order:true}")
    private boolean allowDirectNormalCreateOrder;

    @Value("${app.order.get-pay-url-pending-retries:6}")
    private int getPayUrlPendingRetries;

    @Value("${app.order.get-pay-url-pending-wait-ms:50}")
    private long getPayUrlPendingWaitMs;

    @Value("${app.order.pending-marker-ttl-minutes:30}")
    private long pendingMarkerTtlMinutes;

    @Override
    public String createOrder(CreateOrderCommand command) {
        if (!allowDirectNormalCreateOrder
                && "normal".equalsIgnoreCase(StringUtils.trimToEmpty(command.getMarketType()))) {
            throw new AppException(
                    ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "普通商品请走商城 create_normal_order 接口，直连 create_order 已关闭");
        }
        MarketTypeVO marketType = MarketTypeVO.fromCode(command.getMarketType());
        String orderId = orderIdGenerator.nextOrderId();
        String outTradeNo = orderIdGenerator.nextOutTradeNo();

        // 拼团：异步落库（生成 ID → 写 Redis 标记 → 发 MQ → 立即返回），与普通单形态对齐
        if (MarketTypeVO.GROUP_BUY == marketType) {
            return submitGroupBuyOrderAsync(command, orderId, outTradeNo);
        }

        // 构建订单实体
        OrderEntity order = OrderEntity.builder()
                .orderId(orderId)
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
                .marketType(marketType)
                .notifyType("MQ")
                .build();

        // 保存订单（对 normal 类型同时锁 SKU 库存）
        String savedOrderId = orderRepository.saveOrder(order);
        log.info("createOrder 完成 userId:{} orderId:{} marketType:{}", command.getUserId(), savedOrderId, command.getMarketType());
        return savedOrderId;
    }

    private String submitGroupBuyOrderAsync(CreateOrderCommand command, String orderId, String outTradeNo) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("orderId", orderId);
        msg.put("outTradeNo", outTradeNo);
        msg.put("userId", command.getUserId());
        msg.put("goodsId", command.getGoodsId());
        msg.put("goodsName", command.getGoodsName());
        msg.put("goodsImageUrl", command.getGoodsImageUrl());
        msg.put("source", command.getSource());
        msg.put("channel", command.getChannel());
        msg.put("originalPrice", command.getOriginalPrice());
        msg.put("deductionPrice", command.getDeductionPrice() != null ? command.getDeductionPrice() : BigDecimal.ZERO);
        msg.put("payPrice", command.getPayPrice());
        msg.put("marketType", MarketTypeVO.GROUP_BUY.getCode());
        msg.put("notifyType", "MQ");

        Duration ttl = Duration.ofMinutes(Math.max(1, pendingMarkerTtlMinutes));
        orderCacheRepository.markPending(command.getUserId(), orderId, outTradeNo, ttl);
        try {
            groupBuyOrderPendingPublisher.publishInsertSync(JSON.toJSONString(msg));
        } catch (RuntimeException e) {
            orderCacheRepository.clearPending(command.getUserId(), orderId);
            throw e;
        }
        log.info("createOrder 拼团已入队 userId:{} orderId:{} outTradeNo:{}",
                command.getUserId(), orderId, outTradeNo);
        return orderId;
    }

    @Override
    public OrderEntity queryByUserIdAndOutTradeNo(String userId, String outTradeNo) {
        if (StringUtils.isAnyBlank(userId, outTradeNo)) {
            return null;
        }
        OrderEntity order = orderRepository.queryByOutTradeNo(outTradeNo.trim());
        if (order == null || !StringUtils.equals(order.getUserId(), userId)) {
            return null;
        }
        return order;
    }

    @Override
    public NormalOrderEnqueueResult submitNormalOrderFromMall(CreateOrderCommand command) {
        if (!"normal".equalsIgnoreCase(StringUtils.trimToEmpty(command.getMarketType()))) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "仅支持 marketType=normal");
        }
        if (StringUtils.isAnyBlank(command.getUserId(), command.getGoodsId()) || command.getPayPrice() == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "userId、goodsId、payPrice 不能为空");
        }
        String orderId = orderIdGenerator.nextOrderId();
        String outTradeNo = orderIdGenerator.nextOutTradeNo();

        Map<String, Object> msg = new HashMap<>();
        msg.put("orderId", orderId);
        msg.put("outTradeNo", outTradeNo);
        msg.put("userId", command.getUserId());
        msg.put("goodsId", command.getGoodsId());
        msg.put("goodsName", command.getGoodsName());
        msg.put("goodsImageUrl", command.getGoodsImageUrl());
        msg.put("source", command.getSource());
        msg.put("channel", command.getChannel());
        msg.put("originalPrice", command.getOriginalPrice());
        msg.put("deductionPrice", command.getDeductionPrice() != null ? command.getDeductionPrice() : BigDecimal.ZERO);
        msg.put("payPrice", command.getPayPrice());
        msg.put("marketType", "normal");
        msg.put("notifyType", "MQ");

        Duration ttl = Duration.ofMinutes(Math.max(1, pendingMarkerTtlMinutes));
        orderCacheRepository.markPending(command.getUserId(), orderId, outTradeNo, ttl);
        try {
            normalOrderPendingPublisher.publishInsertSync(JSON.toJSONString(msg));
        } catch (RuntimeException e) {
            orderCacheRepository.clearPending(command.getUserId(), orderId);
            throw e;
        }
        log.info("submitNormalOrderFromMall 已入队 userId:{} orderId:{}", command.getUserId(), orderId);
        return NormalOrderEnqueueResult.builder().orderId(orderId).build();
    }

    @Override
    public String getPayUrl(String userId, String orderId) {
        OrderEntity order = orderRepository.queryByUserIdAndOrderId(userId, orderId);
        if (order == null) {
            // DB 未命中 → 用 Redis 存在标记区分「订单确实在飞」与「订单不存在」
            // 标记存在 → 走重试循环兜底；标记不存在 → 直接报错，避免无谓阻塞
            if (!orderCacheRepository.existsPending(userId, orderId)) {
                throw new AppException(ResponseCode.ORDER_NOT_FOUND.getCode(), "订单不存在: " + orderId);
            }
            int attempts = Math.max(1, getPayUrlPendingRetries);
            for (int i = 0; i < attempts; i++) {
                try {
                    Thread.sleep(Math.max(1, getPayUrlPendingWaitMs));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                order = orderRepository.queryByUserIdAndOrderId(userId, orderId);
                if (order != null) {
                    break;
                }
            }
        }
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

        // 如果订单已关闭（例如先收到关单 MQ），但支付宝回调已扣款，需要触发退款
        if (order.getStatus() == OrderStatusVO.CLOSE) {
            log.warn("handlePaySuccess 订单已关闭但收到支付成功消息，触发退款 outTradeNo:{} marketType:{}", outTradeNo, marketType);
            refundPublisher.publishPayRefund(order.getUserId(), outTradeNo, marketType);
            return;
        }

        if (order.getStatus() != OrderStatusVO.LOCK) {
            log.info("handlePaySuccess 订单状态已更新，跳过 outTradeNo:{} status:{}", outTradeNo, order.getStatus());
            return;
        }

        // 更新订单状态为支付成功
        orderRepository.updatePaySuccess(outTradeNo, outTradeTime != null ? outTradeTime : new Date());
        String resolvedMarketType = order.getMarketType() != null
                ? order.getMarketType().getCode()
                : StringUtils.defaultIfBlank(marketType, MarketTypeVO.NORMAL.getCode());
        log.info("handlePaySuccess 更新订单成功 outTradeNo:{} marketType:{}", outTradeNo, resolvedMarketType);

        // 发布下游事件（order-paid-normal / order-paid-group_buy / order-paid-seckill）
        orderPaidPublisher.publishOrderPaid(order.getUserId(), order.getOrderId(), outTradeNo, resolvedMarketType, outTradeTime);

        // 普通大促 / 秒杀：支付成功后直接推进到履约发货任务
        if (!MarketTypeVO.GROUP_BUY.getCode().equals(resolvedMarketType)) {
            orderShipTaskPublisher.publishOrderShipTask(order.getUserId(), order.getOrderId(), outTradeNo);
            log.info("handlePaySuccess 已发布发货任务 outTradeNo:{} marketType:{}", outTradeNo, resolvedMarketType);
        }
    }

    @Override
    public void handleGroupBuySuccess(String message) {
        if (StringUtils.isBlank(message)) {
            log.warn("handleGroupBuySuccess 收到空消息");
            return;
        }

        JSONObject dto = JSON.parseObject(message);
        String teamId = dto.getString("teamId");
        JSONArray orders = dto.getJSONArray("orders");
        if (orders == null || orders.isEmpty()) {
            log.info("handleGroupBuySuccess 无订单可处理 teamId:{}", teamId);
            return;
        }

        for (int i = 0; i < orders.size(); i++) {
            JSONObject item = orders.getJSONObject(i);
            if (item == null) {
                continue;
            }
            String userId = item.getString("userId");
            String orderId = item.getString("orderId");
            if (StringUtils.isAnyBlank(userId, orderId)) {
                log.warn("handleGroupBuySuccess 订单信息缺失 teamId:{} item:{}", teamId, item.toJSONString());
                continue;
            }
            handleGroupBuySuccessOne(teamId, userId, orderId);
        }
    }

    @Override
    public void handleShipTask(String outTradeNo) {
        if (StringUtils.isBlank(outTradeNo)) {
            log.warn("handleShipTask 收到空 outTradeNo");
            return;
        }

        OrderEntity order = orderRepository.queryByOutTradeNo(outTradeNo);
        if (order == null) {
            log.warn("handleShipTask 订单不存在 outTradeNo:{}", outTradeNo);
            return;
        }
        if (order.getStatus() == OrderStatusVO.SHIPPED || order.getStatus() == OrderStatusVO.DELIVERED) {
            log.info("handleShipTask 订单已发货/签收，跳过 outTradeNo:{} status:{}", outTradeNo, order.getStatus().getCode());
            return;
        }
        if (order.getStatus() == OrderStatusVO.CLOSE
                || order.getStatus() == OrderStatusVO.WAIT_REFUND
                || order.getStatus() == OrderStatusVO.REFUNDED) {
            log.warn("handleShipTask 订单已关闭/退款，禁止发货 outTradeNo:{} status:{}", outTradeNo, order.getStatus().getCode());
            return;
        }
        if (order.getStatus() != OrderStatusVO.WAIT_SHIP) {
            throw new AppException(ResponseCode.ORDER_STATUS_ERROR.getCode(),
                    "订单未进入待发货状态，无法发货: " + order.getStatus().getCode());
        }

        // 状态机软校验：WAIT_SHIP → SHIPPED
        OrderStateMachine.next(order.getStatus(), OrderEvent.SHIP_DONE);

        int updated = orderRepository.updateShippedByOutTradeNo(outTradeNo);
        if (updated == 1) {
            log.info("handleShipTask 发货完成 outTradeNo:{} orderId:{}", outTradeNo, order.getOrderId());
            return;
        }

        OrderEntity refreshed = orderRepository.queryByOutTradeNo(outTradeNo);
        if (refreshed != null && (refreshed.getStatus() == OrderStatusVO.SHIPPED || refreshed.getStatus() == OrderStatusVO.DELIVERED)) {
            log.info("handleShipTask 幂等跳过 outTradeNo:{} status:{}", outTradeNo, refreshed.getStatus().getCode());
            return;
        }
        throw new AppException(ResponseCode.UPDATE_ZERO.getCode(), "发货状态更新失败: " + outTradeNo);
    }

    @Override
    public void handleDelivered(String outTradeNo) {
        if (StringUtils.isBlank(outTradeNo)) {
            log.warn("handleDelivered 收到空 outTradeNo");
            return;
        }

        OrderEntity order = orderRepository.queryByOutTradeNo(outTradeNo);
        if (order == null) {
            log.warn("handleDelivered 订单不存在 outTradeNo:{}", outTradeNo);
            return;
        }
        if (order.getStatus() == OrderStatusVO.DELIVERED) {
            log.info("handleDelivered 订单已签收，跳过 outTradeNo:{}", outTradeNo);
            return;
        }
        if (order.getStatus() != OrderStatusVO.SHIPPED) {
            throw new AppException(ResponseCode.ORDER_STATUS_ERROR.getCode(),
                    "订单未发货，无法签收: " + order.getStatus().getCode());
        }

        // 状态机软校验：SHIPPED → DELIVERED
        OrderStateMachine.next(order.getStatus(), OrderEvent.DELIVER);

        int updated = orderRepository.updateDeliveredByOutTradeNo(outTradeNo);
        if (updated == 1) {
            log.info("handleDelivered 签收完成 outTradeNo:{} orderId:{}", outTradeNo, order.getOrderId());
            return;
        }

        OrderEntity refreshed = orderRepository.queryByOutTradeNo(outTradeNo);
        if (refreshed != null && refreshed.getStatus() == OrderStatusVO.DELIVERED) {
            log.info("handleDelivered 幂等跳过 outTradeNo:{}", outTradeNo);
            return;
        }
        throw new AppException(ResponseCode.UPDATE_ZERO.getCode(), "签收状态更新失败: " + outTradeNo);
    }

    @Override
    public void refund(String userId, String orderId) {
        OrderEntity order = orderRepository.queryByUserIdAndOrderId(userId, orderId);
        if (order == null) {
            throw new AppException(ResponseCode.ORDER_NOT_FOUND.getCode(), "订单不存在: " + orderId);
        }
        if (order.getStatus() == OrderStatusVO.CLOSE) {
            throw new AppException(ResponseCode.ORDER_STATUS_ERROR.getCode(), "订单已关闭，无法退款");
        }
        if (order.getStatus() == OrderStatusVO.WAIT_REFUND) {
            throw new AppException(ResponseCode.ORDER_STATUS_ERROR.getCode(), "订单退款处理中，请勿重复提交");
        }
        if (order.getStatus() == OrderStatusVO.REFUNDED) {
            throw new AppException(ResponseCode.ORDER_STATUS_ERROR.getCode(), "订单已退款，无需重复提交");
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

    @Override
    public void handleOrderClose(String outTradeNo) {
        OrderEntity order = orderRepository.queryByOutTradeNo(outTradeNo);
        if (order == null) {
            log.warn("handleOrderClose 订单不存在 outTradeNo:{}", outTradeNo);
            return;
        }

        // 如果订单已支付但收到关单消息（竞态：pay-success 先于 order-close 到达），触发退款
        if (order.getStatus() == OrderStatusVO.PAY_SUCCESS) {
            log.warn("handleOrderClose 订单已支付但收到关单消息，触发退款 outTradeNo:{} marketType:{}",
                    outTradeNo, order.getMarketType().getCode());
            refundPublisher.publishPayRefund(order.getUserId(), outTradeNo, order.getMarketType().getCode());
            return;
        }

        if (order.getStatus() != OrderStatusVO.LOCK) {
            log.info("handleOrderClose 订单状态非锁定，跳过 outTradeNo:{} status:{}", outTradeNo, order.getStatus());
            return;
        }

        // 状态机软校验：LOCK → CLOSE
        OrderStateMachine.next(order.getStatus(), OrderEvent.CLOSE);

        // 释放库存（普通商品通过 mall 服务解锁）
        orderRepository.unlockStock(order);

        orderRepository.updateCloseByOutTradeNo(outTradeNo);
        log.info("handleOrderClose 订单关闭成功 outTradeNo:{} marketType:{}", outTradeNo, order.getMarketType().getCode());
    }

    @Override
    public void handlePayRefund(String outTradeNo) {
        OrderEntity order = orderRepository.queryByOutTradeNo(outTradeNo);
        if (order == null) {
            log.warn("handlePayRefund 订单不存在 outTradeNo:{}", outTradeNo);
            return;
        }
        if (order.getStatus() == OrderStatusVO.REFUNDED) {
            log.info("handlePayRefund 订单已是退款完成状态，跳过 outTradeNo:{}", outTradeNo);
            return;
        }
        // 状态机软校验：WAIT_REFUND → REFUNDED
        OrderStateMachine.next(order.getStatus(), OrderEvent.REFUND_COMPLETE);

        orderRepository.updateRefundedByOutTradeNo(outTradeNo);
        log.info("handlePayRefund 订单退款确认成功 outTradeNo:{} marketType:{}", outTradeNo, order.getMarketType().getCode());
    }

    private void handleGroupBuySuccessOne(String teamId, String userId, String orderId) {
        OrderEntity order = orderRepository.queryByUserIdAndOrderId(userId, orderId);
        if (order == null) {
            log.warn("handleGroupBuySuccess 订单不存在 teamId:{} userId:{} orderId:{}", teamId, userId, orderId);
            return;
        }
        log.info("handleGroupBuySuccess 开始发送事务发货消息 teamId:{} orderId:{} status:{}",
                teamId, orderId, order.getStatus() != null ? order.getStatus().getCode() : null);
        orderShipTaskPublisher.publishOrderShipTask(userId, orderId, order.getOutTradeNo());
    }

    @Override
    public void triggerTimeoutClose() {
        List<String> outTradeNos = orderRepository.queryTimeoutCloseOrderList();
        if (outTradeNos == null || outTradeNos.isEmpty()) {
            log.info("businessTimeoutClose 暂无超时未支付订单");
            return;
        }

        for (String outTradeNo : outTradeNos) {
            OrderEntity order = orderRepository.queryByOutTradeNo(outTradeNo);
            if (order == null) {
                log.warn("businessTimeoutClose 订单不存在 outTradeNo:{}", outTradeNo);
                continue;
            }

            handleOrderClose(outTradeNo);
            orderClosePublisher.publishOrderClose(order.getUserId(), order.getOrderId(), outTradeNo, order.getMarketType().getCode());
            log.info("businessTimeoutClose 已触发关单 outTradeNo:{} marketType:{}",
                    outTradeNo, order.getMarketType().getCode());
        }
    }

    private void doRefund(OrderEntity order) {
        if (order.getStatus() == OrderStatusVO.LOCK) {
            // 状态机软校验：LOCK → CLOSE
            OrderStateMachine.next(order.getStatus(), OrderEvent.CLOSE);
            // 发 MQ：由 order-close listener 异步关单 + 解库存；pay / seckill 等下游也通过此 topic 清理台账
            orderClosePublisher.publishOrderClose(order.getUserId(), order.getOrderId(),
                    order.getOutTradeNo(), order.getMarketType().getCode());
            log.info("doRefund LOCK 已发 order-close-{} userId:{} orderId:{} outTradeNo:{}",
                    order.getMarketType().getCode(), order.getUserId(), order.getOrderId(), order.getOutTradeNo());
            return;
        }

        if (order.getStatus() == OrderStatusVO.CLOSE
                || order.getStatus() == OrderStatusVO.WAIT_REFUND
                || order.getStatus() == OrderStatusVO.REFUNDED) {
            log.info("退款请求无需重复处理 userId:{} orderId:{} outTradeNo:{} status:{}",
                    order.getUserId(), order.getOrderId(), order.getOutTradeNo(), order.getStatus().getCode());
            return;
        }

        // 状态机软校验：PAY_SUCCESS → WAIT_REFUND
        OrderStateMachine.next(order.getStatus(), OrderEvent.REFUND_REQUEST);
        refundPublisher.publishPayRefund(order.getUserId(), order.getOutTradeNo(), order.getMarketType().getCode());
        log.info("退款请求已入MQ userId:{} orderId:{} outTradeNo:{} marketType:{}",
                order.getUserId(), order.getOrderId(), order.getOutTradeNo(), order.getMarketType().getCode());
    }
}
