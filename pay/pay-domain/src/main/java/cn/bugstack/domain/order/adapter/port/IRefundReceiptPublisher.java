package cn.bugstack.domain.order.adapter.port;

/**
 * 退款回执发布接口（出站，由 infrastructure 层实现）
 */
public interface IRefundReceiptPublisher {

    /**
     * 发布退款完成回执，并在本地事务中更新 pay_order 为 REFUNDED
     */
    void publishRefundReceipt(String userId, String orderId, String marketType);
}
