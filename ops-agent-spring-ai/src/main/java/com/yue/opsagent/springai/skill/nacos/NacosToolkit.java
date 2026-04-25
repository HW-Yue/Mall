package com.yue.opsagent.springai.skill.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.yue.opsagent.springai.config.OpsAiProperties;
import com.yue.opsagent.springai.skill.api.ToolResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Nacos ConfigService wrapper (get / publish).
 */
@Component
public class NacosToolkit {

    private final OpsAiProperties props;
    private final AtomicReference<ConfigService> cached = new AtomicReference<>();
    private final AtomicReference<NamingService> namingCached = new AtomicReference<>();

    public NacosToolkit(OpsAiProperties props) {
        this.props = props;
    }

    private ConfigService configService() throws NacosException {
        ConfigService existing = cached.get();
        if (existing != null) {
            return existing;
        }
        Properties p = nacosClientProperties();
        ConfigService created = NacosFactory.createConfigService(p);
        cached.compareAndSet(null, created);
        return cached.get();
    }

    private Properties nacosClientProperties() {
        var n = props.getNacos();
        Properties p = new Properties();
        p.setProperty("serverAddr", n.getServerAddr());
        if (n.getNamespace() != null && !n.getNamespace().isBlank()) {
            p.setProperty("namespace", n.getNamespace());
        }
        if (n.getUsername() != null && !n.getUsername().isBlank()) {
            p.setProperty("username", n.getUsername());
            p.setProperty("password", n.getPassword() == null ? "" : n.getPassword());
        }
        return p;
    }

    private NamingService namingService() throws NacosException {
        NamingService existing = namingCached.get();
        if (existing != null) {
            return existing;
        }
        NamingService created = NacosFactory.createNamingService(nacosClientProperties());
        namingCached.compareAndSet(null, created);
        return namingCached.get();
    }

    public ToolResult listInstances(String serviceName, String group) {
        if (serviceName == null || serviceName.isBlank()) {
            return ToolResult.error("serviceName 不能为空");
        }
        String g = (group == null || group.isBlank()) ? "DEFAULT_GROUP" : group;
        try {
            List<Instance> instances = namingService().selectInstances(serviceName, g, true);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Instance in : instances) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("ip", in.getIp());
                m.put("port", in.getPort());
                m.put("healthy", in.isHealthy());
                m.put("weight", in.getWeight());
                m.put("metadata", in.getMetadata());
                rows.add(m);
            }
            return ToolResult.ok("instances", rows);
        } catch (NacosException e) {
            return ToolResult.error("Nacos listInstances 失败: " + e.getMessage());
        }
    }

    public ToolResult listServices(int pageNo, int pageSize) {
        int pn = pageNo <= 0 ? 1 : pageNo;
        int ps = pageSize <= 0 ? 100 : Math.min(pageSize, 500);
        try {
            ListView<String> view = namingService().getServicesOfServer(pn, ps);
            return ToolResult.ok("services", Map.of(
                    "count", view.getCount(),
                    "data", view.getData()));
        } catch (NacosException e) {
            return ToolResult.error("Nacos listServices 失败: " + e.getMessage());
        }
    }

    public ToolResult getConfig(String dataId, String group) {
        if (dataId == null || dataId.isBlank()) {
            return ToolResult.error("dataId 不能为空");
        }
        String g = (group == null || group.isBlank()) ? "DEFAULT_GROUP" : group;
        try {
            String content = configService().getConfig(dataId, g, 5000);
            return ToolResult.ok("配置内容", content == null ? "" : content);
        } catch (NacosException e) {
            return ToolResult.error("Nacos getConfig 失败: " + e.getMessage());
        }
    }

    public ToolResult publishConfig(String dataId, String group, String content) {
        if (dataId == null || dataId.isBlank()) {
            return ToolResult.error("dataId 不能为空");
        }
        String g = (group == null || group.isBlank()) ? "DEFAULT_GROUP" : group;
        if (content == null) {
            content = "";
        }
        try {
            boolean ok = configService().publishConfig(dataId, g, content);
            return ok
                    ? ToolResult.ok("发布成功", Map.of("dataId", dataId, "group", g))
                    : ToolResult.error("publishConfig 返回 false");
        } catch (NacosException e) {
            return ToolResult.error("Nacos publishConfig 失败: " + e.getMessage());
        }
    }
}
