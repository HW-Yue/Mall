package com.yue.groupbuy.domain.trade.model.valobj;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * 拼团个人订单状态机（t_order.status）。
 *
 * <p>合法转移：
 * <pre>
 *   CREATE       -- PAID            -->  COMPLETE
 *   CREATE       -- CLOSE           -->  CLOSED
 *   COMPLETE     -- REFUND_REQUEST  -->  WAIT_REFUND
 *   WAIT_REFUND  -- REFUND_COMPLETE -->  REFUNDED
 * </pre>
 *
 * 软校验：非法转移仅 log warn，不抛异常。
 */
@Slf4j
public final class GroupBuyTradeOrderStateMachine {

    private static final Map<TradeOrderStatusEnumVO, Map<GroupBuyTradeOrderEvent, TradeOrderStatusEnumVO>> TRANSITIONS = build();

    private GroupBuyTradeOrderStateMachine() {
    }

    private static Map<TradeOrderStatusEnumVO, Map<GroupBuyTradeOrderEvent, TradeOrderStatusEnumVO>> build() {
        Map<TradeOrderStatusEnumVO, Map<GroupBuyTradeOrderEvent, TradeOrderStatusEnumVO>> map = new EnumMap<>(TradeOrderStatusEnumVO.class);
        put(map, TradeOrderStatusEnumVO.CREATE,      GroupBuyTradeOrderEvent.PAID,            TradeOrderStatusEnumVO.COMPLETE);
        put(map, TradeOrderStatusEnumVO.CREATE,      GroupBuyTradeOrderEvent.CLOSE,           TradeOrderStatusEnumVO.CLOSED);
        put(map, TradeOrderStatusEnumVO.COMPLETE,    GroupBuyTradeOrderEvent.REFUND_REQUEST,  TradeOrderStatusEnumVO.WAIT_REFUND);
        put(map, TradeOrderStatusEnumVO.WAIT_REFUND, GroupBuyTradeOrderEvent.REFUND_COMPLETE, TradeOrderStatusEnumVO.REFUNDED);
        return map;
    }

    private static void put(Map<TradeOrderStatusEnumVO, Map<GroupBuyTradeOrderEvent, TradeOrderStatusEnumVO>> map,
                            TradeOrderStatusEnumVO from, GroupBuyTradeOrderEvent event, TradeOrderStatusEnumVO to) {
        map.computeIfAbsent(from, k -> new EnumMap<>(GroupBuyTradeOrderEvent.class)).put(event, to);
    }

    public static TradeOrderStatusEnumVO next(TradeOrderStatusEnumVO current, GroupBuyTradeOrderEvent event) {
        TradeOrderStatusEnumVO target = TRANSITIONS.getOrDefault(current, Collections.emptyMap()).get(event);
        if (target == null) {
            log.warn("[GroupBuyTradeOrderStateMachine] 非法转移 current:{} event:{}（软校验模式）", current, event);
        }
        return target;
    }

    public static boolean canTransition(TradeOrderStatusEnumVO current, GroupBuyTradeOrderEvent event) {
        return TRANSITIONS.getOrDefault(current, Collections.emptyMap()).containsKey(event);
    }

    public static Set<GroupBuyTradeOrderEvent> validEvents(TradeOrderStatusEnumVO current) {
        return TRANSITIONS.getOrDefault(current, Collections.emptyMap()).keySet();
    }

}
