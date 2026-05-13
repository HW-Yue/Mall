CREATE DATABASE IF NOT EXISTS ops_agent
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE ops_agent;

CREATE TABLE IF NOT EXISTS ops_run_summary (
    run_id VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '运行ID',
    input_type VARCHAR(32) DEFAULT NULL COMMENT '输入类型: ALERT/TEXT',
    status VARCHAR(32) DEFAULT NULL COMMENT '运行状态',
    current_node VARCHAR(64) DEFAULT NULL COMMENT '当前节点',
    event_count BIGINT NOT NULL DEFAULT 0 COMMENT '累计事件数',
    first_event_type VARCHAR(64) DEFAULT NULL COMMENT '首个事件类型',
    first_event_message VARCHAR(255) DEFAULT NULL COMMENT '首个事件消息',
    last_event_type VARCHAR(64) DEFAULT NULL COMMENT '最后事件类型',
    last_event_message VARCHAR(255) DEFAULT NULL COMMENT '最后事件消息',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL COMMENT '更新时间',
    KEY idx_updated_at (updated_at),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ops-agent运行摘要';
