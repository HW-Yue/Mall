package cn.bugstack.api;

import cn.bugstack.api.dto.CreatePayRequestDTO;

/**
 * 支付服务 Dubbo RPC 接口（Triple 协议）
 * 供 order-service 调用创建支付单
 */
public interface IPayDubboService {

    /** 返回支付 URL；失败时抛异常 */
    String createPayOrder(CreatePayRequestDTO request);
}
