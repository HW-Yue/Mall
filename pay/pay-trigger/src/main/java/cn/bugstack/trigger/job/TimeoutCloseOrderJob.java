package cn.bugstack.trigger.job;

import cn.bugstack.domain.order.model.entity.OrderEntity;
import cn.bugstack.domain.order.service.IOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 支付台账异常对账关闭
 * @create 2024-09-30 09:59
 */
@Slf4j
@Component()
public class TimeoutCloseOrderJob {

    @Resource
    private IOrderService orderService;

    @Scheduled(cron = "0 0/30 * * * ?")
    public void exec() {
        try {
            log.info("payReconcileClose 开始：扫描长时间残留的 PAY_WAIT 支付单");
            List<String> orderIds = orderService.queryPayReconcileCloseOrderList();
            if (null == orderIds || orderIds.isEmpty()) {
                log.info("payReconcileClose 结束：暂无需要本地对账关闭的支付单");
                return;
            }
            for (String orderId : orderIds) {
                OrderEntity orderEntity = orderService.queryOrderByOrderId(orderId);
                if (null == orderEntity) {
                    log.warn("payReconcileClose 跳过：支付单不存在 orderId:{}", orderId);
                    continue;
                }

                boolean status = orderService.changeOrderClose(orderId);
                log.info("payReconcileClose 完成：仅关闭支付台账 orderId:{} marketType:{} status:{}",
                        orderId, orderEntity.getMarketType(), status);
            }
        } catch (Exception e) {
            log.error("payReconcileClose 失败：支付台账异常对账关闭异常", e);
        }
    }

}
