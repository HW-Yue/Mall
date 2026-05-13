package com.yue.order.trigger.http;

import com.yue.order.api.IOrderController;
import com.yue.order.api.dto.CreateOrderRequestDTO;
import com.yue.order.api.dto.CreateOrderResponseDTO;
import com.yue.order.api.dto.GetPayUrlRequestDTO;
import com.yue.order.api.dto.QuerySeckillOrderRequestDTO;
import com.yue.order.api.dto.QuerySeckillOrderResponseDTO;
import com.yue.order.api.dto.QueryUserOrderListRequestDTO;
import com.yue.order.api.dto.QueryUserOrderListResponseDTO;
import com.yue.order.api.dto.RefundRequestDTO;
import com.yue.order.api.dto.UserOrderItemDTO;
import com.yue.order.api.response.Response;
import com.yue.order.domain.order.model.entity.OrderEntity;
import com.yue.order.domain.order.model.valobj.OrderStatusVO;
import com.yue.order.domain.order.model.entity.CreateOrderCommand;
import com.yue.order.domain.order.service.IOrderDomainService;
import com.yue.order.types.enums.ResponseCode;
import com.yue.order.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单服务 HTTP Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/order/")
public class OrderController implements IOrderController {

    private static final String TOKEN_STATUS_KEY_PREFIX = "seckill:token:status:";
    private static final String ORDER_RESULT_KEY_PREFIX = "seckill:order:";

    @Resource
    private IOrderDomainService orderDomainService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${app.order.internal-create-token:}")
    private String internalCreateToken;

    /**
     * 创建订单（锁单）
     * 普通商品：前端直接调
     * 拼团/秒杀：各自营销服务验证后调
     */
    @RequestMapping(value = "create_order", method = RequestMethod.POST)
    @Override
    public Response<CreateOrderResponseDTO> createOrder(@RequestBody CreateOrderRequestDTO request) {
        try {
            log.info("createOrder req:{}", request);
            if (StringUtils.isAnyBlank(request.getUserId(), request.getProductId(), request.getMarketType())) {
                return Response.error(ResponseCode.ILLEGAL_PARAMETER.getCode(), "userId / productId / marketType 不能为空");
            }
            if (request.getPayPrice() == null) {
                return Response.error(ResponseCode.ILLEGAL_PARAMETER.getCode(), "payPrice 不能为空");
            }

            CreateOrderCommand command = CreateOrderCommand.builder()
                    .userId(request.getUserId())
                    .goodsId(request.getProductId())
                    .marketType(request.getMarketType())
                    .originalPrice(request.getOriginalPrice())
                    .deductionPrice(request.getDeductionPrice())
                    .payPrice(request.getPayPrice())
                    .source(request.getSource())
                    .channel(request.getChannel())
                    .goodsName(request.getGoodsName())
                    .goodsImageUrl(request.getGoodsImageUrl())
                    .build();

            String orderId = orderDomainService.createOrder(command);
            CreateOrderResponseDTO data = CreateOrderResponseDTO.builder()
                    .orderId(orderId)
                    .build();
            return Response.success(data);
        } catch (AppException e) {
            log.warn("createOrder 业务异常 userId:{} e:{}", request.getUserId(), e.getMessage());
            return Response.error(e.getCode(), e.getInfo());
        } catch (Exception e) {
            log.error("createOrder 失败 userId:{}", request.getUserId(), e);
            return Response.error(ResponseCode.UN_ERROR.getCode(), ResponseCode.UN_ERROR.getInfo());
        }
    }

