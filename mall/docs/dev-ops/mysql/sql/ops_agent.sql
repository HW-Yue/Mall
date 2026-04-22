-- ============================================================
-- ops_agent_db：ops-agent 审批流水独立库
--
-- 用途：
--   ops-agent 收到 Prometheus 告警后，由 LLM Agent 生成 Nacos 配置变更方案，
--   提交到审批队列等人工 approve / reject。本库只存这一类任务记录。
--
-- 和其它服务的关系：
--   - 复用 mall 的 MySQL 实例（localhost:13306）
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
