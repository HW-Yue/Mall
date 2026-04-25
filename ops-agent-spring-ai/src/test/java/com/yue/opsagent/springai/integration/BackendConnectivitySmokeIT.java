package com.yue.opsagent.springai.integration;

import com.yue.opsagent.springai.config.OpsAiProperties;
import com.yue.opsagent.springai.skill.api.ToolResult;
import com.yue.opsagent.springai.skill.docker.DockerToolkit;
import com.yue.opsagent.springai.skill.elasticsearch.ElasticsearchToolkit;
import com.yue.opsagent.springai.skill.mysql.MysqlToolkit;
import com.yue.opsagent.springai.skill.nacos.NacosToolkit;
import com.yue.opsagent.springai.skill.prometheus.PrometheusToolkit;
import com.yue.opsagent.springai.skill.redis.RedisToolkit;
import com.yue.opsagent.springai.skill.rocketmq.RocketMqToolkit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 七域 {@code *Toolkit} 方法全覆盖冒烟：不跑 LLM，只验证各 public 方法能否走通（返回 {@link ToolResult.Ok}）。
 * <p>
 * 默认整类跳过；启用：
 * {@code OPS_AGENT_INTEGRATION_SMOKE=true mvn -pl ops-agent-spring-ai test -Dtest=BackendConnectivitySmokeIT}
 * <p>
 * 可选环境变量（不设则跳过对应用例，避免误伤）：
 * <ul>
 *   <li>{@code OPS_AGENT_SMOKE_DOCKER_CONTAINER} — 容器 id，用于 docker_logs/stats/inspect/exec</li>
 *   <li>{@code OPS_AGENT_SMOKE_NACOS_DATA_ID} — getConfig 的 dataId（group 默认 DEFAULT_GROUP）</li>
 *   <li>{@code OPS_AGENT_SMOKE_NACOS_SERVICE} — listInstances 的服务名</li>
 *   <li>{@code OPS_AGENT_SMOKE_MQ_GROUP} — consumerStatus 的消费组</li>
 *   <li>{@code OPS_AGENT_SMOKE_MQ_TOPIC} — topicStats(route) / consumerStatus 的可选 topic</li>
 * </ul>
 * {@link NacosToolkit#publishConfig} 有写副作用，本类<strong>不</strong>自动测，需人工在控制台试。
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.ai.dashscope.api-key=dummy-key-for-smoke",
        "ops-ai.otlp.enabled=false"
})
@EnabledIfEnvironmentVariable(named = "OPS_AGENT_INTEGRATION_SMOKE", matches = "true")
class BackendConnectivitySmokeIT {

    @Autowired
    private PrometheusToolkit prometheusToolkit;
    @Autowired
    private ElasticsearchToolkit elasticsearchToolkit;
    @Autowired
    private MysqlToolkit mysqlToolkit;
    @Autowired
    private RedisToolkit redisToolkit;
    @Autowired
    private NacosToolkit nacosToolkit;
    @Autowired
    private DockerToolkit dockerToolkit;
    @Autowired
    private RocketMqToolkit rocketMqToolkit;
    @Autowired
    private OpsAiProperties opsAiProperties;

    private static void assertOk(ToolResult r, String hint) {
        assertThat(r).as(hint).isInstanceOf(ToolResult.Ok.class);
    }

    private static String env(String key) {
        String v = System.getenv(key);
        return v == null ? "" : v.trim();
    }

    // --- PrometheusToolkit（仅 queryInstant）---

    @Test
    void prometheus_queryInstant() {
        assertOk(prometheusToolkit.queryInstant("up"), "PrometheusToolkit.queryInstant");
    }

    // --- ElasticsearchToolkit ---

    @Test
    void elasticsearch_listIndices() {
        assertOk(elasticsearchToolkit.listIndices(), "ElasticsearchToolkit.listIndices");
    }

    @Test
    void elasticsearch_listIndices_skywalkingCluster() {
        assertOk(elasticsearchToolkit.listIndices("skywalking"), "ElasticsearchToolkit.listIndices skywalking");
    }

    @Test
    void elasticsearch_search() {
        assertOk(elasticsearchToolkit.search("*", null), "ElasticsearchToolkit.search");
    }

    @Test
    void elasticsearch_count() {
        assertOk(elasticsearchToolkit.count("*", null), "ElasticsearchToolkit.count");
    }

    @Test
    void elasticsearch_aggregate() {
        assertOk(elasticsearchToolkit.aggregate("*", null), "ElasticsearchToolkit.aggregate");
    }

    // --- MysqlToolkit ---

    @Test
    void mysql_processlist() {
        assertOk(mysqlToolkit.processlist(), "MysqlToolkit.processlist");
    }

    @Test
    void mysql_globalStatusLike() {
        assertOk(mysqlToolkit.globalStatusLike("Uptime"), "MysqlToolkit.globalStatusLike");
    }

    @Test
    void mysql_metadataLocks() {
        assertOk(mysqlToolkit.metadataLocks(), "MysqlToolkit.metadataLocks");
    }

    @Test
    void mysql_slowQuerySample() {
        ToolResult r = mysqlToolkit.slowQuerySample();
        if (r instanceof ToolResult.Error e) {
            assumeFalse(
                    e.message().contains("doesn't exist")
                            || e.message().contains("Unknown table")
                            || e.message().contains("slow_log"),
                    "未开启 slow_log 表时跳过: " + e.message());
        }
        assertOk(r, "MysqlToolkit.slowQuerySample");
    }

    // --- RedisToolkit ---

