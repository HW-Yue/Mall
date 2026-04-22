package com.yue.opsagent.adapter.sdk;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.yue.opsagent.domain.port.NacosConfigPort;
import org.springframework.stereotype.Component;

/**
 * 出站适配器：Nacos Config SDK 实现 Domain Port
 * 负责将 Domain 的通用调用翻译成 Nacos SDK 的具体调用
 */
@Component
public class NacosConfigSdkAdapter implements NacosConfigPort {

    private final ConfigService configService;

    public NacosConfigSdkAdapter(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public String getConfig(String dataId, String group) {
        try {
            return configService.getConfig(dataId, group, 3000);
        } catch (NacosException e) {
            throw new RuntimeException("获取配置失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean publishConfig(String dataId, String group, String content) {
        try {
            return configService.publishConfig(dataId, group, content);
        } catch (NacosException e) {
            throw new RuntimeException("发布配置失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteConfig(String dataId, String group) {
        try {
            return configService.removeConfig(dataId, group);
        } catch (NacosException e) {
            throw new RuntimeException("删除配置失败: " + e.getMessage(), e);
        }
    }
}
