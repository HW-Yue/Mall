package com.yue.groupbuy.trigger.http;

import com.yue.groupbuy.api.IGroupBuyTradeController;
import com.yue.groupbuy.api.dto.CreateGroupBuyOrderRequestDTO;
import com.yue.groupbuy.api.dto.CreateGroupBuyOrderResponseDTO;
import com.yue.groupbuy.api.dto.GroupBuyRefundRequestDTO;
import com.yue.groupbuy.api.response.Response;
import com.yue.groupbuy.domain.trade.model.entity.LockOrderCommand;
import com.yue.groupbuy.domain.trade.model.entity.MarketPayOrderEntity;
import com.yue.groupbuy.domain.trade.service.IGroupBuyDomainService;
import com.yue.groupbuy.types.enums.ResponseCode;
import com.yue.groupbuy.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/api/v1/group-buy/trade/")
public class GroupBuyTradeController implements IGroupBuyTradeController {

    @Resource
    private IGroupBuyDomainService groupBuyDomainService;

    @Override
    public Response<CreateGroupBuyOrderResponseDTO> createPayOrder(CreateGroupBuyOrderRequestDTO request) {
        try {
            log.info("拼团下单请求 userId:{} activityId:{} teamId:{}",
                    request.getUserId(), request.getActivityId(), request.getTeamId());

            if (StringUtils.isAnyBlank(request.getUserId(), request.getProductId())
                    || request.getActivityId() == null) {
                return Response.<CreateGroupBuyOrderResponseDTO>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("userId、productId、activityId 不能为空")
                        .build();
            }

            LockOrderCommand command = LockOrderCommand.builder()
                    .userId(request.getUserId())
                    .productId(request.getProductId())
                    .teamId(request.getTeamId())
                    .activityId(request.getActivityId())
                    .source(request.getSource())
                    .channel(request.getChannel())
                    .build();

            MarketPayOrderEntity result = groupBuyDomainService.createGroupBuyOrder(command);

            return Response.<CreateGroupBuyOrderResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(CreateGroupBuyOrderResponseDTO.builder()
                            .orderId(result.getOrderId())
                            .teamId(result.getTeamId())
                            .originalPrice(result.getOriginalPrice())
                            .deductionPrice(result.getDeductionPrice())
                            .payPrice(result.getPayPrice())
                            .build())
                    .build();
        } catch (AppException e) {
            log.warn("拼团下单业务异常 userId:{} code:{} info:{}", request.getUserId(), e.getCode(), e.getInfo());
            return Response.<CreateGroupBuyOrderResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("拼团下单系统异常 userId:{}", request.getUserId(), e);
            return Response.<CreateGroupBuyOrderResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @Override
    public Response<Boolean> refund(GroupBuyRefundRequestDTO request) {
        try {
            log.info("拼团退款请求 userId:{} orderId:{}", request.getUserId(), request.getOrderId());

            if (StringUtils.isAnyBlank(request.getUserId(), request.getOrderId())) {
                return Response.<Boolean>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("userId、orderId 不能为空")
                        .build();
            }

            groupBuyDomainService.refundGroupBuyOrder(request.getUserId(), request.getOrderId());

            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(true)
                    .build();
        } catch (AppException e) {
            log.warn("拼团退款业务异常 userId:{} code:{} info:{}", request.getUserId(), e.getCode(), e.getInfo());
            return Response.<Boolean>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .data(false)
                    .build();
        } catch (Exception e) {
            log.error("拼团退款系统异常 userId:{}", request.getUserId(), e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(false)
                    .build();
        }
    }
}
