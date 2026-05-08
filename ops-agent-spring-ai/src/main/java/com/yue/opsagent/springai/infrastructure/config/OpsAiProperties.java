package com.yue.opsagent.springai.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "ops-ai")
public class OpsAiProperties {

    private final Elasticsearch elasticsearch = new Elasticsearch();
    private final Prometheus prometheus = new Prometheus();
    private final Nacos nacos = new Nacos();
    private final Sop sop = new Sop();
    private final React react = new React();
    private final Chat chat = new Chat();
    private final Alert alert = new Alert();
    private final Docker docker = new Docker();
    private final Mysql mysql = new Mysql();
    private final Rocketmq rocketmq = new Rocketmq();
    private final Redis redis = new Redis();
    private final Otlp otlp = new Otlp();
    private final Catalog catalog = new Catalog();

    public Elasticsearch getElasticsearch() {
        return elasticsearch;
    }

    public Prometheus getPrometheus() {
        return prometheus;
    }

    public Nacos getNacos() {
        return nacos;
    }

    public Sop getSop() {
        return sop;
    }

    public React getReact() {
        return react;
    }

    public Chat getChat() {
        return chat;
    }

    public Alert getAlert() {
        return alert;
    }

    public Docker getDocker() {
        return docker;
    }

    public Mysql getMysql() {
        return mysql;
    }

    public Rocketmq getRocketmq() {
        return rocketmq;
    }

    public Redis getRedis() {
        return redis;
    }

    public Otlp getOtlp() {
        return otlp;
    }

    public Catalog getCatalog() {
        return catalog;
    }

    /**
     * OpenTelemetry OTLP → SkyWalking OAP 10.4（Virtual GenAI：span 含 gen_ai.response.model）。
     */
    public static class Otlp {
        private boolean enabled = true;
        /** gRPC OTLP，与 Java Agent 共用 OAP 11800 */
        private String endpoint = "http://127.0.0.1:11800";
        private String serviceName = "yue-ops-agent";
        private int timeoutSeconds = 10;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    public static class Alert {
        /** react: SOP 注入 OpsAgent ReAct；deterministic: 逐步 SopStepRunner */
        private String mode = "deterministic";

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }
    }