    @RequestMapping(value = "create_order_normal_from_mall", method = RequestMethod.POST)
    @Override
    public Response<CreateOrderResponseDTO> createOrderNormalFromMall(
            @RequestBody CreateOrderRequestDTO request,
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken) {
        try {
            if (StringUtils.isNotBlank(internalCreateToken)
                    && !internalCreateToken.equals(StringUtils.trimToEmpty(internalToken))) {
                return Response.error(ResponseCode.ILLEGAL_PARAMETER.getCode(), "无效的内部调用凭证");
            }
            log.info("createOrderNormalFromMall req:{}", request);
            if (StringUtils.isAnyBlank(request.getUserId(), request.getProductId())) {
                return Response.error(ResponseCode.ILLEGAL_PARAMETER.getCode(), "userId / productId 不能为空");
            }
            if (request.getPayPrice() == null) {
                return Response.error(ResponseCode.ILLEGAL_PARAMETER.getCode(), "payPrice 不能为空");
            }
            CreateOrderCommand command = CreateOrderCommand.builder()
                    .userId(request.getUserId())
                    .goodsId(request.getProductId())
                    .marketType("normal")
                    .originalPrice(request.getOriginalPrice())
                    .deductionPrice(request.getDeductionPrice())
                    .payPrice(request.getPayPrice())
                    .source(request.getSource())
                    .channel(request.getChannel())
                    .goodsName(request.getGoodsName())
                    .goodsImageUrl(request.getGoodsImageUrl())
                    .build();
            var result = orderDomainService.submitNormalOrderFromMall(command);
            CreateOrderResponseDTO data = CreateOrderResponseDTO.builder()
                    .orderId(result.getOrderId())
                    .build();
            return Response.success(data);
        } catch (AppException e) {
            log.warn("createOrderNormalFromMall 业务异常 userId:{} e:{}", request.getUserId(), e.getMessage());
            return Response.error(e.getCode(), e.getInfo());
        } catch (Exception e) {
            log.error("createOrderNormalFromMall 失败 userId:{}", request.getUserId(), e);
            return Response.error(ResponseCode.UN_ERROR.getCode(), ResponseCode.UN_ERROR.getInfo());
        }
    }

    /**
     * 获取支付 URL（用户在支付页点击"立即支付"时调用）
     */
    @RequestMapping(value = "get_pay_url", method = RequestMethod.POST)
    @Override
    public Response<String> getPayUrl(@RequestBody GetPayUrlRequestDTO request) {
        try {
            log.info("getPayUrl userId:{} orderId:{}", request.getUserId(), request.getOrderId());
            if (StringUtils.isAnyBlank(request.getUserId(), request.getOrderId())) {
                return Response.error(ResponseCode.ILLEGAL_PARAMETER.getCode(), "userId / orderId 不能为空");
            }
            String payUrl = orderDomainService.getPayUrl(request.getUserId(), request.getOrderId());
            return Response.success(payUrl);
        } catch (AppException e) {
            log.warn("getPayUrl 业务异常 userId:{} orderId:{} e:{}", request.getUserId(), request.getOrderId(), e.getMessage());
            return Response.error(e.getCode(), e.getInfo());
        } catch (Exception e) {
            log.error("getPayUrl 失败 userId:{} orderId:{}", request.getUserId(), request.getOrderId(), e);
            return Response.error(ResponseCode.UN_ERROR.getCode(), ResponseCode.UN_ERROR.getInfo());
        }
    }

    /**
     * 普通订单退款（前端直接调）
     */
    @RequestMapping(value = "refund", method = RequestMethod.POST)
    @Override
    public Response<Boolean> refund(@RequestBody RefundRequestDTO request) {
        try {
            log.info("refund userId:{} orderId:{}", request.getUserId(), request.getOrderId());
            if (StringUtils.isAnyBlank(request.getUserId(), request.getOrderId())) {
                return Response.error(ResponseCode.ILLEGAL_PARAMETER.getCode(), "userId / orderId 不能为空");
            }
            orderDomainService.refund(request.getUserId(), request.getOrderId());
            return Response.success(true);
        } catch (AppException e) {
            log.warn("refund 业务异常 userId:{} orderId:{} e:{}", request.getUserId(), request.getOrderId(), e.getMessage());
            return Response.error(e.getCode(), e.getInfo());
        } catch (Exception e) {
            log.error("refund 失败 userId:{} orderId:{}", request.getUserId(), request.getOrderId(), e);
            return Response.error(ResponseCode.UN_ERROR.getCode(), ResponseCode.UN_ERROR.getInfo());
        }
    }

