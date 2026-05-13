package com.yue.order.infrastructure.adapter.repository;

import com.yue.order.domain.order.adapter.repository.IOrderRepository;
import com.yue.order.domain.order.model.entity.OrderEntity;
import com.yue.order.domain.order.model.valobj.MarketTypeVO;
import com.yue.order.domain.order.model.valobj.OrderStatusVO;
import com.yue.order.infrastructure.dao.IOrderDao;
import com.yue.order.infrastructure.dao.po.OrderPO;
import com.yue.api.IMallDubboService;
import com.yue.api.dto.SkuStockRequestDTO;
import com.yue.order.types.enums.ResponseCode;
import com.yue.order.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Repository
public class OrderRepository implements IOrderRepository {

    @Resource
    private IOrderDao orderDao;

    @DubboReference
    private IMallDubboService mallDubboService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveOrder(OrderEntity order) {
        // 普通商品：调用 mall 服务锁定库存
        if (MarketTypeVO.NORMAL == order.getMarketType()) {
            try {
                Boolean locked = mallDubboService.lockStock(new SkuStockRequestDTO(order.getGoodsId(), 1));
                if (!Boolean.TRUE.equals(locked)) {
                    throw new AppException(ResponseCode.STOCK_INSUFFICIENT.getCode(), "库存锁定失败");
                }
            } catch (AppException e) {
                throw e;
            } catch (Exception e) {
                log.warn("lockStock 失败 goodsId:{}", order.getGoodsId(), e);
                throw new AppException(ResponseCode.STOCK_INSUFFICIENT.getCode(), "库存锁定失败: " + e.getMessage());
            }
        }

        String orderId = order.getOrderId();
        if (StringUtils.isBlank(orderId) || StringUtils.isBlank(order.getOutTradeNo())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "orderId / outTradeNo 不能为空");
        }

        OrderPO po = OrderPO.builder()
                .orderId(orderId)
                .userId(order.getUserId())
                .goodsId(order.getGoodsId())
                .goodsName(order.getGoodsName())
                .goodsImageUrl(order.getGoodsImageUrl())
                .source(order.getSource())
                .channel(order.getChannel())
                .originalPrice(order.getOriginalPrice())
                .deductionPrice(order.getDeductionPrice())
                .payPrice(order.getPayPrice())
                .status(0)
                .outTradeNo(order.getOutTradeNo())
                .bizId(order.getGoodsId() + "_" + order.getUserId() + "_" + order.getOutTradeNo())
                .notifyType(order.getNotifyType() != null ? order.getNotifyType() : "MQ")
                .marketType(order.getMarketType().getCode())
                .build();

