package com.yue.groupbuy.domain.trade.service;

import com.yue.groupbuy.domain.trade.adapter.port.IGroupBuyRefundMqProducer;
import com.yue.groupbuy.domain.trade.adapter.port.IGroupBuyTimeoutMqProducer;
import com.yue.groupbuy.domain.trade.adapter.port.IOrderServicePort;
import com.yue.groupbuy.domain.trade.adapter.repository.IGroupBuyRepository;
import com.yue.groupbuy.domain.trade.adapter.repository.ITradeRepository;
import com.yue.groupbuy.domain.trade.model.entity.*;
import com.yue.groupbuy.domain.trade.model.valobj.NotifyConfigVO;
import com.yue.groupbuy.domain.trade.model.valobj.NotifyTypeEnumVO;
import com.yue.groupbuy.types.enums.ResponseCode;
import com.yue.groupbuy.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

@Slf4j
@Service
public class GroupBuyDomainService implements IGroupBuyDomainService {

    @Resource
    private IGroupBuyRepository repository;
    @Resource
    private ITradeRepository tradeRepository;
    @Resource
    private IOrderServicePort orderServicePort;
    @Resource
    private ITradeLockOrderService tradeLockOrderService;
    @Resource
    private ITradeSettlementOrderService tradeSettlementOrderService;
    @Resource
    private ITradeRefundOrderService tradeRefundOrderService;
    @Resource
    private IGroupBuyRefundMqProducer groupBuyRefundMqProducer;
    @Resource
    private IGroupBuyTimeoutMqProducer groupBuyTimeoutMqProducer;

    @Override
    public MarketPayOrderEntity createGroupBuyOrder(LockOrderCommand command) {
        String userId = command.getUserId();
        String goodsId = command.getProductId();
        Long activityId = command.getActivityId();
        boolean newTeam = StringUtils.isBlank(command.getTeamId());

        log.info("拼团锁单开始 userId:{} activityId:{}", userId, activityId);

        if (StringUtils.isBlank(command.getOutTradeNo())) {
            command.setOutTradeNo(RandomStringUtils.randomNumeric(12));
        }

        // 查询活动定价
        ActivityPricingEntity pricing = repository.queryActivityPricing(activityId, goodsId);
        if (pricing == null) {
            throw new AppException(ResponseCode.ACTIVITY_NOT_FOUND);
        }

        // 构建聚合参数
        UserEntity userEntity = UserEntity.builder().userId(userId).build();

        PayActivityEntity payActivityEntity = PayActivityEntity.builder()
                .teamId(command.getTeamId())
                .activityId(activityId)
                .activityName(pricing.getActivityName())
                .startTime(pricing.getStartTime())
                .endTime(pricing.getEndTime())
                .validTime(pricing.getValidTime())
                .targetCount(pricing.getTarget())
                .build();

        PayDiscountEntity payDiscountEntity = PayDiscountEntity.builder()
                .source(command.getSource())
                .channel(command.getChannel())
                .goodsId(goodsId)
                .goodsName(pricing.getActivityName())
                .originalPrice(pricing.getOriginalPrice())
                .deductionPrice(pricing.getDeductionPrice())
                .payPrice(pricing.getPayPrice())
                .outTradeNo(command.getOutTradeNo())
                .notifyConfigVO(NotifyConfigVO.builder()
                        .notifyType(NotifyTypeEnumVO.MQ.getCode())
                        .notifyUrl(null)
                        .build())
                .build();

        // 调锁单服务（责任链：校验活动、用户限制、占库存）
        MarketPayOrderEntity marketPayOrderEntity;
        try {
            marketPayOrderEntity = tradeLockOrderService.lockMarketPayOrder(userEntity, payActivityEntity, payDiscountEntity);
        } catch (Exception e) {
            log.error("拼团锁单失败 userId:{}", userId, e);
            if (e instanceof AppException) throw (AppException) e;
            throw new AppException(ResponseCode.CREATE_ORDER_FAILED);
        }

        // 调 order-service 创建外部订单，并本地写入 t_order_group 关联记录
        String orderId;
        try {
            orderId = orderServicePort.createOrder(
                    userId, goodsId, pricing.getActivityName(), null,
                    pricing.getOriginalPrice(), pricing.getDeductionPrice(), pricing.getPayPrice(),
                    command.getSource(), command.getChannel(), command.getOutTradeNo(),
                    marketPayOrderEntity.getTeamId(), activityId,
                    payActivityEntity.getStartTime(), payActivityEntity.getEndTime());
        } catch (Exception e) {
            log.error("调 order-service 创建订单失败 userId:{}", userId, e);
            throw new AppException(ResponseCode.CREATE_ORDER_FAILED);
        }

        if (newTeam) {
            scheduleTeamTimeoutIfNecessary(marketPayOrderEntity.getTeamId());
        }

        log.info("拼团锁单完成 userId:{} orderId:{} teamId:{}", userId, orderId, marketPayOrderEntity.getTeamId());
        return MarketPayOrderEntity.builder()
                .orderId(orderId)
                .teamId(marketPayOrderEntity.getTeamId())
                .originalPrice(marketPayOrderEntity.getOriginalPrice())
                .deductionPrice(marketPayOrderEntity.getDeductionPrice())
                .payPrice(marketPayOrderEntity.getPayPrice())
                .build();
    }

