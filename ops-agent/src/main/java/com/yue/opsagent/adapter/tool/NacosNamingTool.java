package com.yue.opsagent.adapter.tool;

import com.yue.opsagent.domain.port.NacosNamingPort;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 出站适配器：面向 AgentScope 的 Tool 层
 * 只负责 @Tool 注解和参数转换，业务逻辑全部委托给 Port
 * 不直接依赖 Nacos SDK！
 */
@Component
public class NacosNamingTool {

    private final NacosNamingPort namingPort;

    public NacosNamingTool(NacosNamingPort namingPort) {
        this.namingPort = namingPort;
    }

    @Tool(description = "获取服务实例列表")
    public String getInstances(
            @ToolParam(name = "serviceName", description = "服务名") String serviceName) {
        List<String> instances = namingPort.getInstances(serviceName);
        return instances.isEmpty() ? "暂无实例" : String.join(", ", instances);
    }

    @Tool(description = "注册服务实例")
    public String registerInstance(
            @ToolParam(name = "serviceName", description = "服务名") String serviceName,
            @ToolParam(name = "ip", description = "实例 IP") String ip,
            @ToolParam(name = "port", description = "实例端口") int port) {
        namingPort.registerInstance(serviceName, ip, port);
        return "注册成功: " + ip + ":" + port;
    }

    @Tool(description = "注销服务实例")
    public String deregisterInstance(
            @ToolParam(name = "serviceName", description = "服务名") String serviceName,
            @ToolParam(name = "ip", description = "实例 IP") String ip,
            @ToolParam(name = "port", description = "实例端口") int port) {
        namingPort.deregisterInstance(serviceName, ip, port);
        return "注销成功: " + ip + ":" + port;
    }
}
