package cn.bugstack.domain.goods.adapter.repository;

/**
 * 商品/履约相关仓储出站接口
 */
public interface IGoodsRepository {

    /**
     * 将订单标记为履约完成
     */
    void changeOrderDealDone(String orderId);
}
