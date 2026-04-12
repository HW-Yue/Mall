package cn.bugstack.domain.order.service;

import cn.bugstack.domain.order.adapter.repository.IOrderRepository;
import cn.bugstack.domain.order.model.entity.*;
import cn.bugstack.domain.order.model.valobj.MarketTypeVO;
import com.alipay.api.AlipayApiException;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
public abstract class AbstractOrderService implements IOrderService {

    protected final IOrderRepository repository;


    public AbstractOrderService(IOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public PayOrderEntity createOrder(CreateOrderEntity createOrderEntity) throws Exception {

        // 写入本地订单表
        this.doSaveOrder(createOrderEntity);

        MarketPayDiscountEntity marketPayDiscountEntity =MarketPayDiscountEntity.builder()
                .payPrice(createOrderEntity.getPayPrice())
                .originalPrice(createOrderEntity.getOriginalPrice())
                .deductionPrice(createOrderEntity.getDeductionPrice())
                .build();


        // 调用支付接口,拿到支付url
        PayOrderEntity payOrderEntity = doPrepayOrder(createOrderEntity.getUserId(),
                createOrderEntity.getProductId(),
                createOrderEntity.getProductName(),
                createOrderEntity.getOrderId(),
                createOrderEntity.getPayPrice(),
                createOrderEntity.getMarketTypeVO(),
                marketPayDiscountEntity);

        log.info("创建订单-完成，生成支付单。userId: {} orderId: {} payUrl: {}", createOrderEntity.getUserId(), createOrderEntity.getOrderId(), payOrderEntity.getPayUrl());

        return PayOrderEntity.builder()
                .orderId(createOrderEntity.getOrderId())
                .payUrl(payOrderEntity.getPayUrl())
                .build();
    }

    protected abstract void doSaveOrder(CreateOrderEntity createOrderEntity) throws Exception;

    protected abstract PayOrderEntity doPrepayOrder(String userId, String productId, String productName, String orderId, BigDecimal totalAmount,MarketTypeVO marketTypeVO, MarketPayDiscountEntity marketPayDiscountEntity) throws AlipayApiException;

    @Override
    public List<OrderEntity> queryUserOrderList(String userId, Long lastId, Integer pageSize) {
        return repository.queryUserOrderList(userId, lastId, pageSize);
    }

}
