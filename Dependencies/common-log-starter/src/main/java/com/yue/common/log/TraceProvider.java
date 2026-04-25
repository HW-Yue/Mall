package com.yue.common.log;

/**
 * 全链路追踪实现：自研 UUID/Header 或与 SkyWalking Agent 联动。
 */
public enum TraceProvider {

    /**
     * 自研：TraceIdServletFilter / Gateway / Feign 生成或透传 trace-id。
     */
    legacy,

    /**
     * SkyWalking：由 Java Agent 传播上下文；本 Starter 仅把 TraceContext 写入 MDC，不添加 Feign/网关的 trace-id 头。
     */
    skywalking
}
