package com.yue.opsagent.springai.skill.redis;

import java.util.Arrays;
import java.util.stream.Collectors;

enum RedisToolDocumentation {

    redis_info("""
            section (string 可选)。
            对应 INFO 命令，可指定 section。"""),

    redis_slowlog("""
            count (number 可选，默认 32)。
            SLOWLOG GET 采样。"""),

    redis_client_list("""
            无参（args 可为 {}）。
            CLIENT LIST。"""),

    redis_memory("""
            无参（args 可为 {}）。
            INFO memory / 内存相关信息。"""),

    redis_get("""
            key (string 可选)；或使用 scanPattern + scanCount 做受控 SCAN 采样（scanCount 默认 50）。
            读单键或前缀扫描，勿大范围全库扫描。""");

    private final String usage;

    RedisToolDocumentation(String usage) {
        this.usage = usage.trim();
    }

    static String docFor(String toolId) {
        try {
            return valueOf(toolId).usage;
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    static String aggregatePromptFragment() {
        return Arrays.stream(values())
                .map(e -> "- " + e.name() + ":\n" + e.usage)
                .collect(Collectors.joining("\n\n"));
    }
}
