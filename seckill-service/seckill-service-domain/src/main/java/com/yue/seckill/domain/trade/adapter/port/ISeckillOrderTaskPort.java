package com.yue.seckill.domain.trade.adapter.port;

import com.yue.seckill.types.model.SeckillOrderTaskMessage;

/**
 * 秒杀下单任务消息端口
 */
public interface ISeckillOrderTaskPort {

    void sendCreateOrderTask(SeckillOrderTaskMessage message);

}
