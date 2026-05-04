package com.yue.order.domain.order.model.valobj;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * 订单（order-service.t_order）状态机。
 *
 * <p>合法转移：
 * <pre>
 *   LOCK         -- PAID            -->   PAY_SUCCESS
 *   LOCK         -- CLOSE           -->   CLOSE        (超时 + 用户主动取消 共用)
 *   PAY_SUCCESS  -- REFUND_REQUEST  -->   WAIT_REFUND
 *   PAY_SUCCESS  -- SHIP            -->   WAIT_SHIP
 *   WAIT_REFUND  -- REFUND_COMPLETE -->   REFUNDED
 *   WAIT_SHIP    -- SHIP_DONE       -->   SHIPPED
 *   SHIPPED      -- DELIVER         -->   DELIVERED
 * </pre>
 *
 * 软校验模式：非法转移仅 log warn 返回 null，不抛异常；调用方按需处理。
 */
@Slf4j
public final class OrderStateMachine {

    private static final Map<OrderStatusVO, Map<OrderEvent, OrderStatusVO>> TRANSITIONS = build();

    private OrderStateMachine() {
    }

    private static Map<OrderStatusVO, Map<OrderEvent, OrderStatusVO>> build() {
        Map<OrderStatusVO, Map<OrderEvent, OrderStatusVO>> map = new EnumMap<>(OrderStatusVO.class);
        put(map, OrderStatusVO.LOCK,        OrderEvent.PAID,            OrderStatusVO.PAY_SUCCESS);
        put(map, OrderStatusVO.LOCK,        OrderEvent.CLOSE,           OrderStatusVO.CLOSE);
        put(map, OrderStatusVO.PAY_SUCCESS, OrderEvent.REFUND_REQUEST,  OrderStatusVO.WAIT_REFUND);
        put(map, OrderStatusVO.PAY_SUCCESS, OrderEvent.SHIP,            OrderStatusVO.WAIT_SHIP);
        put(map, OrderStatusVO.WAIT_REFUND, OrderEvent.REFUND_COMPLETE, OrderStatusVO.REFUNDED);
        put(map, OrderStatusVO.WAIT_SHIP,   OrderEvent.SHIP_DONE,       OrderStatusVO.SHIPPED);
        put(map, OrderStatusVO.SHIPPED,     OrderEvent.DELIVER,         OrderStatusVO.DELIVERED);
        return map;
    }

    private static void put(Map<OrderStatusVO, Map<OrderEvent, OrderStatusVO>> map,
                            OrderStatusVO from, OrderEvent event, OrderStatusVO to) {
        map.computeIfAbsent(from, k -> new EnumMap<>(OrderEvent.class)).put(event, to);
    }

    /**
     * 软校验：合法→返回目标状态；非法→log warn 返回 null。
     */
    public static OrderStatusVO next(OrderStatusVO current, OrderEvent event) {
        OrderStatusVO target = TRANSITIONS.getOrDefault(current, Collections.emptyMap()).get(event);
        if (target == null) {
            log.warn("[OrderStateMachine] 非法转移 current:{} event:{}（软校验模式，仅记录）", current, event);
        }
        return target;
    }

    public static boolean canTransition(OrderStatusVO current, OrderEvent event) {
        return TRANSITIONS.getOrDefault(current, Collections.emptyMap()).containsKey(event);
    }

    public static Set<OrderEvent> validEvents(OrderStatusVO current) {
        return TRANSITIONS.getOrDefault(current, Collections.emptyMap()).keySet();
    }

}
