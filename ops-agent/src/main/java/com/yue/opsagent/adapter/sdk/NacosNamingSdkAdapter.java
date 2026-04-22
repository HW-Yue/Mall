package com.yue.opsagent.adapter.sdk;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.yue.opsagent.domain.port.NacosNamingPort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 出站适配器：Nacos Naming SDK 实现 Domain Port
 * 负责将 Domain 的通用调用翻译成 Nacos SDK 的具体调用
 */
@Component
public class NacosNamingSdkAdapter implements NacosNamingPort {

    private final NamingService namingService;

    public NacosNamingSdkAdapter(NamingService namingService) {
        this.namingService = namingService;
    }

    @Override
    public List<String> getInstances(String serviceName) {
        try {
            return namingService.getAllInstances(serviceName).stream()
                    .map(i -> i.getIp() + ":" + i.getPort())
                    .toList();
        } catch (NacosException e) {
            throw new RuntimeException("获取实例失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void registerInstance(String serviceName, String ip, int port) {
        try {
            namingService.registerInstance(serviceName, ip, port);
        } catch (NacosException e) {
            throw new RuntimeException("注册实例失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deregisterInstance(String serviceName, String ip, int port) {
        try {
            namingService.deregisterInstance(serviceName, ip, port);
        } catch (NacosException e) {
            throw new RuntimeException("注销实例失败: " + e.getMessage(), e);
        }
    }
}
