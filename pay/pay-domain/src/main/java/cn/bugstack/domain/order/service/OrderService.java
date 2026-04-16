package cn.bugstack.domain.order.service;

import cn.bugstack.domain.order.adapter.repository.IOrderRepository;
import cn.bugstack.domain.order.model.entity.CreateOrderEntity;
import cn.bugstack.domain.order.model.entity.MarketPayDiscountEntity;
import cn.bugstack.domain.order.model.entity.OrderEntity;
import cn.bugstack.domain.order.model.entity.PayOrderEntity;
import cn.bugstack.domain.order.model.valobj.MarketTypeVO;
import cn.bugstack.domain.order.model.valobj.OrderStatusVO;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class OrderService extends AbstractOrderService {

    @Value("${alipay.notify_url}")
    private String notifyUrl;
    @Value("${alipay.return_url}")
    private String returnUrl;

    @Resource
    private AlipayClient alipayClient;

    public OrderService(IOrderRepository repository) {
        super(repository);
    }

    @Override
    protected void doSaveOrder(CreateOrderEntity createOrderEntity) {
        repository.doSaveOrder(createOrderEntity);
    }


    @Override
    protected PayOrderEntity doPrepayOrder(String userId, String productId, String productName, String orderId, BigDecimal totalAmount,MarketTypeVO marketTypeVO, MarketPayDiscountEntity marketPayDiscountEntity) throws AlipayApiException {
        // 支付金额
        BigDecimal payAmount = marketPayDiscountEntity.getPayPrice();

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(notifyUrl);
        request.setReturnUrl(returnUrl);

        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderId);
        bizContent.put("total_amount", payAmount);
        bizContent.put("subject", productName);
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        request.setBizContent(bizContent.toString());

        String form = alipayClient.pageExecute(request).getBody();

        PayOrderEntity payOrderEntity = new PayOrderEntity();
        payOrderEntity.setOrderId(orderId);
        payOrderEntity.setPayUrl(form);
        payOrderEntity.setOrderStatus(OrderStatusVO.PAY_WAIT);

        // 营销信息
        payOrderEntity.setMarketType(marketTypeVO.getCode());
        payOrderEntity.setMarketDeductionAmount(null == marketPayDiscountEntity ? BigDecimal.ZERO : marketPayDiscountEntity.getDeductionPrice());
        payOrderEntity.setPayAmount(payAmount);

        repository.updateOrderPayInfo(payOrderEntity);

        return payOrderEntity;
    }

    @Override
    public void changeOrderPaySuccess(String orderId, Date payTime) {
//        OrderEntity orderEntity = repository.queryOrderByOrderId(orderId);
//        if (null == orderEntity) return;
//
//        if (MarketTypeVO.GroupBuyMarket.getCode().equals(orderEntity.getMarketType())) {
//
//            repository.changeMarketOrderPaySuccess(orderId);
//
//        } else if (MarketTypeVO.Normal.getCode().equals(orderEntity.getMarketType())) {
//            repository.changeOrderPaySuccess(orderId, payTime);
//
//        } else {
//            repository.changeOrderPaySuccess(orderId, payTime);
//        }

    }

    @Override
    public List<String> queryNoPayNotifyOrder() {
        return repository.queryNoPayNotifyOrder();
    }

    @Override
    public List<String> queryTimeoutCloseOrderList() {
        return repository.queryTimeoutCloseOrderList();
    }

    @Override
    public boolean changeOrderClose(String orderId) {
        return repository.changeOrderClose(orderId);
    }

    @Override
    public boolean changeOrderPayAfterClose(String orderId) {
        return repository.changeOrderPayAfterClose(orderId);
    }

    @Override
    public void changeOrderMarketSettlement(List<String> outTradeNoList) {
        repository.changeOrderMarketSettlement(outTradeNoList);
    }

    @Override
    public OrderEntity queryOrderByOrderId(String orderId) {
        return repository.queryOrderByOrderId(orderId);
    }

    @Override
    public String getPayUrl(String userId, String orderId) {
        OrderEntity orderEntity = repository.queryOrderByUserIdAndOrderId(userId, orderId);
        if (orderEntity == null) {
            return null;
        }
        if (orderEntity.getOrderStatusVO() != OrderStatusVO.PAY_WAIT) {
            log.warn("获取支付链接失败，订单状态非等待支付 userId:{} orderId:{} status:{}",
                    userId, orderId, orderEntity.getOrderStatusVO() != null ? orderEntity.getOrderStatusVO().getCode() : null);
            return null;
        }
        String payUrl = orderEntity.getPayUrl();
        return (payUrl != null && !payUrl.trim().isEmpty()) ? payUrl : null;
    }

    @Override
    public boolean refundMarketOrder(String userId, String orderId) {
        // 1. 查询订单信息，验证订单是否存在且属于该用户
        OrderEntity orderEntity = repository.queryOrderByUserIdAndOrderId(userId, orderId);
        if (null == orderEntity) {
            log.warn("退单失败，订单不存在或不属于该用户 userId:{} orderId:{}", userId, orderId);
            return false;
        }

        // 2. 检查订单状态，只有create、pay_wait、pay_success、deal_done状态的订单可以退单
        String status = orderEntity.getOrderStatusVO().getCode();
        if (OrderStatusVO.CLOSE.getCode().equals(status)) {
            log.warn("退单失败，订单已关闭 userId:{} orderId:{} status:{}", userId, orderId, status);
            return false;
        }


        // 4. 执行退单操作；CREATE 新创建订单，不需要退款
        if (OrderStatusVO.CREATE.getCode().equals(status) || OrderStatusVO.PAY_WAIT.getCode().equals(status)) {
            return repository.refundOrder(userId, orderId);
        } else {
            boolean result = repository.refundMarketOrder(userId, orderId);
            if (result) {
                log.info("退单成功 userId:{} orderId:{}", userId, orderId);
            } else {
                log.warn("退单失败 userId:{} orderId:{}", userId, orderId);
            }
            return result;
        }

    }

    @Override
    public boolean refundPayOrder(String userId, String orderId) throws AlipayApiException {
        // 1. 查询订单信息，验证订单是否存在且属于该用户
        OrderEntity orderEntity = repository.queryOrderByUserIdAndOrderId(userId, orderId);
        if (null == orderEntity) {
            log.warn("退款失败，订单不存在或不属于该用户 userId:{} orderId:{}", userId, orderId);
            return false;
        }

        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        AlipayTradeRefundModel refundModel = new AlipayTradeRefundModel();
        refundModel.setOutTradeNo(orderEntity.getOrderId());
        refundModel.setRefundAmount(orderEntity.getPayAmount().toString());
        refundModel.setRefundReason("交易退单");
        request.setBizModel(refundModel);

        // 交易退款
        AlipayTradeRefundResponse execute = alipayClient.execute(request);
        if (!execute.isSuccess()) return false;

        // 状态变更
        repository.refundOrder(userId, orderId);

        return true;
    }

    @Override
    public boolean closePayOrder(String outTradeNo) {
        OrderEntity orderEntity = repository.queryOrderByOrderId(outTradeNo);
        if (null == orderEntity) {
            log.warn("关单失败，支付订单不存在 outTradeNo:{}", outTradeNo);
            return false;
        }
        if (OrderStatusVO.PAY_WAIT.getCode().equals(orderEntity.getOrderStatusVO().getCode())) {
            boolean result = repository.changeOrderClose(outTradeNo);
            if (result) {
                log.info("支付订单关单成功 outTradeNo:{}", outTradeNo);
            } else {
                log.warn("支付订单关单失败 outTradeNo:{}", outTradeNo);
            }
            return result;
        }
        log.info("支付订单状态非等待支付，无需关单 outTradeNo:{} status:{}", outTradeNo, orderEntity.getOrderStatusVO().getCode());
        return true;
    }

    @Override
    public boolean refundPayOrderByOutTradeNo(String outTradeNo) throws AlipayApiException {
        OrderEntity orderEntity = repository.queryOrderByOrderId(outTradeNo);
        if (null == orderEntity) {
            log.warn("退款失败，支付订单不存在 outTradeNo:{}", outTradeNo);
            return false;
        }

        // 只有已支付状态才需要调支付宝退款
        if (!OrderStatusVO.PAY_SUCCESS.getCode().equals(orderEntity.getOrderStatusVO().getCode())) {
            log.info("支付订单状态非支付成功，跳过支付宝退款 outTradeNo:{} status:{}", outTradeNo, orderEntity.getOrderStatusVO().getCode());
            // 仍然更新本地状态为关闭
            repository.refundOrder(orderEntity.getUserId(), outTradeNo);
            return true;
        }

        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        AlipayTradeRefundModel refundModel = new AlipayTradeRefundModel();
        refundModel.setOutTradeNo(orderEntity.getOrderId());
        refundModel.setRefundAmount(orderEntity.getPayAmount().toString());
        refundModel.setRefundReason("拼团超时退款");
        request.setBizModel(refundModel);

        AlipayTradeRefundResponse execute = alipayClient.execute(request);
        if (!execute.isSuccess()) {
            log.error("支付宝退款失败 outTradeNo:{} msg:{}", outTradeNo, execute.getMsg());
            return false;
        }

        repository.refundOrder(orderEntity.getUserId(), outTradeNo);
        log.info("支付订单退款成功 outTradeNo:{}", outTradeNo);
        return true;
    }

}
