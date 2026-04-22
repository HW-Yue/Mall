package com.yue.opsagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 反查映射配置，绑定 {@code ops-agent.mapping.*}。
 *
 * <p>主要给 HikariTuningStrategy / RedisNotifyStrategy 用：</p>
 * <ul>
 *   <li>{@link #poolNameToApp} —— 由告警里的 {@code labels.pool} 反查到具体服务及 Nacos dataId，
 *       这样 HikariCP 阈值调优不需要 {@code ${app}-datasource-dev.yml} 猜应用名</li>
 *   <li>{@link #clientNameToApp} —— 由 Redis client-name（如 {@code mall-redisson}）反查到服务，
 *       供 Agent 在通知文本里给出"责任服务"提示</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "ops-agent.mapping")
public class MappingProperties {

    /** Hikari pool-name → 应用/Nacos 定位信息。key 与 {@code spring.datasource.hikari.pool-name} 对齐。 */
    private Map<String, PoolTarget> poolNameToApp = new LinkedHashMap<>();

    /** Redis client-name → application name（用于 NotifyOnly 策略在诊断文本里展示）。 */
    private Map<String, String> clientNameToApp = new LinkedHashMap<>();

    public Map<String, PoolTarget> getPoolNameToApp() {
        return poolNameToApp;
    }

    public void setPoolNameToApp(Map<String, PoolTarget> poolNameToApp) {
        this.poolNameToApp = poolNameToApp;
    }

    public Map<String, String> getClientNameToApp() {
        return clientNameToApp;
    }

    public void setClientNameToApp(Map<String, String> clientNameToApp) {
        this.clientNameToApp = clientNameToApp;
    }

    /** Hikari pool 对应的定位三元组：服务名 + Nacos dataId + group。 */
    public static class PoolTarget {

        private String application;
        private String dataId;
        private String group = "DEFAULT_GROUP";

        public String getApplication() {
            return application;
        }

        public void setApplication(String application) {
            this.application = application;
        }

        public String getDataId() {
            return dataId;
        }

        public void setDataId(String dataId) {
            this.dataId = dataId;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }
    }
}
