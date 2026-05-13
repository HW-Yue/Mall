package com.yue.opsagent.springai.skill.redis;

import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;
import com.yue.opsagent.springai.skill.support.SkillToolHelp;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class RedisSkillRegistry implements OpsSkillRegistry {

    public static final String SKILL_NAME = "redis_inspect";

    private static final Set<String> DATA_TOOLS =
            Set.of("redis_info", "redis_slowlog", "redis_client_list", "redis_memory", "redis_get");

    private final RedisToolkit toolkit;

    public RedisSkillRegistry(RedisToolkit toolkit) {
        this.toolkit = toolkit;
    }

    @Override
    public String name() {
        return SKILL_NAME;
    }

    @Override
    public String description() {
        return "Redis 只读：INFO、慢日志、客户端、内存、GET/SCAN";
    }

    @Override
    public String promptFragment() {
        return RedisToolDocumentation.aggregatePromptFragment();
    }

    @Override
    public String toolMenuBrief() {
        return """
                - redis_info: INFO
                - redis_slowlog: SLOWLOG GET
                - redis_client_list: CLIENT LIST
                - redis_memory: INFO memory
                - redis_get: GET 或 SCAN 采样
                """;
    }

    @Override
    public Set<String> toolNames() {
        return SkillToolHelp.toolNamesWithHelp(DATA_TOOLS, this);
    }

    @Override
    public String documentationForDataTool(String dataToolName) {
        return RedisToolDocumentation.docFor(dataToolName);
    }

    @Override
    public ToolResult execute(String toolName, Map<String, Object> args) {
        ToolResult help = SkillToolHelp.tryExecute(this, toolName, args);
        if (help != null) {
            return help;
        }
        Map<String, Object> a = args == null ? Map.of() : args;
        return switch (toolName) {
            case "redis_info" -> toolkit.info(str(a, "section"));
            case "redis_slowlog" -> toolkit.slowlog(intArg(a, "count", 32));
            case "redis_client_list" -> toolkit.clientList();
            case "redis_memory" -> toolkit.memoryStats();
            case "redis_get" -> toolkit.getKey(str(a, "key"), str(a, "scanPattern"), intArg(a, "scanCount", 50));
            default -> ToolResult.error("unknown tool: " + toolName);
        };
    }

    private static String str(Map<String, Object> a, String key) {
        Object v = a.get(key);
        return v == null ? "" : String.valueOf(v);
    }

    private static int intArg(Map<String, Object> a, String key, int def) {
        Object v = a.get(key);
        if (v == null) {
            return def;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
