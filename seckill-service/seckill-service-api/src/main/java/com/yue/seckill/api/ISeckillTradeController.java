package com.yue.seckill.api;

import com.yue.seckill.api.dto.CreateSeckillOrderRequestDTO;
import com.yue.seckill.api.dto.CreateSeckillOrderResponseDTO;
import com.yue.seckill.api.response.Response;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/seckill/trade/")
public interface ISeckillTradeController {

    /**
     * 秒杀下单（TODO: 库存锁定逻辑待实现）
     */
    @PostMapping("create_pay_order")
    Response<CreateSeckillOrderResponseDTO> createPayOrder(@RequestBody CreateSeckillOrderRequestDTO request);

    /**
     * 秒杀退款（TODO: 库存归还逻辑待实现）
     */
    @PostMapping("refund")
    Response<Boolean> refund(@RequestBody String body);
}
