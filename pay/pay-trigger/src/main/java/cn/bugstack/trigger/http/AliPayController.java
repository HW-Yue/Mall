package cn.bugstack.trigger.http;

import cn.bugstack.api.IPayService;
import cn.bugstack.api.dto.CreatePayRequestDTO;
import cn.bugstack.api.response.Response;
import cn.bugstack.domain.order.model.entity.CreateOrderEntity;
import cn.bugstack.domain.order.model.entity.OrderEntity;
import cn.bugstack.domain.order.model.entity.PayOrderEntity;
import cn.bugstack.domain.order.model.valobj.MarketTypeVO;
import cn.bugstack.domain.order.service.IOrderService;
import cn.bugstack.infrastructure.adapter.port.PaySuccessRocketMqPort;
import cn.bugstack.types.common.Constants;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeQueryRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController()
@CrossOrigin("*")
@RequestMapping("/api/v1/alipay/")
public class AliPayController implements IPayService {

    @Value("${alipay.alipay_public_key}")
    private String alipayPublicKey;

    @Resource
    private IOrderService orderService;

    @Resource
    private PaySuccessRocketMqPort paySuccessRocketMqPort;
    
    @Resource
    private AlipayClient alipayClient;

    /**
     * http://100.86.250.112:8080/api/v1/alipay/create_pay_order
     * <p>
     * {
     * "userId": "10001",
     * "productId": "100001"
     * }
     */
    @RequestMapping(value = "create_pay_order", method = RequestMethod.POST)
    @Override
    public Response<String> createPayOrder(@RequestBody CreatePayRequestDTO createPayRequestDTO) {
        String userId = createPayRequestDTO.getUserId();
        String productId = createPayRequestDTO.getProductId();
        BigDecimal deductionPrice = createPayRequestDTO.getDeductionPrice();
        BigDecimal payPrice = createPayRequestDTO.getPayPrice();
        BigDecimal originalprice = createPayRequestDTO.getOriginalPrice();
        String productName = createPayRequestDTO.getProductName();
        String orderId=createPayRequestDTO.getOutTradeNo();
        String marketType=createPayRequestDTO.getMarketType();


        try {
            log.info("商品下单，根据商品ID创建支付单开始 userId:{} productId:{}", userId, productId);
            String teamId = createPayRequestDTO.getTeamId();

            CreateOrderEntity createPayOrder=CreateOrderEntity.builder()
                    .originalPrice(originalprice)
                    .deductionPrice(deductionPrice)
                    .payPrice(payPrice)
                    .orderId(orderId)
                    .teamId(teamId)
                    .productId(productId)
                    .marketTypeVO(MarketTypeVO.fromCode(marketType))
                    .userId(userId)
                    .productName(productName)
                    .build();
            //下单
            PayOrderEntity payOrderEntity=orderService.createOrder(createPayOrder);

            log.info("商品下单，根据商品ID创建支付单完成 userId:{} productId:{} orderId:{}", userId, productId, payOrderEntity.getOrderId());
            return Response.<String>builder()
                    .code(Constants.ResponseCode.SUCCESS.getCode())
                    .info(Constants.ResponseCode.SUCCESS.getInfo())
                    .data(payOrderEntity.getPayUrl())
                    .build();
        } catch (Exception e) {
            log.error("商品下单，根据商品ID创建支付单失败 userId:{} productId:{}", createPayRequestDTO.getUserId(), createPayRequestDTO.getUserId(), e);
            return Response.<String>builder()
                    .code(Constants.ResponseCode.UN_ERROR.getCode())
                    .info(Constants.ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }



    /**
     * http://xfg-studio.natapp1.cc/api/v1/alipay/alipay_notify_url
     */
    @RequestMapping(value = "alipay_notify_url", method = RequestMethod.POST)
    public String payNotify(HttpServletRequest request) throws AlipayApiException, ParseException {
        log.info("支付回调，消息接收 {}", request.getParameter("trade_status"));

        if (!request.getParameter("trade_status").equals("TRADE_SUCCESS")) {
            return "false";
        }

        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            params.put(name, request.getParameter(name));
        }

        String tradeNo = params.get("out_trade_no");

        String sign = params.get("sign");
        String content = AlipaySignature.getSignCheckContentV1(params);
        boolean checkSignature = AlipaySignature.rsa256CheckContent(content, sign, alipayPublicKey, "UTF-8"); // 验证签名
        // 支付宝验签
        if (!checkSignature) {
            return "false";
        }

        // 验签通过
        log.info("支付回调，交易名称: {}", params.get("subject"));
        log.info("支付回调，交易状态: {}", params.get("trade_status"));
        log.info("支付回调，支付宝交易凭证号: {}", params.get("trade_no"));
        log.info("支付回调，商户订单号: {}", params.get("out_trade_no"));
        log.info("支付回调，交易金额: {}", params.get("total_amount"));
        log.info("支付回调，买家在支付宝唯一id: {}", params.get("buyer_id"));
        log.info("支付回调，买家付款时间: {}", params.get("gmt_payment"));
        log.info("支付回调，买家付款金额: {}", params.get("buyer_pay_amount"));
        log.info("支付回调，支付回调，更新订单 {}", tradeNo);

        Date payTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(params.get("gmt_payment"));

        // 支付宝异步通知为 form 参数，与验签所用 params 一致；发往 RocketMQ 供下游消费（替代原 HTTP/Retrofit 通知）
        OrderEntity orderEntity = orderService.queryOrderByOrderId(tradeNo);
        String userId = orderEntity != null ? orderEntity.getUserId() : null;
        String marketType = orderEntity != null ? orderEntity.getMarketType() : null;

        // 处理关单后收到付款的情况：先改状态，再退款，再通知订单服务
        if (orderEntity != null && "CLOSE".equals(orderEntity.getOrderStatusVO() != null ? orderEntity.getOrderStatusVO().getCode() : null)) {
            orderService.changeOrderPayAfterClose(tradeNo);
            boolean refundStatus = orderService.refundPayOrder(userId, tradeNo);
            log.info("支付回调，订单已关单，执行退款 orderId:{} refundStatus:{}", tradeNo, refundStatus);
            if (refundStatus) {
                paySuccessRocketMqPort.sendPayRefundMessage(userId, tradeNo, marketType);
            }
            return "success";
        }

        // 幂等：如果已经是关单后付款状态，直接返回成功
        if (orderEntity != null && "PAY_AFTER_CLOSE".equals(orderEntity.getOrderStatusVO() != null ? orderEntity.getOrderStatusVO().getCode() : null)) {
            log.info("支付回调，订单已是关单后付款状态，跳过处理 orderId:{}", tradeNo);
            return "success";
        }

        orderService.changeOrderPaySuccess(tradeNo, payTime);
        paySuccessRocketMqPort.sendSettlementMessage(userId, tradeNo, payTime, marketType);

        return "success";
    }


    /**
     * 测试回调接口 - 主动查询支付宝交易状态
     * @param outTradeNo 商户订单号
     * @return 处理结果
     */
    @RequestMapping(value = "active_pay_notify", method = RequestMethod.POST)
    public Response<String> activePayNotify(@RequestParam String outTradeNo) {
        try {
            log.info("测试回调接口，开始查询订单: {}", outTradeNo);
            
            // 构建支付宝交易查询请求
            AlipayTradeQueryModel bizModel = new AlipayTradeQueryModel();
            bizModel.setOutTradeNo(outTradeNo);
            
            AlipayTradeQueryRequest queryRequest = new AlipayTradeQueryRequest();
            queryRequest.setBizModel(bizModel);
            
            // 调用支付宝API查询交易状态
            String body = alipayClient.execute(queryRequest).getBody();
            log.info("支付宝查询结果: {}", body);
            
            // 解析查询结果
            JSONObject responseJson = JSON.parseObject(body);
            JSONObject queryResponse = responseJson.getJSONObject("alipay_trade_query_response");
            
            if (queryResponse != null && "10000".equals(queryResponse.getString("code"))) {
                String tradeStatus = queryResponse.getString("trade_status");
                String tradeNo = queryResponse.getString("trade_no");
                String totalAmount = queryResponse.getString("total_amount");
                String gmtPayment = queryResponse.getString("send_pay_date");
                
                log.info("查询成功 - 交易状态: {}, 支付宝交易号: {}, 金额: {}, 支付时间: {}", 
                        tradeStatus, tradeNo, totalAmount, gmtPayment);
                
                // 如果交易成功，执行后续流程处理
                if ("TRADE_SUCCESS".equals(tradeStatus)) {
                    log.info("交易成功，开始处理后续流程，订单号: {}", outTradeNo);
                    
                    // 调用订单服务更新订单状态
                    orderService.changeOrderPaySuccess(outTradeNo,
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(gmtPayment));

                    log.info("订单状态更新成功，订单号: {}", outTradeNo);
                    
                    return Response.<String>builder()
                            .code(Constants.ResponseCode.SUCCESS.getCode())
                            .info(Constants.ResponseCode.SUCCESS.getInfo())
                            .data("交易成功，订单状态已更新")
                            .build();
                } else {
                    log.info("交易状态非成功状态: {}, 订单号: {}", tradeStatus, outTradeNo);
                    return Response.<String>builder()
                            .code(Constants.ResponseCode.SUCCESS.getCode())
                            .info(Constants.ResponseCode.SUCCESS.getInfo())
                            .data("交易状态: " + tradeStatus)
                            .build();
                }
            } else {
                String errorMsg = queryResponse != null ? queryResponse.getString("msg") : "查询失败";
                log.error("支付宝查询失败: {}, 订单号: {}", errorMsg, outTradeNo);
                return Response.<String>builder()
                        .code(Constants.ResponseCode.UN_ERROR.getCode())
                        .info(Constants.ResponseCode.UN_ERROR.getInfo())
                        .data("查询失败: " + errorMsg)
                        .build();
            }
            
        } catch (Exception e) {
            log.error("测试回调接口异常，订单号: {}", outTradeNo, e);
            return Response.<String>builder()
                    .code(Constants.ResponseCode.UN_ERROR.getCode())
                    .info(Constants.ResponseCode.UN_ERROR.getInfo())
                    .data("系统异常: " + e.getMessage())
                    .build();
        }
    }

}
