package com.yue.groupbuy.domain.trade.model.valobj;

import com.yue.groupbuy.types.enums.GroupBuyOrderEnumVO;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * 拼团团队聚合状态机（team_order.status）。
 *
 * <p>合法转移（基于 {@link GroupBuyOrderEnumVO}：PROGRESS=0, COMPLETE=1, FAIL=2, COMPLETE_FAIL=3）：
 * <pre>
 *   PROGRESS    -- FORMED          -->  COMPLETE
 *   PROGRESS    -- TIMEOUT         -->  FAIL
 *   COMPLETE    -- PARTIAL_REFUND  -->  COMPLETE_FAIL
 *   COMPLETE    -- FULL_REFUND     -->  FAIL
 * </pre>
 *
 * 软校验：非法转移仅 log warn，不抛异常。
 */
@Slf4j
public final class GroupBuyTeamStateMachine {

    private static final Map<GroupBuyOrderEnumVO, Map<GroupBuyTeamEvent, GroupBuyOrderEnumVO>> TRANSITIONS = build();

    private GroupBuyTeamStateMachine() {
    }

    private static Map<GroupBuyOrderEnumVO, Map<GroupBuyTeamEvent, GroupBuyOrderEnumVO>> build() {
        Map<GroupBuyOrderEnumVO, Map<GroupBuyTeamEvent, GroupBuyOrderEnumVO>> map = new EnumMap<>(GroupBuyOrderEnumVO.class);
        put(map, GroupBuyOrderEnumVO.PROGRESS, GroupBuyTeamEvent.FORMED,         GroupBuyOrderEnumVO.COMPLETE);
        put(map, GroupBuyOrderEnumVO.PROGRESS, GroupBuyTeamEvent.TIMEOUT,        GroupBuyOrderEnumVO.FAIL);
        put(map, GroupBuyOrderEnumVO.COMPLETE, GroupBuyTeamEvent.PARTIAL_REFUND, GroupBuyOrderEnumVO.COMPLETE_FAIL);
        put(map, GroupBuyOrderEnumVO.COMPLETE, GroupBuyTeamEvent.FULL_REFUND,    GroupBuyOrderEnumVO.FAIL);
        return map;
    }

    private static void put(Map<GroupBuyOrderEnumVO, Map<GroupBuyTeamEvent, GroupBuyOrderEnumVO>> map,
                            GroupBuyOrderEnumVO from, GroupBuyTeamEvent event, GroupBuyOrderEnumVO to) {
        map.computeIfAbsent(from, k -> new EnumMap<>(GroupBuyTeamEvent.class)).put(event, to);
    }

    public static GroupBuyOrderEnumVO next(GroupBuyOrderEnumVO current, GroupBuyTeamEvent event) {
        GroupBuyOrderEnumVO target = TRANSITIONS.getOrDefault(current, Collections.emptyMap()).get(event);
        if (target == null) {
            log.warn("[GroupBuyTeamStateMachine] 非法转移 current:{} event:{}（软校验模式）", current, event);
        }
        return target;
    }

    public static boolean canTransition(GroupBuyOrderEnumVO current, GroupBuyTeamEvent event) {
        return TRANSITIONS.getOrDefault(current, Collections.emptyMap()).containsKey(event);
    }

    public static Set<GroupBuyTeamEvent> validEvents(GroupBuyOrderEnumVO current) {
        return TRANSITIONS.getOrDefault(current, Collections.emptyMap()).keySet();
    }

}
