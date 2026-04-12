package cn.bugstack.ai.api.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 策略与客户端槽位（client_type）映射配置
 * 定义各执行策略所需的必选/可选 client_type，供前端二级联动与后端校验使用。
 *
 * @author bugstack.cn
 */
public final class AgentStrategyConfig {

    /** Flow 策略 Bean 名 */
    public static final String STRATEGY_FLOW = "flowAgentExecuteStrategy";
    /** Auto 策略 Bean 名 */
    public static final String STRATEGY_AUTO = "autoAgentExecuteStrategy";
    /** Fixed 策略 Bean 名 */
    public static final String STRATEGY_FIXED = "fixedAgentExecuteStrategy";

    /** FLOW 策略槽位：工具分析、规划、执行 */
    public static final List<String> FLOW_REQUIRED = List.of(
            "TOOL_MCP_CLIENT", "PLANNING_CLIENT", "EXECUTOR_CLIENT"
    );
    public static final List<String> FLOW_OPTIONAL = List.of();

    /** AUTO 策略槽位：分析、执行、监督、响应 */
    public static final List<String> AUTO_REQUIRED = List.of(
            "TASK_ANALYZER_CLIENT", "PRECISION_EXECUTOR_CLIENT",
            "QUALITY_SUPERVISOR_CLIENT", "RESPONSE_ASSISTANT"
    );
    public static final List<String> AUTO_OPTIONAL = List.of();

    /** FIXED 策略：不按 clientType 区分，按配置顺序执行；可选槽位为全部类型供下拉 */
    public static final List<String> FIXED_REQUIRED = List.of();
    public static final List<String> FIXED_OPTIONAL = List.of(
            "DEFAULT", "TASK_ANALYZER_CLIENT", "PRECISION_EXECUTOR_CLIENT",
            "QUALITY_SUPERVISOR_CLIENT", "RESPONSE_ASSISTANT",
            "TOOL_MCP_CLIENT", "PLANNING_CLIENT", "EXECUTOR_CLIENT"
    );

    private static final Map<String, SlotDef> STRATEGY_SLOTS = Map.of(
            STRATEGY_FLOW, new SlotDef(STRATEGY_FLOW, FLOW_REQUIRED, FLOW_OPTIONAL),
            STRATEGY_AUTO, new SlotDef(STRATEGY_AUTO, AUTO_REQUIRED, AUTO_OPTIONAL),
            STRATEGY_FIXED, new SlotDef(STRATEGY_FIXED, FIXED_REQUIRED, FIXED_OPTIONAL)
    );

    /**
     * 根据策略返回该策略的必选/可选 client_type 列表；未知策略返回空列表。
     */
    public static SlotDef getSlotsByStrategy(String strategy) {
        return Optional.ofNullable(STRATEGY_SLOTS.get(strategy))
                .orElse(new SlotDef(strategy, Collections.emptyList(), Collections.emptyList()));
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotDef {
        private String strategy;
        private List<String> requiredSlots;
        private List<String> optionalSlots;
    }
}