    @Test
    void redis_info_default() {
        assertOk(redisToolkit.info(""), "RedisToolkit.info default");
    }

    @Test
    void redis_info_server() {
        assertOk(redisToolkit.info("server"), "RedisToolkit.info server");
    }

    @Test
    void redis_slowlog() {
        assertOk(redisToolkit.slowlog(8), "RedisToolkit.slowlog");
    }

    @Test
    void redis_clientList() {
        assertOk(redisToolkit.clientList(), "RedisToolkit.clientList");
    }

    @Test
    void redis_memoryStats() {
        assertOk(redisToolkit.memoryStats(), "RedisToolkit.memoryStats");
    }

    @Test
    void redis_getKey_scanSample() {
        assertOk(redisToolkit.getKey(null, "*", 5), "RedisToolkit.getKey SCAN");
    }

    // --- NacosToolkit（不测 publishConfig）---

    @Test
    void nacos_listServices() {
        assertOk(nacosToolkit.listServices(1, 20), "NacosToolkit.listServices");
    }

    @Test
    void nacos_getConfig() {
        String dataId = env("OPS_AGENT_SMOKE_NACOS_DATA_ID");
        assumeTrue(!dataId.isEmpty(), "未设置 OPS_AGENT_SMOKE_NACOS_DATA_ID 时跳过 getConfig");
        assertOk(nacosToolkit.getConfig(dataId, null), "NacosToolkit.getConfig");
    }

    @Test
    void nacos_listInstances() {
        String svc = env("OPS_AGENT_SMOKE_NACOS_SERVICE");
        assumeTrue(!svc.isEmpty(), "未设置 OPS_AGENT_SMOKE_NACOS_SERVICE 时跳过 listInstances");
        assertOk(nacosToolkit.listInstances(svc, null), "NacosToolkit.listInstances");
    }

    // --- DockerToolkit ---

    @Test
    void docker_pingEngine() {
        assertOk(dockerToolkit.pingEngine(), "DockerToolkit.pingEngine");
    }

    @Test
    void docker_logs() {
        String cid = env("OPS_AGENT_SMOKE_DOCKER_CONTAINER");
        assumeTrue(!cid.isEmpty(), "未设置 OPS_AGENT_SMOKE_DOCKER_CONTAINER 时跳过 docker_logs");
        assertOk(dockerToolkit.dockerLogs(cid, 20), "DockerToolkit.dockerLogs");
    }

    @Test
    void docker_stats() {
        String cid = env("OPS_AGENT_SMOKE_DOCKER_CONTAINER");
        assumeTrue(!cid.isEmpty(), "未设置 OPS_AGENT_SMOKE_DOCKER_CONTAINER 时跳过 docker_stats");
        assertOk(dockerToolkit.dockerStats(cid), "DockerToolkit.dockerStats");
    }

    @Test
    void docker_inspect() {
        String cid = env("OPS_AGENT_SMOKE_DOCKER_CONTAINER");
        assumeTrue(!cid.isEmpty(), "未设置 OPS_AGENT_SMOKE_DOCKER_CONTAINER 时跳过 docker_inspect");
        assertOk(dockerToolkit.dockerInspect(cid), "DockerToolkit.dockerInspect");
    }

    @Test
    void docker_exec() {
        String cid = env("OPS_AGENT_SMOKE_DOCKER_CONTAINER");
        assumeTrue(!cid.isEmpty(), "未设置 OPS_AGENT_SMOKE_DOCKER_CONTAINER 时跳过 docker_exec");
        assertOk(dockerToolkit.dockerExec(cid, "echo ok"), "DockerToolkit.dockerExec");
    }

    // --- RocketMqToolkit ---

    @Test
    void rocketmq_topicStats_list() {
        assumeTrue(opsAiProperties.getRocketmq().isEnabled(), "ops-ai.rocketmq.enabled=false");
        assertOk(rocketMqToolkit.topicStats(""), "RocketMqToolkit.topicStats list");
    }

    @Test
    void rocketmq_topicStats_route() {
        assumeTrue(opsAiProperties.getRocketmq().isEnabled(), "ops-ai.rocketmq.enabled=false");
        String topic = env("OPS_AGENT_SMOKE_MQ_TOPIC");
        assumeTrue(!topic.isEmpty(), "未设置 OPS_AGENT_SMOKE_MQ_TOPIC 时跳过路由查询");
        assertOk(rocketMqToolkit.topicStats(topic), "RocketMqToolkit.topicStats route");
    }

    @Test
    void rocketmq_consumerStatus() {
        assumeTrue(opsAiProperties.getRocketmq().isEnabled(), "ops-ai.rocketmq.enabled=false");
        String group = env("OPS_AGENT_SMOKE_MQ_GROUP");
        assumeTrue(!group.isEmpty(), "未设置 OPS_AGENT_SMOKE_MQ_GROUP 时跳过 consumerStatus");
        String topic = env("OPS_AGENT_SMOKE_MQ_TOPIC");
        assertOk(
                rocketMqToolkit.consumerStatus(topic.isEmpty() ? null : topic, group),
                "RocketMqToolkit.consumerStatus");
    }

    @Test
    void rocketmq_deadLetterHint() {
        assumeTrue(opsAiProperties.getRocketmq().isEnabled(), "ops-ai.rocketmq.enabled=false");
        String topic = env("OPS_AGENT_SMOKE_MQ_TOPIC");
        assertOk(
                rocketMqToolkit.deadLetterHint(topic.isEmpty() ? null : topic, null),
                "RocketMqToolkit.deadLetterHint");
    }
}
