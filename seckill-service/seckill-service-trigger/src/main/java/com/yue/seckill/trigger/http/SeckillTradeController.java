package com.yue.seckill.trigger.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yue.seckill.api.ISeckillTradeController;
import com.yue.seckill.api.dto.CreateSeckillOrderRequestDTO;
import com.yue.seckill.api.dto.CreateSeckillOrderResponseDTO;
import com.yue.seckill.api.response.Response;
import com.yue.seckill.domain.trade.service.ISeckillTradeService;
import com.yue.seckill.types.enums.ResponseCode;
import com.yue.seckill.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 秒杀服务控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/seckill/trade")
public class SeckillTradeController implements ISeckillTradeController {

    @Resource
    private ISeckillTradeService seckillTradeService;

    @Override
    public Response<CreateSeckillOrderResponseDTO> createPayOrder(CreateSeckillOrderRequestDTO request) {
        try {
            log.info("秒杀下单请求 userId:{} productId:{} activityId:{}",
                    request.getUserId(), request.getProductId(), request.getActivityId());

            if (StringUtils.isAnyBlank(request.getUserId(), request.getProductId())) {
                return Response.<CreateSeckillOrderResponseDTO>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("userId、productId 不能为空")
                        .build();
            }

            if (request.getActivityId() == null) {
                return Response.<CreateSeckillOrderResponseDTO>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("activityId 不能为空")
                        .build();
            }

            // 调用秒杀交易服务创建订单
            String seckillToken = seckillTradeService.createSeckillOrder(
                    request.getUserId(),
                    request.getProductId(),
                    request.getActivityId(),
                    request.getSource(),
                    request.getChannel(),
                    request.getGoodsName(),
                    request.getGoodsImageUrl(),
                    Boolean.TRUE.equals(request.getIsTest()));

            return Response.<CreateSeckillOrderResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(CreateSeckillOrderResponseDTO.builder()
                            .seckillToken(seckillToken)
                            .build())
                    .build();
        } catch (AppException e) {
            log.warn("秒杀下单业务异常 userId:{} e:{}", request.getUserId(), e.getMessage());
            return Response.<CreateSeckillOrderResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("秒杀下单系统异常 userId:{}", request.getUserId(), e);
            return Response.<CreateSeckillOrderResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @Override
    public Response<Boolean> refund(String body) {
        log.info("秒杀退款请求: {}", body);
        try {
            JSONObject json = JSON.parseObject(body);
            String userId = json.getString("userId");
            String orderId = json.getString("orderId");
            if (StringUtils.isAnyBlank(userId, orderId)) {
                return Response.<Boolean>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("userId、orderId 不能为空")
                        .build();
            }

            seckillTradeService.refund(userId, orderId);

            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(true)
                    .build();
        } catch (AppException e) {
            log.warn("秒杀退款业务异常 e:{}", e.getMessage());
            return Response.<Boolean>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .data(false)
                    .build();
        } catch (Exception e) {
            log.error("秒杀退款系统异常", e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(false)
                    .build();
        }
    }

}
