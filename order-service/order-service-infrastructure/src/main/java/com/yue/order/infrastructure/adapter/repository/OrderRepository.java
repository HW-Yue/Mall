package com.yue.order.infrastructure.adapter.repository;

import com.yue.order.domain.order.adapter.repository.IOrderRepository;
import com.yue.order.domain.order.model.entity.OrderEntity;
import com.yue.order.domain.order.model.valobj.MarketTypeVO;
import com.yue.order.domain.order.model.valobj.OrderStatusVO;
import com.yue.order.infrastructure.dao.IOrderDao;
import com.yue.order.infrastructure.dao.po.OrderPO;
import com.yue.order.infrastructure.gateway.IMallService;
import com.yue.order.infrastructure.gateway.dto.SkuStockRequestDTO;
import com.yue.order.infrastructure.gateway.response.GatewayResponse;
import com.yue.order.types.enums.ResponseCode;
import com.yue.order.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Repository
public class OrderRepository implements IOrderRepository {

    @Resource
    private IOrderDao orderDao;

    @Resource
    private IMallService mallService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveOrder(OrderEntity order) {
        // 普通商品：调用 mall 服务锁定库存
        if (MarketTypeVO.NORMAL == order.getMarketType()) {
            GatewayResponse<Boolean> lockResult = mallService.lockStock(
                    new SkuStockRequestDTO(order.getGoodsId(), 1));
            if (lockResult == null || !"0000".equals(lockResult.getCode()) || Boolean.FALSE.equals(lockResult.getData())) {
                String info = lockResult != null ? lockResult.getInfo() : "mall 服务无响应";
                log.warn("lockStock 失败 goodsId:{} reason:{}", order.getGoodsId(), info);
                throw new AppException(ResponseCode.STOCK_INSUFFICIENT.getCode(), "库存锁定失败: " + info);
            }
        }

        // 生成内部订单号
        String orderId = RandomStringUtils.randomNumeric(12);

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
        order.setOrderId(orderId);
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
    public void updatePayUrl(String userId, String outTradeNo, String payUrl) {
        orderDao.updatePayUrl(userId, outTradeNo, payUrl);
    }

    @Override
    public void updatePaySuccess(String outTradeNo, Date outTradeTime) {
        orderDao.updatePaySuccess(outTradeNo, outTradeTime);
    }

    @Override
    public void updateRefund(String userId, String orderId) {
        orderDao.updateRefund(userId, orderId);
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
