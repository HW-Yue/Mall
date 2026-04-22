package com.yue.opsagent.domain.port;

/**
 * Domain Port: 配置中心能力抽象
 * 纯接口，不依赖任何框架
 */
public interface NacosConfigPort {

    String getConfig(String dataId, String group);

    boolean publishConfig(String dataId, String group, String content);

    boolean deleteConfig(String dataId, String group);
}
