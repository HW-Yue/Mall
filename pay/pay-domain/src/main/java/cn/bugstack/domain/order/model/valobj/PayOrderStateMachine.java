package cn.bugstack.domain.order.model.valobj;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * 支付订单（pay.pay_order）状态机。
 *
 * <p>合法转移：
 * <pre>
 *   CREATE        -- CREATE_DONE      -->  PAY_WAIT
 *   PAY_WAIT      -- PAY_SUCCESS      -->  PAY_SUCCESS
 *   PAY_WAIT      -- CLOSE_TIMEOUT    -->  CLOSE
 *   CLOSE         -- LATE_PAYMENT     -->  PAY_AFTER_CLOSE
 *   PAY_SUCCESS   -- MARKETING_DONE   -->  MARKET
 *   PAY_SUCCESS   -- DEAL_DONE        -->  DEAL_DONE
 *   PAY_SUCCESS   -- REFUND_REQUEST   -->  WAIT_REFUND
 *   MARKET        -- REFUND_REQUEST   -->  WAIT_REFUND
 *   DEAL_DONE     -- REFUND_REQUEST   -->  WAIT_REFUND
 *   WAIT_REFUND   -- REFUND_COMPLETE  -->  REFUNDED
 * </pre>
 *
 * 软校验模式：非法转移仅 log warn 返回 null，不抛异常。
 */
@Slf4j
public final class PayOrderStateMachine {

    private static final Map<OrderStatusVO, Map<PayOrderEvent, OrderStatusVO>> TRANSITIONS = build();

    private PayOrderStateMachine() {
    }

    private static Map<OrderStatusVO, Map<PayOrderEvent, OrderStatusVO>> build() {
        Map<OrderStatusVO, Map<PayOrderEvent, OrderStatusVO>> map = new EnumMap<>(OrderStatusVO.class);
        put(map, OrderStatusVO.CREATE,      PayOrderEvent.CREATE_DONE,      OrderStatusVO.PAY_WAIT);
        put(map, OrderStatusVO.PAY_WAIT,    PayOrderEvent.PAY_SUCCESS,      OrderStatusVO.PAY_SUCCESS);
        put(map, OrderStatusVO.PAY_WAIT,    PayOrderEvent.CLOSE_TIMEOUT,    OrderStatusVO.CLOSE);
        put(map, OrderStatusVO.CLOSE,       PayOrderEvent.LATE_PAYMENT,     OrderStatusVO.PAY_AFTER_CLOSE);
        put(map, OrderStatusVO.PAY_SUCCESS, PayOrderEvent.MARKETING_DONE,   OrderStatusVO.MARKET);
        put(map, OrderStatusVO.PAY_SUCCESS, PayOrderEvent.DEAL_DONE,        OrderStatusVO.DEAL_DONE);
        put(map, OrderStatusVO.PAY_SUCCESS, PayOrderEvent.REFUND_REQUEST,   OrderStatusVO.WAIT_REFUND);
        put(map, OrderStatusVO.MARKET,      PayOrderEvent.REFUND_REQUEST,   OrderStatusVO.WAIT_REFUND);
        put(map, OrderStatusVO.DEAL_DONE,   PayOrderEvent.REFUND_REQUEST,   OrderStatusVO.WAIT_REFUND);
        put(map, OrderStatusVO.WAIT_REFUND, PayOrderEvent.REFUND_COMPLETE,  OrderStatusVO.REFUNDED);
        return map;
    }

    private static void put(Map<OrderStatusVO, Map<PayOrderEvent, OrderStatusVO>> map,
                            OrderStatusVO from, PayOrderEvent event, OrderStatusVO to) {
        map.computeIfAbsent(from, k -> new EnumMap<>(PayOrderEvent.class)).put(event, to);
    }

    /**
     * 软校验：合法→返回目标状态；非法→log warn 返回 null。
     */
    public static OrderStatusVO next(OrderStatusVO current, PayOrderEvent event) {
        OrderStatusVO target = TRANSITIONS.getOrDefault(current, Collections.emptyMap()).get(event);
        if (target == null) {
            log.warn("[PayOrderStateMachine] 非法转移 current:{} event:{}（软校验模式，仅记录）", current, event);
        }
        return target;
    }

    public static boolean canTransition(OrderStatusVO current, PayOrderEvent event) {
        return TRANSITIONS.getOrDefault(current, Collections.emptyMap()).containsKey(event);
    }

    public static Set<PayOrderEvent> validEvents(OrderStatusVO current) {
        return TRANSITIONS.getOrDefault(current, Collections.emptyMap()).keySet();
    }

}
