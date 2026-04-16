package cn.bugstack.trigger.job;

import cn.bugstack.domain.order.model.entity.OrderEntity;
import cn.bugstack.domain.order.service.IOrderService;
import cn.bugstack.infrastructure.adapter.port.PaySuccessRocketMqPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 超时关单
 * @create 2024-09-30 09:59
 */
@Slf4j
@Component()
public class TimeoutCloseOrderJob {

    @Resource
    private IOrderService orderService;

    @Resource
    private PaySuccessRocketMqPort paySuccessRocketMqPort;

    @Scheduled(cron = "0 0/30 * * * ?")
    public void exec() {
        try {
            log.info("任务；超时30分钟订单关闭");
            List<String> orderIds = orderService.queryTimeoutCloseOrderList();
            if (null == orderIds || orderIds.isEmpty()) {
                log.info("定时任务，超时30分钟订单关闭，暂无超时未支付订单 orderIds is null");
                return;
            }
            for (String orderId : orderIds) {
                // 查询订单详情，获取用户ID和营销类型
                OrderEntity orderEntity = orderService.queryOrderByOrderId(orderId);
                if (null == orderEntity) {
                    log.warn("定时任务，超时订单不存在 orderId: {}", orderId);
                    continue;
                }

                boolean status = orderService.changeOrderClose(orderId);
                log.info("定时任务，超时30分钟订单关闭 orderId: {} status：{}", orderId, status);

                if (status) {
                    // 发送MQ通知订单服务关单
                    paySuccessRocketMqPort.sendOrderCloseMessage(
                            orderEntity.getUserId(),
                            orderId,
                            orderEntity.getMarketType()
                    );
                }
            }
        } catch (Exception e) {
            log.error("定时任务，超时30分钟订单关闭失败", e);
        }
    }

}
