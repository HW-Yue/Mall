package com.yue.opsagent.springai.skill.catalog;

import java.util.Arrays;
import java.util.stream.Collectors;

enum CatalogToolDocumentation {

    catalog_resolve_service("""
            用途：根据自由文本或单个线索解析标准服务名。
            args 可选键：
            - query (string)：自由文本，如“下单接口 order-service 超时”“nexus-order-service 死锁”
            - service/application/resource/topic/consumerGroup/table/database/pool (string)：结构化线索，任填其一或多个
            返回：primaryService、candidateServices、serviceProfile、serviceReason、evidence。
            """),

    catalog_describe_service("""
            用途：返回某个服务的静态拓扑。
            args：
            - service (string，必填)：标准服务名或别名
            返回：application、composeService、containerName、aliases、resources、topicsProduced、topicsConsumed、
            consumerGroups、tables、databases、pools。
            """),

    catalog_lookup_resource_owner("""
            用途：按单一资源键查询所属服务。
            args：
            - kind (string，必填)：resource | topic | consumerGroup | table | database | pool
            - value (string，必填)：对应资源值
            返回：对于单 owner 资源返回 ownerService；对于 topic / table / database 等可能返回多个服务。
            """);

    private final String usage;

    CatalogToolDocumentation(String usage) {
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
        String tools = Arrays.stream(values())
                .map(e -> "- " + e.name() + ":\n" + e.usage)
                .collect(Collectors.joining("\n\n"));
        return """
                工具（均为只读静态知识库查询）：

                """
                + tools
                + "\n\n"
                + """
                推荐顺序：
                - 服务不明确时先用 catalog_resolve_service。
                - 确认主服务后用 catalog_describe_service 拿 application、composeService、containerName。
                - 只知道某个 topic/table/pool 时用 catalog_lookup_resource_owner 反查。
                """;
    }
}
