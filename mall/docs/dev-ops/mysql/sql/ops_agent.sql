-- ============================================================
-- ops_agent_db：ops-agent 审批流水独立库
--
-- 用途：
--   ops-agent 收到 Prometheus 告警后，由 LLM Agent 生成 Nacos 配置变更方案，
--   提交到审批队列等人工 approve / reject。本库只存这一类任务记录。
--
-- 和其它服务的关系：
--   - 复用 mall 的 MySQL 实例（100.86.250.112:13306）
--   - 独立库，不和业务表共用 schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS `ops_agent_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `ops_agent_db`;

-- ------------------------------------------------------------
-- approval_task：审批任务流水
--
-- 设计说明：
--   1. id 用 UUID（业务层生成），不走自增，方便跨实例调试
--   2. alert_json 存完整 AlertEvent，方便后期审计 / 回放
--   3. group 是 MySQL 保留字，字段名用 group_name
--   4. status 走 VARCHAR 枚举：
--      PENDING / APPROVED / REJECTED / APPLIED / FAILED / EXPIRED
--   5. 两个索引：
--      - (status, created_at) 供 inbox 列表 + expire 扫描
--      - (alert_key, created_at) 供同告警节流 / 审计查询
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `approval_task`;
CREATE TABLE `approval_task` (
  `id`              varchar(64)   NOT NULL COMMENT '任务ID（UUID）',
  `alert_key`       varchar(255)           DEFAULT NULL COMMENT '告警聚合键 alertname|application|resource',
  `alert_json`      json                   DEFAULT NULL COMMENT '原始 AlertEvent JSON',
  `domain`          varchar(32)            DEFAULT NULL COMMENT '策略域：sentinel / dtp / notify / manual',
  `tool_name`       varchar(64)            DEFAULT NULL COMMENT '被拦截的 tool 名，如 publishConfig',
  `data_id`         varchar(255)           DEFAULT NULL COMMENT 'Nacos dataId',
  `group_name`      varchar(128)           DEFAULT NULL COMMENT 'Nacos group（避开 MySQL 保留字 group）',
  `content_before`  mediumtext             COMMENT '审批创建时 Nacos 原值（防并发覆盖）',
  `content_after`   mediumtext             COMMENT 'Agent 拟写入的新值',
  `reasoning`       text                   COMMENT 'Agent 变更理由 / notify 说明',
  `status`          varchar(16)   NOT NULL COMMENT 'PENDING / APPROVED / REJECTED / APPLIED / FAILED / EXPIRED',
  `error_message`   text                   COMMENT 'FAILED / REJECTED 时的原因',
  `created_at`      datetime(3)   NOT NULL COMMENT '任务创建时间',
  `decided_at`      datetime(3)            DEFAULT NULL COMMENT '决策时间（approve/reject/apply/expire）',
  PRIMARY KEY (`id`),
  KEY `idx_status_created` (`status`, `created_at`),
  KEY `idx_alert_key_created` (`alert_key`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ops-agent 审批任务流水';

-- 本地/联调示例：一条待审批、一条已应用（JSON 为占位，实跑时由服务写入真实 AlertEvent）
INSERT INTO `approval_task` (
  `id`, `alert_key`, `alert_json`, `domain`, `tool_name`, `data_id`, `group_name`, `content_before`, `content_after`, `reasoning`, `status`, `error_message`, `created_at`, `decided_at`
) VALUES
  (
    'a1b2c3d4e5f64789999001100112233',
    'HighCPU|order-service|pod-1',
    '{"status":"firing","summary":"CPU > 80%"}',
    'dtp', 'applyThreadPool', 'order-service-dtp-dev.yml', 'SENTINEL_GROUP',
    'corePoolSize: 4',
    'corePoolSize: 8',
    '示例：压测窗口临时上调核心线程，审批通过后由 Agent 落库 Nacos',
    'PENDING',
    NULL,
    '2026-04-23 10:00:00.000',
    NULL
  ),
  (
    'b2c3d4e5f6478999900110011223344',
    'NacosPushLatency|pay-service|nacos-1',
    '{"status":"firing","summary":"push latency p99 高"}',
    'manual', 'publishConfig', 'pay-routes.yaml', 'DEFAULT_GROUP',
    'timeoutMs: 3000',
    'timeoutMs: 5000',
    '已人工确认网络抖动，提高超时阈值',
    'APPLIED',
    NULL,
    '2026-04-20 14:00:00.000',
    '2026-04-20 14:05:00.123'
  );
