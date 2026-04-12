package com.yue.groupbuy.api;

import com.yue.groupbuy.api.dto.CreateGroupBuyOrderRequestDTO;
import com.yue.groupbuy.api.dto.CreateGroupBuyOrderResponseDTO;
import com.yue.groupbuy.api.dto.GroupBuyRefundRequestDTO;
import com.yue.groupbuy.api.response.Response;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/group-buy/trade/")
public interface IGroupBuyTradeController {

    /**
     * 拼团下单（验证拼团逻辑 → 调 order-service 锁单 → 返回 orderId）
     */
    @PostMapping("create_pay_order")
    Response<CreateGroupBuyOrderResponseDTO> createPayOrder(@RequestBody CreateGroupBuyOrderRequestDTO request);

    /**
     * 拼团退款（验证退款规则 → 调 order-service 退款）
     */
    @PostMapping("refund")
    Response<Boolean> refund(@RequestBody GroupBuyRefundRequestDTO request);
}
