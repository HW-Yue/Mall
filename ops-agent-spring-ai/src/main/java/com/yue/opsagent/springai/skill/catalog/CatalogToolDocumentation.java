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
            注意：如果像“下单链路出问题了”这样过于模糊，先用 catalog_list_services 或 catalog_list_topics 缩小范围，再调用本工具。
            """),

    catalog_list_services("""
            用途：列出当前静态知识库里的标准服务名清单。
            args：无。
            返回：services（canonical service name 列表）和 count。
            典型用途：问题描述很模糊时，先看当前有哪些服务，再对候选服务调用 catalog_describe_service。
            """),

    catalog_list_topics("""
            用途：列出当前静态知识库里的 Topic 名清单。
            args：无。
            返回：topics 和 count。
            典型用途：需要先确认系统里有哪些 MQ Topic，再决定是否继续查 consumerGroup 或某个服务。
            """),

    catalog_describe_service("""
            用途：返回某个服务的静态拓扑。
            args：
            - service (string，必填)：标准服务名或别名；一次只允许传一个服务名
            返回：application、composeService、containerName、configEntries、configDataIds、aliases、resources、
            topicsProduced、topicsConsumed、consumerGroups、tables、databases、pools。
            注意：如果有多个候选服务，必须逐个调用，不能把 "order-service, pay-service" 这类列表整体传进来。
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
                - 描述很模糊、只知道是“某条链路有问题”时，先用 catalog_list_services 或 catalog_list_topics 看现有对象。
                - 线索里已经带 service/application/resource/topic/table/pool 时，再优先用 catalog_resolve_service。
                - 确认主服务后用 catalog_describe_service 拿 application、composeService、containerName、configEntries；如果有多个候选服务，要逐个单独查询。
                - 要查 Nacos 配置时，只能使用 catalog_describe_service 返回的 configEntries / configDataIds，不要猜 dataId 或 group。
                - 只知道某个 topic/table/pool 时用 catalog_lookup_resource_owner 反查。
                """;
    }
}
