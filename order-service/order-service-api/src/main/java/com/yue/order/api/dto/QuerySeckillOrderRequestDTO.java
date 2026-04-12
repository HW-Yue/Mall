package com.yue.order.api.dto;

import lombok.Data;

/**
 * 秒杀建单结果查询请求（前端用 seckillToken 轮询）
 */
@Data
public class QuerySeckillOrderRequestDTO {

    private String seckillToken;

}