    public static class Catalog {
        /**
         * 运行时只读知识库快照，避免依赖 sibling module 路径。
         */
        private String location = "classpath:ops-catalog/catalog.json";

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }
    }

    public static class Docker {
        /** e.g. unix:///var/run/docker.sock or tcp://127.0.0.1:2375 */
        private String host = "unix:///var/run/docker.sock";
        private String apiVersion = "";

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getApiVersion() {
            return apiVersion;
        }

        public void setApiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
        }
    }

    public static class Mysql {
        private String jdbcUrl = "jdbc:mysql://127.0.0.1:13306/mysql?useSSL=false&allowPublicKeyRetrieval=true";
        private String username = "root";
        private String password = "";

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class Rocketmq {
        private String nameServer = "127.0.0.1:9876";
        private boolean enabled = true;

        public String getNameServer() {
            return nameServer;
        }

        public void setNameServer(String nameServer) {
            this.nameServer = nameServer;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Redis {
        /** 若设置则优先于 host/port/password（如 redis://:pass@127.0.0.1:16379/0） */
        private String uri = "";
        private String host = "127.0.0.1";
        private int port = 16379;
        private String password = "";
        private int database = 0;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getDatabase() {
            return database;
        }

        public void setDatabase(int database) {
            this.database = database;
        }
    }

    public static class React {
        private int maxParentIters = 12;
        private int maxSubIters = 8;

        public int getMaxParentIters() {
            return maxParentIters;
        }

        public void setMaxParentIters(int maxParentIters) {
            this.maxParentIters = maxParentIters;
        }

        public int getMaxSubIters() {
            return maxSubIters;
        }

        public void setMaxSubIters(int maxSubIters) {
            this.maxSubIters = maxSubIters;
        }
    }

    public static class Chat {
        /** simple: 仅流式闲聊；react: 父 Agent 工具委派 */
        private String mode = "simple";

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }
    }

    public static class Elasticsearch {
        /** 应用日志 / ELK 写入的 ES（如 nexus-*） */
        private String baseUrl = "http://localhost:9200";
        private String username = "";
        private String password = "";
        /**
         * SkyWalking OAP 存储集群（索引多为 sw_*）。与 {@link #baseUrl} 相同时留空即可。
         */
        private SkywalkingElasticsearch skywalking = new SkywalkingElasticsearch();

        public SkywalkingElasticsearch getSkywalking() {
            return skywalking;
        }

        public void setSkywalking(SkywalkingElasticsearch skywalking) {
            this.skywalking = skywalking != null ? skywalking : new SkywalkingElasticsearch();
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public static class SkywalkingElasticsearch {
            /** 空则使用外层 {@link Elasticsearch#baseUrl} */
            private String baseUrl = "";
            /** 空则继承外层 username/password */
            private String username = "";
            private String password = "";

            public String getBaseUrl() {
                return baseUrl;
            }

            public void setBaseUrl(String baseUrl) {
                this.baseUrl = baseUrl;
            }

            public String getUsername() {
                return username;
            }

            public void setUsername(String username) {
                this.username = username;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }
        }
    }

    public static class Prometheus {
        private String baseUrl = "http://localhost:9090";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    public static class Nacos {
        private String serverAddr = "127.0.0.1:8848";
        private String namespace = "";
        private String username = "";
        private String password = "";

        public String getServerAddr() {
            return serverAddr;
        }

        public void setServerAddr(String serverAddr) {
            this.serverAddr = serverAddr;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class Sop {
        /**
         * 外部 SOP 目录，例如 {@code classpath:sop/rules/}；扫描 *.yml / *.yaml，按文件名排序后追加到 {@link #rules}。
         * 为空或仅空白则跳过外部加载。
         */
        private String rulesDir = "classpath:sop/rules/";
        private List<Rule> rules = new ArrayList<>();

        public String getRulesDir() {
            return rulesDir;
        }

        public void setRulesDir(String rulesDir) {
            this.rulesDir = rulesDir;
        }

        public List<Rule> getRules() {
            return rules;
        }

        public void setRules(List<Rule> rules) {
            this.rules = rules;
        }

        public static class Rule {
            private String matchAlertname = "";
            private String matchCategory = "";
            private String matchSeverity = "";
            private String matchApplication = "";
            private String matchService = "";
            private String matchResourcePrefix = "";
            private String matchTopic = "";
            private String matchConsumerGroup = "";
            private String matchTable = "";
            private String matchDb = "";
            /** 标准作业程序正文（纯文本即可），供 OpsAgent 使用；配置键仍为 sop-markdown */
            private String sopMarkdown = "";
            private List<Step> steps = new ArrayList<>();

            public String getSopMarkdown() {
                return sopMarkdown;
            }

            public void setSopMarkdown(String sopMarkdown) {
                this.sopMarkdown = sopMarkdown;
            }

            public String getMatchAlertname() {
                return matchAlertname;
            }

            public void setMatchAlertname(String matchAlertname) {
                this.matchAlertname = matchAlertname;
            }

            public String getMatchCategory() {
                return matchCategory;
            }

            public void setMatchCategory(String matchCategory) {
                this.matchCategory = matchCategory;
            }

            public String getMatchSeverity() {
                return matchSeverity;
            }

            public void setMatchSeverity(String matchSeverity) {
                this.matchSeverity = matchSeverity;
            }

            public String getMatchApplication() {
                return matchApplication;
            }

            public void setMatchApplication(String matchApplication) {
                this.matchApplication = matchApplication;
            }

            public String getMatchService() {
                return matchService;
            }

            public void setMatchService(String matchService) {
                this.matchService = matchService;
            }

            public String getMatchResourcePrefix() {
                return matchResourcePrefix;
            }

            public void setMatchResourcePrefix(String matchResourcePrefix) {
                this.matchResourcePrefix = matchResourcePrefix;
            }

            public String getMatchTopic() {
                return matchTopic;
            }

            public void setMatchTopic(String matchTopic) {
                this.matchTopic = matchTopic;
            }

            public String getMatchConsumerGroup() {
                return matchConsumerGroup;
            }

            public void setMatchConsumerGroup(String matchConsumerGroup) {
                this.matchConsumerGroup = matchConsumerGroup;
            }

            public String getMatchTable() {
                return matchTable;
            }

            public void setMatchTable(String matchTable) {
                this.matchTable = matchTable;
            }

            public String getMatchDb() {
                return matchDb;
            }

            public void setMatchDb(String matchDb) {
                this.matchDb = matchDb;
            }

            public List<Step> getSteps() {
                return steps;
            }

            public void setSteps(List<Step> steps) {
                this.steps = steps;
            }
        }

        /**
         * SOP step: {@code direct_tool} uses skill/tool/args;
         * {@code delegate_subagent} uses subAgentId + task (+ optional context map).
         */
        public static class Step {
            /** direct_tool | delegate_subagent */
            private String type = "direct_tool";
            private String skill = "";
            private String tool = "";
            private Map<String, Object> args = new HashMap<>();
            /** Same id as {@link com.yue.opsagent.springai.skill.api.OpsSkillRegistry#name()} for sub agents. */
            private String subAgentId = "";
            private String task = "";
            private Map<String, Object> context = new HashMap<>();
            /** stop | continue on step failure */
            private String onError = "stop";

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public String getSkill() {
                return skill;
            }

            public void setSkill(String skill) {
                this.skill = skill;
            }

            public String getTool() {
                return tool;
            }

            public void setTool(String tool) {
                this.tool = tool;
            }

            public Map<String, Object> getArgs() {
                return args;
            }

            public void setArgs(Map<String, Object> args) {
                this.args = args;
            }

            public String getSubAgentId() {
                return subAgentId;
            }

            public void setSubAgentId(String subAgentId) {
                this.subAgentId = subAgentId;
            }

            public String getTask() {
                return task;
            }

            public void setTask(String task) {
                this.task = task;
            }

            public Map<String, Object> getContext() {
                return context;
            }

            public void setContext(Map<String, Object> context) {
                this.context = context;
            }

            public String getOnError() {
                return onError;
            }

            public void setOnError(String onError) {
                this.onError = onError;
            }
        }
    }
}
