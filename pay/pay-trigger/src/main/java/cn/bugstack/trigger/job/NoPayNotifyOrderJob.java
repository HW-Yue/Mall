package cn.bugstack.trigger.job;

import cn.bugstack.domain.order.model.entity.OrderEntity;
import cn.bugstack.domain.order.model.valobj.OrderStatusVO;
import cn.bugstack.domain.order.service.IOrderService;
import cn.bugstack.domain.order.adapter.port.IPaySuccessPublisher;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 检测未接收到或未正确处理的支付回调通知
 * @create 2024-09-30 09:59
 */
@Slf4j
@Component()
public class NoPayNotifyOrderJob {

    @Resource
    private IOrderService orderService;
    @Resource
    private AlipayClient alipayClient;
    @Resource
    private IPaySuccessPublisher paySuccessPublisher;

    @Scheduled(cron = "0 0/30 * * * ?")
    public void exec() {
        try {
            log.info("任务；检测未接收到或未正确处理的支付回调通知");
            List<String> orderIds = orderService.queryNoPayNotifyOrder();
            if (null == orderIds || orderIds.isEmpty()) return;

            for (String orderId : orderIds) {
                OrderEntity orderEntity = orderService.queryOrderByOrderId(orderId);
                if (orderEntity == null) continue;

                // 已关单或已处于关单后付款状态的订单，不重复处理
                OrderStatusVO status = orderEntity.getOrderStatusVO();
                if (status == OrderStatusVO.CLOSE || status == OrderStatusVO.PAY_AFTER_CLOSE) {
                    continue;
                }

                AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
                AlipayTradeQueryModel bizModel = new AlipayTradeQueryModel();
                bizModel.setOutTradeNo(orderId);
                request.setBizModel(bizModel);

                AlipayTradeQueryResponse alipayTradeQueryResponse = alipayClient.execute(request);
                String code = alipayTradeQueryResponse.getCode();

                // 判断状态码
                if ("10000".equals(code)) {
                    orderService.changeOrderPaySuccess(orderId, alipayTradeQueryResponse.getSendPayDate());
                    paySuccessPublisher.sendSettlementMessage(
                            orderEntity.getUserId(),
                            orderId,
                            alipayTradeQueryResponse.getSendPayDate(),
                            orderEntity.getMarketType()
                    );
                }
            }
        } catch (Exception e) {
            log.error("检测未接收到或未正确处理的支付回调通知失败", e);
        }
    }

}