    @Override
    public void settlementGroupBuyOrder(SettlementCommand command) {
        log.info("拼团结算开始 userId:{} outTradeNo:{}", command.getUserId(), command.getOutTradeNo());

        TradePaySuccessEntity tradePaySuccessEntity = TradePaySuccessEntity.builder()
                .source("")
                .channel("")
                .userId(command.getUserId())
                .outTradeNo(command.getOutTradeNo())
                .outTradeTime(command.getOutTradeTime())
                .build();

        try {
            tradeSettlementOrderService.settlementMarketPayOrder(tradePaySuccessEntity);
        } catch (Exception e) {
            log.error("拼团结算失败 userId:{} outTradeNo:{}", command.getUserId(), command.getOutTradeNo(), e);
            if (e instanceof AppException) throw (AppException) e;
        }
    }

    @Override
    public void refundGroupBuyOrder(String userId, String orderId) {
        log.info("拼团退款开始 userId:{} orderId:{}", userId, orderId);

        String outTradeNo = tradeRepository.queryOutTradeNoByOrderId(userId, orderId);
        if (StringUtils.isBlank(outTradeNo)) {
            throw new AppException(ResponseCode.ORDER_NOT_FOUND);
        }

        TradeRefundCommandEntity refundCommandEntity = TradeRefundCommandEntity.builder()
                .userId(userId)
                .outTradeNo(outTradeNo)
                .source("")
                .channel("")
                .build();

        try {
            tradeRefundOrderService.refundOrder(refundCommandEntity);
        } catch (Exception e) {
            log.error("拼团退款失败 userId:{} orderId:{}", userId, orderId, e);
            if (e instanceof AppException) throw (AppException) e;
        }
    }

    @Override
    public void handleTimeoutRefund(String teamId) {
        log.info("拼团超时退款处理开始 teamId:{}", teamId);

        // 1. 乐观锁更新团队状态为失败（仅当当前为拼单中 status=0 时生效）
        int updated = tradeRepository.updateTeamStatus2Fail(teamId);
        if (updated == 0) {
            log.info("拼团超时退款处理跳过，团队状态已变更 teamId:{}", teamId);
            return;
        }

        // 2. 处理未支付订单：本地关单 + 发送 order-close-group-buy MQ
        List<TeamOrderEntity> unpaidOrders = tradeRepository.queryUnpaidOrdersByTeamId(teamId);
        if (unpaidOrders != null && !unpaidOrders.isEmpty()) {
            // 先批量关闭未支付订单
            int closedCount = tradeRepository.closeUnpaidOrdersByTeamId(teamId);
            log.info("拼团超时退款处理，批量关闭未支付订单 teamId:{} closedCount:{}", teamId, closedCount);
            for (TeamOrderEntity order : unpaidOrders) {
                try {
                    groupBuyRefundMqProducer.sendOrderCloseMessage(order.getOutTradeNo(), order.getUserId());
                } catch (Exception e) {
                    log.error("拼团超时退款处理，发送未支付关单 MQ 失败 teamId:{} userId:{} orderId:{}", teamId, order.getUserId(), order.getOrderId(), e);
                }
            }
        }

        // 3. 处理已支付订单：本地标记退款处理中 + 发送 pay-refund-group-buy MQ
        List<TeamOrderEntity> paidOrders = tradeRepository.queryPaidOrdersByTeamId(teamId);
        if (paidOrders != null && !paidOrders.isEmpty()) {
            for (TeamOrderEntity order : paidOrders) {
                try {
                    // 本地先更新为退款处理中，最终完成以后再由 pay/order 链路确认
                    tradeRepository.updateOrder2Refund(order.getUserId(), order.getOrderId());
                    // 发送 MQ 给 order-service 和 pay-service
                    groupBuyRefundMqProducer.sendPayRefundMessage(order.getOutTradeNo(), order.getUserId());
                } catch (Exception e) {
                    log.error("拼团超时退款处理，发送已支付退款 MQ 失败 teamId:{} userId:{} orderId:{}", teamId, order.getUserId(), order.getOrderId(), e);
                }
            }
        }

        log.info("拼团超时退款处理完成 teamId:{} unpaidCount:{} paidCount:{}",
                teamId,
                unpaidOrders == null ? 0 : unpaidOrders.size(),
                paidOrders == null ? 0 : paidOrders.size());
    }

    @Override
    public void handlePayRefund(String outTradeNo) {
        int updated = tradeRepository.updateOrder2Refunded(outTradeNo);
        if (updated == 0) {
            log.info("拼团退款完成回执跳过，订单状态未命中退款处理中 outTradeNo:{}", outTradeNo);
            return;
        }
        log.info("拼团退款完成回执处理成功 outTradeNo:{}", outTradeNo);
    }

    private void scheduleTeamTimeoutIfNecessary(String teamId) {
        GroupBuyTeamEntity team = tradeRepository.queryGroupBuyTeamByTeamId(teamId);
        if (team == null || team.getValidEndTime() == null) {
            log.warn("拼团队级超时消息发送跳过，团队信息缺失 teamId:{}", teamId);
            return;
        }

        long deliverTimeMs = team.getValidEndTime().getTime();
        groupBuyTimeoutMqProducer.sendTimeoutRefundMessage(teamId, deliverTimeMs);
        log.info("拼团队级超时消息已发送 teamId:{} deliverTimeMs:{}", teamId, deliverTimeMs);
    }

}
