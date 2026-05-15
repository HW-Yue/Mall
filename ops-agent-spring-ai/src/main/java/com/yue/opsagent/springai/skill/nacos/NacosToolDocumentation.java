package com.yue.opsagent.springai.skill.nacos;

import java.util.Arrays;
import java.util.stream.Collectors;

enum NacosToolDocumentation {

    nacos_get_config("""
            读取配置。
            参数 JSON 键：
            - dataId (string, 必填)
            - group (string, 可选，默认 DEFAULT_GROUP)
            注意：dataId 只允许单个明确值，不支持 *, ?, 逗号拼接或猜测式输入；必须先从 Catalog 的 configEntries / configDataIds 获取。"""),

    nacos_publish_config("""
            发布配置（高危，可能触发审批挂起；需人工审批后才能真正写入）。
            参数 JSON 键：
            - dataId (string, 必填)
            - group (string, 可选)
            - content (string, 配置正文)"""),

    nacos_list_instances("""
            查询服务实例 / 健康列表。
            参数：serviceName (string 必填), group (string 可选)。
            空实例列表是关键证据：服务未注册、名称不一致或当前无健康实例。"""),

    nacos_list_services("""
            分页列出服务名（与 nacos_get_services 为别名，行为一致）。
            参数：pageNo (number 可选), pageSize (number 可选)。"""),

    nacos_get_services("""
            分页列出服务名（与 nacos_list_services 为别名，行为一致）。
            参数：pageNo (number 可选), pageSize (number 可选)。""");

    private final String usage;

    NacosToolDocumentation(String usage) {
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
                工具：

                """
                + tools
                + "\n\n"
                + """
                服务存在性优先：
                - 排查服务错误率/服务不可用时，先用 nacos_list_instances 查询告警 application/serviceName。
                - 若实例列表为空或无健康实例，直接报告“服务未注册或无健康实例”，不要继续读无关配置。
                - 只有确认服务存在且问题指向配置时，才使用 nacos_get_config；dataId/group 必须来自 Catalog 的 configEntries，不要猜服务名、application 名或 *。
                - nacos_publish_config 必须等待人工审批。
                """;
    }
}
