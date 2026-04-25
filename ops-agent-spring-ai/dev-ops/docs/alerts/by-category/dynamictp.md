# category: `dynamictp`

**规则组**：`dynamictp-global`、`order-service-specific`（线程池子集）  
**来源文件**：`mall/docs/dev-ops/prometheus/alert_rules.yml`

## 全局

| alertname | for | severity | expr（摘要） |
|-----------|-----|----------|--------------|
| ThreadPoolActiveRatioHigh | 2m | warning | `thread_pool_active_count / clamp_min(thread_pool_maximum_size, 1) > 0.8` |
| ThreadPoolQueueUsageHigh | 2m | critical | `thread_pool_queue_size / clamp_min(thread_pool_queue_size + thread_pool_queue_remaining_capacity, 1) > 0.8` |
| ThreadPoolAtMaxCapacity | 1m | warning | `thread_pool_current_size >= thread_pool_maximum_size` |
| ThreadPoolRejectedTasks | 1m | critical | `increase(thread_pool_reject_count[1m]) > 0` |

**标签**：`category: dynamictp`；Micrometer 标签含 **`application`**、`thread_pool_name`、`thread_pool_alias`。

## order-service 专项

| alertname | for | severity | application | expr（摘要） |
|-----------|-----|----------|-------------|--------------|
| OrderServiceThreadPoolBusy | 2m | warning | order-service | `thread_pool_active_count{application="order-service", thread_pool_name="threadPoolExecutor"} / clamp_min(thread_pool_maximum_size{...}, 1) > 0.7` |

## 返回索引

[`README.md`](README.md)
