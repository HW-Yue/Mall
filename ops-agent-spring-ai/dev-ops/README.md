# DevOps 资料（Nexus 仓库）

本目录集中存放 **运维/告警/联调** 相关的前端控制台与 **开发文档**。  
**Agent 开发与 SOP 编写**请优先阅读 [`docs/README.md`](docs/README.md)，并按其中的索引跳转到细则。

## 子目录

| 路径 | 说明 |
|------|------|
| [`frontend/`](frontend/README.md) | 静态控制台：`index.html` 对话 + 审批收件箱（对接本模块 API） |
| [`docs/`](docs/README.md) | **开发文档主入口**：告警格式、来源、与 Agent/SOP 的对应关系 |

## 与商城现网配置的对应关系（准确来源）

商城侧 Prometheus 规则与抓取定义的**权威路径**在 monorepo 内为：

- [`mall/docs/dev-ops/prometheus/alert_rules.yml`](../../mall/docs/dev-ops/prometheus/alert_rules.yml) — 所有 `alert` 名称与表达式
- [`mall/docs/dev-ops/prometheus/prometheus.yml`](../../mall/docs/dev-ops/prometheus/prometheus.yml) — 抓取任务、Nacos SD、Exporter
- [`mall/docs/dev-ops/prometheus/alertmanager.yml`](../../mall/docs/dev-ops/prometheus/alertmanager.yml) — Alertmanager 路由与 webhook

本 `dev-ops/docs` 下的描述均应与上述文件保持一致；若不一致，**以 mall 下 YAML 为准**，并应回写修正本文档。

## 目录树（细碎文件一览，便于 AI 检索）

```text
dev-ops/
├── README.md                          # 本文件：总入口
├── frontend/
│   ├── README.md                      # 前端使用说明
│   ├── index.html
│   ├── config.js
│   ├── css/, js/*.js
└── docs/
    ├── README.md                      # 开发文档索引
    └── alerts/
        ├── README.md                  # 告警文档索引
        ├── overview-alert-pipeline.md # 指标→规则→Alertmanager→Webhook
        ├── sop-tool-mapping.md        # 告警域 → spring-ai 七域工具
        ├── prometheus/
        │   └── scrape-and-labels.md   # 抓取任务、Nacos SD、Exporter 标签
        ├── alertmanager/
        │   └── webhook-and-routing.md # 分组、抑制、webhook URL
        ├── formats/
        │   ├── prometheus-alert-labels.md    # 规则 labels/annotations
        │   └── alertmanager-webhook-payload.md # POST JSON 结构
        └── by-category/
            ├── README.md              # category 总表 + alertname 列表
            ├── sentinel.md
            ├── dynamictp.md
            ├── http.md
            ├── hikari.md
            ├── jvm-system.md
            ├── mysql.md
            ├── redis.md
            ├── rocketmq.md
            └── service-specific.md     # application 标签对照
```

## AI / 贡献者快速跳转

1. 告警总览 → [`docs/alerts/README.md`](docs/alerts/README.md)  
2. 流水线 → [`docs/alerts/overview-alert-pipeline.md`](docs/alerts/overview-alert-pipeline.md)  
3. Webhook / Alertmanager → [`docs/alerts/alertmanager/webhook-and-routing.md`](docs/alerts/alertmanager/webhook-and-routing.md)  
4. 按 category 逐条规则 → [`docs/alerts/by-category/README.md`](docs/alerts/by-category/README.md)  
5. SOP ↔ 工具 → [`docs/alerts/sop-tool-mapping.md`](docs/alerts/sop-tool-mapping.md)
