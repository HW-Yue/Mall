# 部署与配置

## 本地启动

1. 复制模板：

```bash
cp application-local.example.yml application-local.yml
```

2. 按需修改本地地址和账号。
3. 设置模型 Key：

```bash
export DASHSCOPE_API_KEY=your-key
```

4. 启动：

```bash
mvn spring-boot:run
```

5. 单独启动前端时，使用静态服务器或 Docker Compose 暴露 `dev-ops/frontend/`。

```bash
docker compose -f ../dev-ops/docker-compose-apps-test.yml up -d ops-agent-frontend gateway ops-agent-spring-ai
```

## 配置来源

- `src/main/resources/application.yml`：公开默认值
- `application-local.yml`：本地覆盖文件，不提交到仓库
- 环境变量：优先级最高，适合 CI / Docker / 远程部署

## 需要关注的变量

- `DASHSCOPE_API_KEY`
- `NACOS_SERVER_ADDR`
- `NACOS_DISCOVERY_IP`
- `OPS_AI_OTLP_ENDPOINT`
- `OPS_AI_ELASTICSEARCH_BASE_URL`
- `OPS_AI_PROMETHEUS_BASE_URL`
- `OPS_AI_MYSQL_JDBC_URL`
- `OPS_AI_REDIS_URI`
- `LOGSTASH_ENABLED`

## 公开仓库默认值

- Nacos、MySQL、Redis、ES、Prometheus、RocketMQ 都默认指向本机
- OTLP 默认关闭
- DashScope Key 默认留空
- 不包含任何内网 IP、网关地址或真实口令
- 独立前端默认通过 `http://localhost:8090/gw/api/v1/ops-ai` 访问后端 API