        try {
            orderDao.insert(po);
        } catch (DuplicateKeyException e) {
            OrderPO existing = orderDao.queryByOutTradeNo(order.getOutTradeNo());
            if (existing != null && StringUtils.equals(existing.getUserId(), order.getUserId())) {
                log.info("saveOrder 幂等返回已存在订单 userId:{} orderId:{} outTradeNo:{}",
                        order.getUserId(), existing.getOrderId(), order.getOutTradeNo());
                return existing.getOrderId();
            }
            throw e;
        }
        order.setOrderId(orderId);
        return orderId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String insertOrderWithoutMallLock(OrderEntity order) {
        String orderId = order.getOrderId();
        if (orderId == null || orderId.isBlank()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "orderId 不能为空");
        }
        OrderPO po = OrderPO.builder()
                .orderId(orderId)
                .userId(order.getUserId())
                .goodsId(order.getGoodsId())
                .goodsName(order.getGoodsName())
                .goodsImageUrl(order.getGoodsImageUrl())
                .source(order.getSource())
                .channel(order.getChannel())
                .originalPrice(order.getOriginalPrice())
                .deductionPrice(order.getDeductionPrice())
                .payPrice(order.getPayPrice())
                .status(0)
                .outTradeNo(order.getOutTradeNo())
                .bizId(order.getGoodsId() + "_" + order.getUserId() + "_" + order.getOutTradeNo())
                .notifyType(order.getNotifyType() != null ? order.getNotifyType() : "MQ")
                .marketType(order.getMarketType().getCode())
                .build();
        orderDao.insert(po);
        return orderId;
    }

    @Override
    public OrderEntity queryByUserIdAndOrderId(String userId, String orderId) {
        OrderPO po = orderDao.queryByUserIdAndOrderId(userId, orderId);
        return po == null ? null : toEntity(po);
    }

    @Override
    public OrderEntity queryByOutTradeNo(String outTradeNo) {
        OrderPO po = orderDao.queryByOutTradeNo(outTradeNo);
        return po == null ? null : toEntity(po);
    }

    @Override
    public List<String> queryTimeoutCloseOrderList() {
        return orderDao.queryTimeoutCloseOrderList();
    }

    @Override
    public void updatePayUrl(String userId, String outTradeNo, String payUrl) {
        orderDao.updatePayUrl(userId, outTradeNo, payUrl);
    }

    @Override
    public void updatePaySuccess(String outTradeNo, Date outTradeTime) {
        orderDao.updatePaySuccess(outTradeNo, outTradeTime);
    }

    @Override
    public int updateWaitShipByOrderId(String userId, String orderId) {
        return orderDao.updateWaitShipByOrderId(userId, orderId);
    }

    @Override
    public int updateShippedByOutTradeNo(String outTradeNo) {
        return orderDao.updateShippedByOutTradeNo(outTradeNo);
    }

    @Override
    public int updateDeliveredByOutTradeNo(String outTradeNo) {
        return orderDao.updateDeliveredByOutTradeNo(outTradeNo);
    }

    @Override
    public void updateWaitRefundByOutTradeNo(String outTradeNo) {
        orderDao.updateWaitRefundByOutTradeNo(outTradeNo);
    }

    @Override
    public void updateCloseByOutTradeNo(String outTradeNo) {
        orderDao.updateCloseByOutTradeNo(outTradeNo);
    }

    @Override
    public void updateRefundedByOutTradeNo(String outTradeNo) {
        orderDao.updateRefundedByOutTradeNo(outTradeNo);
    }

    @Override
    public List<OrderEntity> queryWaitShipOrderList(int count) {
        List<OrderPO> pos = orderDao.queryWaitShipOrderList(count);
        if (pos == null || pos.isEmpty()) {
            return new ArrayList<>();
        }
        List<OrderEntity> result = new ArrayList<>(pos.size());
        for (OrderPO po : pos) {
            result.add(toEntity(po));
        }
        return result;
    }

    @Override
    public void unlockStock(OrderEntity order) {
        if (MarketTypeVO.NORMAL == order.getMarketType()) {
            try {
                mallDubboService.unlockStock(new SkuStockRequestDTO(order.getGoodsId(), 1));
            } catch (Exception e) {
                log.warn("unlockStock 失败 goodsId:{}", order.getGoodsId(), e);
            }
        }
    }

    @Override
    public List<OrderEntity> queryUserOrderList(String userId, Long lastId, int count) {
        List<OrderPO> pos = orderDao.queryUserOrderList(userId, lastId, count);
        if (pos == null || pos.isEmpty()) {
            return new ArrayList<>();
        }
        List<OrderEntity> result = new ArrayList<>(pos.size());
        for (OrderPO po : pos) {
            result.add(toEntity(po));
        }
        return result;
    }

    private OrderEntity toEntity(OrderPO po) {
        return OrderEntity.builder()
                .id(po.getId())
                .orderId(po.getOrderId())
                .userId(po.getUserId())
                .goodsId(po.getGoodsId())
                .goodsName(po.getGoodsName())
                .goodsImageUrl(po.getGoodsImageUrl())
                .source(po.getSource())
                .channel(po.getChannel())
                .originalPrice(po.getOriginalPrice())
                .deductionPrice(po.getDeductionPrice())
                .payPrice(po.getPayPrice())
                .status(OrderStatusVO.fromDbValue(po.getStatus()))
                .outTradeNo(po.getOutTradeNo())
                .outTradeTime(po.getOutTradeTime())
                .payUrl(po.getPayUrl())
                .marketType(MarketTypeVO.fromCode(po.getMarketType()))
                .createTime(po.getCreateTime())
                .build();
    }
}