    /**
     * 查询用户订单列表（游标分页）
     */
    @RequestMapping(value = "query_user_order_list", method = RequestMethod.POST)
    @Override
    public Response<QueryUserOrderListResponseDTO> queryUserOrderList(
            @RequestBody QueryUserOrderListRequestDTO request) {
        try {
            if (StringUtils.isBlank(request.getUserId())) {
                return Response.error(ResponseCode.ILLEGAL_PARAMETER.getCode(), "userId 不能为空");
            }
            int pageSize = (request.getPageSize() == null || request.getPageSize() < 1)
                    ? 10 : Math.min(request.getPageSize(), 50);

            List<OrderEntity> orders = orderDomainService.queryUserOrderList(
                    request.getUserId(), request.getLastId(), pageSize + 1);

            boolean hasMore = orders.size() > pageSize;
            List<OrderEntity> page = hasMore ? orders.subList(0, pageSize) : orders;

            List<UserOrderItemDTO> list = new ArrayList<>(page.size());
            for (OrderEntity o : page) {
                list.add(UserOrderItemDTO.builder()
                        .orderId(o.getOrderId())
                        .status(toStatusCode(o))
                        .productName(o.getGoodsName() != null ? o.getGoodsName() : o.getGoodsId())
                        .orderTime(o.getCreateTime())
                        .totalAmount(o.getOriginalPrice())
                        .payAmount(o.getPayPrice())
                        .marketType(o.getMarketType() != null ? o.getMarketType().getCode() : null)
                        .build());
            }

            Long newLastId = page.isEmpty() ? null : page.get(page.size() - 1).getId();
            return Response.success(QueryUserOrderListResponseDTO.builder()
                    .orderList(list).hasMore(hasMore).lastId(newLastId).build());
        } catch (Exception e) {
            log.error("queryUserOrderList 失败 userId:{}", request.getUserId(), e);
            return Response.error(ResponseCode.UN_ERROR.getCode(), ResponseCode.UN_ERROR.getInfo());
        }
    }

    /**
     * 查询秒杀建单结果（前端轮询）
     * seckillToken 由 seckill-service 颁发，MQ 异步建单后 status 变为 1
     */
    @RequestMapping(value = "query_seckill_order", method = RequestMethod.POST)
    @Override
    public Response<QuerySeckillOrderResponseDTO> querySeckillOrder(@RequestBody QuerySeckillOrderRequestDTO request) {
        try {
            if (StringUtils.isBlank(request.getSeckillToken())) {
                return Response.error(ResponseCode.ILLEGAL_PARAMETER.getCode(), "seckillToken 不能为空");
            }
            String token = request.getSeckillToken();
            String status = stringRedisTemplate.opsForValue().get(TOKEN_STATUS_KEY_PREFIX + token);
            if ("1".equals(status)) {
                String orderId = stringRedisTemplate.opsForValue().get(ORDER_RESULT_KEY_PREFIX + token);
                return Response.success(QuerySeckillOrderResponseDTO.builder().status(1).orderId(orderId).build());
            }
            return Response.success(QuerySeckillOrderResponseDTO.builder().status(0).build());
        } catch (Exception e) {
            log.error("querySeckillOrder 失败 token:{}", request.getSeckillToken(), e);
            return Response.error(ResponseCode.UN_ERROR.getCode(), ResponseCode.UN_ERROR.getInfo());
        }
    }

    private String toStatusCode(OrderEntity o) {
        if (o.getStatus() == null) return "UNKNOWN";
        switch (o.getStatus()) {
            case LOCK: return "PAY_WAIT";
            case PAY_SUCCESS: return "PAY_SUCCESS";
            case CLOSE: return "CLOSE";
            case WAIT_REFUND: return "WAIT_REFUND";
            case REFUNDED: return "REFUNDED";
            case WAIT_SHIP: return "WAIT_SHIP";
            case SHIPPED: return "SHIPPED";
            case DELIVERED: return "DELIVERED";
            default: return o.getStatus().getCode();
        }
    }

    /**
     * 营销订单退款执行（拼团/秒杀服务调用，跳过业务校验）
     */
    @RequestMapping(value = "refund_execute", method = RequestMethod.POST)
    @Override
    public Response<Boolean> refundExecute(@RequestBody RefundRequestDTO request) {
        try {
            log.info("refundExecute userId:{} orderId:{}", request.getUserId(), request.getOrderId());
            if (StringUtils.isAnyBlank(request.getUserId(), request.getOrderId())) {
                return Response.error(ResponseCode.ILLEGAL_PARAMETER.getCode(), "userId / orderId 不能为空");
            }
            orderDomainService.refundExecute(request.getUserId(), request.getOrderId());
            return Response.success(true);
        } catch (AppException e) {
            log.warn("refundExecute 业务异常 userId:{} orderId:{} e:{}", request.getUserId(), request.getOrderId(), e.getMessage());
            return Response.error(e.getCode(), e.getInfo());
        } catch (Exception e) {
            log.error("refundExecute 失败 userId:{} orderId:{}", request.getUserId(), request.getOrderId(), e);
            return Response.error(ResponseCode.UN_ERROR.getCode(), ResponseCode.UN_ERROR.getInfo());
        }
    }
}
