#!/usr/bin/env bash
#
# 手工构造 3 类 Alertmanager webhook 验证策略路由：
#   1. Sentinel   (category=sentinel  , alertname=MallServiceQueryGoodsPageBlockHigh) → 橙色徽章、dataId=mall-flow-rules.json
#   2. DTP        (category=dynamictp , alertname=ThreadPoolAtMaxCapacity           ) → 蓝色徽章、dataId=mall-dtp-dev.yml
#   3. Notify     (category=system    , alertname=JvmHeapUsageHigh                  ) → 灰色徽章、不触发 Agent
#
# 使用：bash ops-agent/devops/test-webhooks.sh
#
set -euo pipefail
API=${API:-http://localhost:8098/api/v1/alert/receive}

post() {
  local name="$1"; shift
  local body="$1"; shift
  echo "===== [$name] POST $API"
  curl -sS -w "\n-> HTTP %{http_code}\n" -X POST "$API" \
    -H 'Content-Type: application/json' \
    -d "$body"
  echo
  sleep 1
}

post "sentinel:MallServiceQueryGoodsPageBlockHigh" '{
  "version": "4",
  "status": "firing",
  "receiver": "ops-agent",
  "alerts": [{
    "status": "firing",
    "labels": {
      "alertname": "MallServiceQueryGoodsPageBlockHigh",
      "severity": "warning",
      "category": "sentinel",
      "app": "mall",
      "application": "mall",
      "resource": "/api/v1/mall/index/query_goods_page",
      "instance": "host.docker.internal:8077"
    },
    "annotations": {
      "summary": "mall 商品查询限流频繁",
      "description": "query_goods_page block QPS = 1.8k/s"
    },
    "startsAt": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'"
  }]
}'

post "dtp:ThreadPoolAtMaxCapacity" '{
  "version": "4",
  "status": "firing",
  "receiver": "ops-agent",
  "alerts": [{
    "status": "firing",
    "labels": {
      "alertname": "ThreadPoolAtMaxCapacity",
      "severity": "warning",
      "category": "dynamictp",
      "app_name": "mall",
      "application": "mall",
      "thread_pool_name": "tomcatTp",
      "thread_pool_alias": "mall-tomcat",
      "instance": "host.docker.internal:8077"
    },
    "annotations": {
      "summary": "线程池已达到最大容量",
      "description": "服务 mall 线程池 tomcatTp current=20 已达到 max"
    },
    "startsAt": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'"
  }]
}'

post "notify:JvmHeapUsageHigh" '{
  "version": "4",
  "status": "firing",
  "receiver": "ops-agent",
  "alerts": [{
    "status": "firing",
    "labels": {
      "alertname": "JvmHeapUsageHigh",
      "severity": "warning",
      "category": "system",
      "application": "mall",
      "instance": "host.docker.internal:8077"
    },
    "annotations": {
      "summary": "JVM 堆使用率过高",
      "description": "mall 服务堆使用率 > 85% 持续 5 分钟"
    },
    "startsAt": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'"
  }]
}'

echo "======================================"
echo "预期结果："
echo "  curl $API/../approvals 能看到 3 条 task"
echo "    - domain=sentinel  dataId=mall-flow-rules.json     group=SENTINEL_GROUP"
echo "    - domain=dtp       dataId=mall-dtp-dev.yml         group=DEFAULT_GROUP"
echo "    - domain=notify    dataId=-                         group=-  tool=''"
echo "  前端 devops/index.html#inbox 的徽章依次显示 sentinel(橙) / dtp(蓝) / notify(灰)"
echo "======================================"
