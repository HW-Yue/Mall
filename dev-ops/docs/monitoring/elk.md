# ELK

- ELK 开发环境 compose：`dev-ops/docker-compose-elk.yml`
- 组成：Elasticsearch 9.2.0、Logstash 9.2.0、Kibana 9.2.0

## 端口

| 组件 | 宿主机端口 | 说明 |
|---|---|---|
| Elasticsearch | `9200` / `9300` | ES HTTP / transport |
| Logstash | `4560` / `50000` / `9600` | 日志接收与监控 |
| Kibana | `5601` | Web UI |

## 开发环境约定

- `xpack.security.enabled=false`，开发环境默认无鉴权
- 使用独立命名卷 `nexus-esdata`
- 日志上报入口由各服务 `logstash` 配置指向 `:4560`

## 与 SkyWalking 的关系

- ELK 使用 ES 9.x
- SkyWalking 使用独立 ES 7.17 存储，不与 ELK 共库

## 事实来源

- `dev-ops/docker-compose-elk.yml`
- `dev-ops/full-flow-test/README.md`
