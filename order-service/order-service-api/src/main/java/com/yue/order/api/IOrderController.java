package com.yue.order.api;

import com.yue.order.api.dto.CreateOrderRequestDTO;
import com.yue.order.api.dto.CreateOrderResponseDTO;
import com.yue.order.api.dto.GetPayUrlRequestDTO;
import com.yue.order.api.dto.QuerySeckillOrderRequestDTO;
import com.yue.order.api.dto.QuerySeckillOrderResponseDTO;
import com.yue.order.api.dto.QueryOrderByOutTradeNoRequestDTO;
import com.yue.order.api.dto.QueryUserOrderListRequestDTO;
import com.yue.order.api.dto.QueryUserOrderListResponseDTO;
import com.yue.order.api.dto.RefundRequestDTO;
import com.yue.order.api.response.Response;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 订单服务 HTTP 接口契约
 */
public interface IOrderController {

    /** 创建订单（锁单）：普通/拼团/秒杀都调此接口 */
    Response<CreateOrderResponseDTO> createOrder(CreateOrderRequestDTO request);

    /**
     * 普通商品：mall 已锁库存后调用，同步发 MQ 异步落单；建议仅服务间调用，可配 X-Internal-Token
     */
    @PostMapping("create_order_normal_from_mall")
    Response<CreateOrderResponseDTO> createOrderNormalFromMall(
            @RequestBody CreateOrderRequestDTO request,
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken);

    /** 获取支付 URL：用户在支付页点击"立即支付"时调用 */
    Response<String> getPayUrl(GetPayUrlRequestDTO request);

    /** 普通订单退款：前端直接调，含业务状态校验 */
    Response<Boolean> refund(RefundRequestDTO request);

    /** 营销订单退款执行：由营销服务调用，跳过业务校验直接退款 */
    Response<Boolean> refundExecute(RefundRequestDTO request);

    /** 查询用户订单列表（游标分页） */
    @PostMapping("query_user_order_list")
    Response<QueryUserOrderListResponseDTO> queryUserOrderList(@RequestBody QueryUserOrderListRequestDTO request);

    /** 查询秒杀建单结果（前端用 seckillToken 轮询，status=1 时返回 orderId） */
    @PostMapping("query_seckill_order")
    Response<QuerySeckillOrderResponseDTO> querySeckillOrder(@RequestBody QuerySeckillOrderRequestDTO request);

    /** 按 outTradeNo 查询订单，用于服务间超时确认和幂等补偿 */
    @PostMapping("query_order_by_out_trade_no")
    Response<CreateOrderResponseDTO> queryOrderByOutTradeNo(@RequestBody QueryOrderByOutTradeNoRequestDTO request);
}
