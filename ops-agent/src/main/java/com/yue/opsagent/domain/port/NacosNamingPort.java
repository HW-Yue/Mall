package com.yue.opsagent.domain.port;

import java.util.List;

/**
 * Domain Port: 服务发现能力抽象
 * 纯接口，不依赖任何框架
 */
public interface NacosNamingPort {

    List<String> getInstances(String serviceName);

    void registerInstance(String serviceName, String ip, int port);

    void deregisterInstance(String serviceName, String ip, int port);
}
