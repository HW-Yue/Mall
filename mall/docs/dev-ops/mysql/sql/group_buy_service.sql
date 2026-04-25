-- ============================================================
-- group_buy_service：拼团营销服务独立库
--
-- 包含：
--   人群标签（crowd_tags / crowd_tags_detail / crowd_tags_job）
--   折扣配置（discount）
--   拼团活动（group_buy_activity）
--   渠道-商品-活动映射（sc_sku_activity，仅拼团条目）
--   拼团团队记录（group_buy_order）
--   拼团订单扩展（t_order_group，与 order_service.t_order 通过 order_id 关联）
--   回调任务（notify_task）
--
-- 注意：discount / crowd_tags 每个营销服务各自维护，数据独立，不共享
-- ============================================================

CREATE DATABASE IF NOT EXISTS `group_buy_service` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `group_buy_service`;

-- ------------------------------------------------------------
-- crowd_tags：人群标签
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `crowd_tags`;
CREATE TABLE `crowd_tags` (
  `id`          int unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `tag_id`      varchar(32)  NOT NULL COMMENT '人群ID',
  `tag_name`    varchar(64)  NOT NULL COMMENT '人群名称',
  `tag_desc`    varchar(256) NOT NULL COMMENT '人群描述',
  `statistics`  int          NOT NULL COMMENT '人群统计量',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人群标签';

INSERT INTO `crowd_tags` (`tag_id`, `tag_name`, `tag_desc`, `statistics`)
VALUES ('RQ_KJHKL98UU78H66554GFDV', '潜在消费用户', '潜在消费用户', 33);


-- ------------------------------------------------------------
-- crowd_tags_detail：人群标签明细（用户归属）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `crowd_tags_detail`;
CREATE TABLE `crowd_tags_detail` (
  `id`          int unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `tag_id`      varchar(32)  NOT NULL COMMENT '人群ID',
  `user_id`     varchar(16)  NOT NULL COMMENT '用户ID',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_tag_user` (`tag_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人群标签明细';

INSERT INTO `crowd_tags_detail` (`tag_id`, `user_id`)
VALUES
  ('RQ_KJHKL98UU78H66554GFDV', 'xiaofuge'),
  ('RQ_KJHKL98UU78H66554GFDV', 'liergou'),
  ('RQ_KJHKL98UU78H66554GFDV', 'xfg01'),
  ('RQ_KJHKL98UU78H66554GFDV', 'xfg02'),
  ('RQ_KJHKL98UU78H66554GFDV', 'xfg03'),
  ('RQ_KJHKL98UU78H66554GFDV', 'xfg04'),
  ('RQ_KJHKL98UU78H66554GFDV', 'xfg05'),
  ('RQ_KJHKL98UU78H66554GFDV', 'xfg06'),
  ('RQ_KJHKL98UU78H66554GFDV', 'xfg07'),
  ('RQ_KJHKL98UU78H66554GFDV', 'xfg08'),
  ('RQ_KJHKL98UU78H66554GFDV', 'xfg09');


-- ------------------------------------------------------------
-- crowd_tags_job：人群标签计算任务
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `crowd_tags_job`;
CREATE TABLE `crowd_tags_job` (
  `id`              int unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `tag_id`          varchar(32)  NOT NULL COMMENT '标签ID',
  `batch_id`        varchar(8)   NOT NULL COMMENT '批次ID',
  `tag_type`        tinyint(1)   NOT NULL DEFAULT 1 COMMENT '标签类型（参与量、消费金额）',
  `tag_rule`        varchar(8)   NOT NULL COMMENT '标签规则（限定类型 N次）',
  `stat_start_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '统计开始时间',
  `stat_end_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '统计结束时间',
  `status`          tinyint(1)   NOT NULL DEFAULT 0 COMMENT '状态：0初始、1计划、2重置、3完成',
  `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_batch_id` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人群标签任务';

INSERT INTO `crowd_tags_job` (`tag_id`, `batch_id`, `tag_type`, `tag_rule`, `stat_start_time`, `stat_end_time`, `status`)
VALUES ('RQ_KJHKL98UU78H66554GFDV', '10001', 0, '100', NOW(), NOW(), 0);


-- ------------------------------------------------------------
-- group_buy_discount：折扣配置（拼团服务专属，秒杀等其他服务各自维护）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `group_buy_discount`;
CREATE TABLE `group_buy_discount` (
  `id`            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `discount_id`   varchar(8)      NOT NULL COMMENT '折扣ID',
  `discount_name` varchar(64)     NOT NULL COMMENT '折扣标题',
  `discount_desc` varchar(256)    NOT NULL COMMENT '折扣描述',
  `discount_type` tinyint(1)      NOT NULL DEFAULT '0' COMMENT '折扣类型（0:base、1:tag）',
  `market_plan`   varchar(4)      NOT NULL DEFAULT 'ZJ' COMMENT '营销优惠计划（ZJ:直减、MJ:满减、N元购）',
  `market_expr`   varchar(32)     NOT NULL COMMENT '营销优惠表达式',
  `tag_id`        varchar(8)               DEFAULT NULL COMMENT '人群标签，特定优惠限定',
  `create_time`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_discount_id` (`discount_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='折扣配置';

INSERT INTO `group_buy_discount` (`discount_id`, `discount_name`, `discount_desc`, `discount_type`, `market_plan`, `market_expr`, `tag_id`)
VALUES
  ('25120207', '模型拼团直减20', 'AI模型拼团立减20元',   0, 'ZJ', '20',  NULL),
  ('25120208', '存储拼团直减30', '存储资源拼团立减30元', 0, 'ZJ', '30',  NULL);


-- ------------------------------------------------------------
-- group_buy_activity：拼团活动
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `group_buy_activity`;
CREATE TABLE `group_buy_activity` (
  `id`               bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增',
  `activity_id`      bigint          NOT NULL COMMENT '活动ID',
  `activity_name`    varchar(128)    NOT NULL COMMENT '活动名称',
  `discount_id`      varchar(8)      NOT NULL COMMENT '关联 discount.discount_id',
  `group_type`       tinyint(1)      NOT NULL DEFAULT 0 COMMENT '拼团方式（0自动成团、1达目标成团）',
  `take_limit_count` int             NOT NULL DEFAULT 1 COMMENT '每人参团次数上限',
  `target`           int             NOT NULL DEFAULT 1 COMMENT '成团人数目标',
  `valid_time`       int             NOT NULL DEFAULT 15 COMMENT '拼团有效时长（分钟）',
  `status`           tinyint(1)      NOT NULL DEFAULT 0 COMMENT '状态：0创建、1生效、2过期、3废弃',
  `start_time`       datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '活动开始时间',
  `end_time`         datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '活动结束时间',
  `tag_id`           varchar(8)               DEFAULT NULL COMMENT '人群标签规则标识',
  `tag_scope`        varchar(4)               DEFAULT NULL COMMENT '人群范围（1可见限制、2参与限制）',
  `create_time`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_activity_id` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='拼团活动';

INSERT INTO `group_buy_activity` (`activity_id`, `activity_name`, `discount_id`, `group_type`, `take_limit_count`, `target`, `valid_time`, `status`, `start_time`, `end_time`, `tag_id`, `tag_scope`)
VALUES
  (100123, 'AI模型拼团活动',   '25120207', 0, 1, 3, 15, 1, '2024-12-07 10:19:40', '2029-12-07 10:19:40', '1', '1'),
  (100124, '存储资源拼团活动', '25120208', 0, 2, 2, 20, 1, NOW(), DATE_ADD(NOW(), INTERVAL 1 YEAR), NULL, NULL);


-- ------------------------------------------------------------
-- sc_sku_activity：渠道-商品-活动映射（拼团条目）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sc_sku_activity`;
CREATE TABLE `sc_sku_activity` (
  `id`            int unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `source`        varchar(8)   NOT NULL COMMENT '渠道',
  `channel`       varchar(8)   NOT NULL COMMENT '来源',
  `activity_id`   bigint       NOT NULL COMMENT '活动ID',
  `activity_type` varchar(32)  NOT NULL COMMENT '活动类型（固定为 group_buy）',
  `goods_id`      varchar(16)  NOT NULL COMMENT '商品ID',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_sc_goods` (`source`, `channel`, `goods_id`),
  KEY `idx_activity_type` (`activity_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='渠道商品活动映射（拼团）';

INSERT INTO `sc_sku_activity` (`source`, `channel`, `activity_id`, `activity_type`, `goods_id`)
VALUES
  ('s01', 'c01', 100123, 'group_buy', '1001'),
  ('s01', 'c01', 100123, 'group_buy', '1003'),
  ('s01', 'c01', 100123, 'group_buy', '1005'),
  ('s01', 'c01', 100124, 'group_buy', '2002'),
  ('s01', 'c01', 100124, 'group_buy', '2004'),
  ('s01', 'c01', 100124, 'group_buy', '2005');


-- ------------------------------------------------------------
-- team_order：拼团团队记录（团级，非个人订单）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `team_order`;
CREATE TABLE `team_order` (
  `id`              int unsigned  NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `team_id`         varchar(8)    NOT NULL COMMENT '拼单组队ID',
  `activity_id`     bigint        NOT NULL COMMENT '活动ID',
  `source`          varchar(8)    NOT NULL COMMENT '渠道',
  `channel`         varchar(8)    NOT NULL COMMENT '来源',
  `original_price`  decimal(8,2)  NOT NULL COMMENT '原始价格',
  `deduction_price` decimal(8,2)  NOT NULL COMMENT '折扣金额',
  `pay_price`       decimal(8,2)  NOT NULL COMMENT '支付价格',
  `target_count`    int           NOT NULL COMMENT '成团目标人数',
  `complete_count`  int           NOT NULL COMMENT '已完成支付人数',
  `lock_count`      int           NOT NULL COMMENT '已锁单人数',
  `status`          tinyint(1)    NOT NULL DEFAULT 0 COMMENT '状态：0拼团中、1完成、2失败、3完成含退单',
  `valid_start_time` datetime     NOT NULL COMMENT '拼团开始时间',
  `valid_end_time`   datetime     NOT NULL COMMENT '拼团结束时间',
  `notify_type`     varchar(8)    NOT NULL DEFAULT 'MQ'  COMMENT '回调类型（HTTP / MQ）',
  `notify_url`      varchar(512)           DEFAULT NULL  COMMENT 'HTTP 回调地址',
  `create_time`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_team_id` (`team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='拼团团队订单';


-- ------------------------------------------------------------
-- t_order：拼团个人订单（含用户、商品、状态信息）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order` (
  `order_id`        varchar(12)   NOT NULL COMMENT '订单ID（来自 order_service）',
  `user_id`         varchar(16)   NOT NULL COMMENT '用户ID',
  `team_id`         varchar(8)    NOT NULL COMMENT '拼团组队ID',
  `activity_id`     bigint        NOT NULL COMMENT '活动ID',
  `goods_id`        varchar(16)   NOT NULL COMMENT '商品ID',
  `out_trade_no`    varchar(32)   NOT NULL COMMENT '外部交易单号',
  `status`          tinyint(1)    NOT NULL DEFAULT 0 COMMENT '状态：0锁定、1已支付、2退款处理中、3已关团（超时未支付）、4已退款',
  `original_price`  decimal(8,2)  NOT NULL COMMENT '原始价格',
  `deduction_price` decimal(8,2)  NOT NULL COMMENT '折扣金额',
  `pay_price`       decimal(8,2)  NOT NULL COMMENT '支付价格',
  `out_trade_time`  datetime               DEFAULT NULL COMMENT '支付完成时间',
  `start_time`      datetime      NOT NULL COMMENT '拼团开始时间',
  `end_time`        datetime      NOT NULL COMMENT '拼团结束时间',
  `create_time`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  KEY `idx_team_id` (`team_id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_user_activity` (`user_id`, `activity_id`),
  KEY `idx_out_trade_no` (`out_trade_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='拼团个人订单';


-- ------------------------------------------------------------
-- notify_task：回调通知任务（拼团结算/退单通知）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `notify_task`;
CREATE TABLE `notify_task` (
  `id`              int unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `activity_id`     bigint       NOT NULL COMMENT '活动ID',
  `team_id`         varchar(8)   NOT NULL COMMENT '拼单组队ID',
  `notify_category` varchar(64)           DEFAULT NULL COMMENT '回调种类',
  `notify_type`     varchar(8)   NOT NULL DEFAULT 'MQ' COMMENT '回调类型（HTTP / MQ）',
  `notify_mq`       varchar(32)           DEFAULT NULL COMMENT 'MQ topic',
  `notify_url`      varchar(128)          DEFAULT NULL COMMENT 'HTTP 回调接口',
  `notify_count`    int          NOT NULL COMMENT '回调次数',
  `notify_status`   tinyint(1)   NOT NULL COMMENT '回调状态：0初始、1完成、2重试、3失败',
  `parameter_json`  varchar(256) NOT NULL COMMENT '回调参数',
  `uuid`            varchar(128) NOT NULL COMMENT '幂等唯一标识',
  `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='回调通知任务';

-- ------------------------------------------------------------
-- 开发/联调用拼团样例：一团已成团、一团进行中（与活动 100123/100124、渠道 s01/c01 对齐）
-- 订单号可与 order_service.t_order 中拼团/普通单对照调试
-- ------------------------------------------------------------
INSERT INTO `team_order` (
  `team_id`, `activity_id`, `source`, `channel`, `original_price`, `deduction_price`, `pay_price`,
  `target_count`, `complete_count`, `lock_count`, `status`, `valid_start_time`, `valid_end_time`, `notify_type`, `notify_url`
) VALUES
  ('TGB10001', 100123, 's01', 'c01', 199.00, 20.00, 179.00, 3, 3, 0, 1, '2026-04-18 10:00:00', '2026-04-18 10:20:00', 'MQ', NULL),
  ('TGB10002', 100123, 's01', 'c01', 199.00, 20.00, 179.00, 3, 1, 2, 0, '2026-04-22 12:00:00', '2026-04-22 12:20:00', 'MQ', NULL),
  ('TGB20001', 100124, 's01', 'c01',  69.00, 30.00,  39.00, 2, 2, 0, 1, '2026-04-20 09:00:00', '2026-04-20 09:30:00', 'MQ', NULL);

INSERT INTO `t_order` (
  `order_id`, `user_id`, `team_id`, `activity_id`, `goods_id`, `out_trade_no`, `status`,
  `original_price`, `deduction_price`, `pay_price`, `out_trade_time`, `start_time`, `end_time`
) VALUES
  -- 已成团 TGB10001（3 人全支付）
  ('901300100001', 'xfg01',  'TGB10001', 100123, '1001', '200000000001', 1, 199.00, 20.00, 179.00, '2026-04-18 10:02:00', '2026-04-18 10:00:00', '2026-04-18 10:20:00'),
  ('901300100002', 'xfg02',  'TGB10001', 100123, '1001', '200000000002', 1, 199.00, 20.00, 179.00, '2026-04-18 10:04:00', '2026-04-18 10:00:00', '2026-04-18 10:20:00'),
  ('901300100003', 'xiaofuge','TGB10001', 100123, '1001', '200000000003', 1, 199.00, 20.00, 179.00, '2026-04-18 10:05:00', '2026-04-18 10:00:00', '2026-04-18 10:20:00'),
  -- 拼团中 TGB10002（1 人付、2 人锁单）
  ('901300200001', 'xfg04',  'TGB10002', 100123, '1003', '200000000010', 1, 159.00, 20.00, 139.00, '2026-04-22 12:01:00', '2026-04-22 12:00:00', '2026-04-22 12:20:00'),
  ('901300200002', 'xfg05',  'TGB10002', 100123, '1003', '200000000011', 0, 159.00, 20.00, 139.00, NULL,            '2026-04-22 12:00:00', '2026-04-22 12:20:00'),
  ('901300200003', 'xfg06',  'TGB10002', 100123, '1003', '200000000012', 0, 159.00, 20.00, 139.00, NULL,            '2026-04-22 12:00:00', '2026-04-22 12:20:00'),
  -- 存储拼团 2 人成团
  ('901300300001', 'xfg07',  'TGB20001', 100124, '2004', '200000000020', 1, 69.00, 30.00, 39.00, '2026-04-20 09:05:00', '2026-04-20 09:00:00', '2026-04-20 09:30:00'),
  ('901300300002', 'xfg08',  'TGB20001', 100124, '2004', '200000000021', 1, 69.00, 30.00, 39.00, '2026-04-20 09:08:00', '2026-04-20 09:00:00', '2026-04-20 09:30:00');

INSERT INTO `notify_task` (
  `activity_id`, `team_id`, `notify_category`, `notify_type`, `notify_mq`, `notify_url`, `notify_count`, `notify_status`, `parameter_json`, `uuid`
) VALUES
  (100123, 'TGB10001', 'GROUP_COMPLETE', 'MQ', 'group_settle', NULL, 1, 1, '{"team_id":"TGB10001","activity_id":100123}', 'uuid-tgb10001-001'),
  (100124, 'TGB20001', 'GROUP_COMPLETE', 'MQ', 'group_settle', NULL, 1, 1, '{"team_id":"TGB20001","activity_id":100124}', 'uuid-tgb20001-001');


-- ------------------------------------------------------------
-- category：商品类目
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id`          int unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `name`        varchar(32)  NOT NULL COMMENT '类目名称，如：AI模型',
  `code`        varchar(32)  NOT NULL COMMENT '类目编码，如：ai_model',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品类目';

INSERT INTO `category` (`id`, `name`, `code`)
VALUES
  (1, 'AI模型',   'ai_model'),
  (2, '存储资源', 'storage');


-- ------------------------------------------------------------
-- sku：商品信息（含展示字段，供拼团前端直接展示，无需请求 mall 服务）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sku`;
CREATE TABLE `sku` (
  `id`              int unsigned   NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `goods_id`        varchar(16)    NOT NULL COMMENT '商品ID',
  `goods_name`      varchar(128)   NOT NULL COMMENT '商品名称',
  `goods_image_url` varchar(512)            DEFAULT NULL COMMENT '商品图片URL',
  `goods_detail`    text                    COMMENT '商品详情介绍',
  `original_price`  decimal(10,2)  NOT NULL COMMENT '商品价格',
  `category_id`     int unsigned   NOT NULL COMMENT '所属类目ID',
  `create_time`     datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_goods_id` (`goods_id`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品信息';

INSERT INTO `sku` (`goods_id`, `goods_name`, `goods_image_url`, `goods_detail`, `original_price`, `category_id`)
VALUES
  ('1001', 'GPT-4 100万Token',     NULL, NULL, 199.00, 1),
  ('1003', 'Claude 100万Token',    NULL, NULL, 159.00, 1),
  ('1005', '通义千问 100万Token',  NULL, NULL,  89.00, 1),
  ('2002', '云存储 200MB',         NULL, NULL,  18.00, 2),
  ('2004', '云存储 1GB',           NULL, NULL,  69.00, 2),
  ('2005', '云存储 1TB',           NULL, NULL, 299.00, 2);


-- ------------------------------------------------------------
-- sku_resource_detail：商品资源详情（AI Token 模型名/额度，存储容量等）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sku_resource_detail`;
CREATE TABLE `sku_resource_detail` (
  `id`        int unsigned  NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `goods_id`  varchar(16)   NOT NULL COMMENT '关联 sku.goods_id',
  `res_key`   varchar(32)   NOT NULL COMMENT '资源键（模型名 / storage）',
  `res_value` varchar(128)  NOT NULL COMMENT '资源值（token 数 / 容量数值）',
  `unit`      varchar(16)            DEFAULT NULL COMMENT '单位：token / MB / GB',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_goods_res` (`goods_id`, `res_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品资源详情';

INSERT INTO `sku_resource_detail` (`goods_id`, `res_key`, `res_value`, `unit`)
VALUES
  ('1001', 'gpt-4',   '1000000',  'token'),
  ('1003', 'claude-3','1000000',  'token'),
  ('1005', 'qwen',    '1000000',  'token'),
  ('2002', 'storage', '200',      'MB'),
  ('2004', 'storage', '1024',     'MB'),
  ('2005', 'storage', '1048576',  'MB');


-- ------------------------------------------------------------
-- ALTER TABLE：给已存在的 discount 表添加/修正 discount_type 字段
-- ------------------------------------------------------------
-- ALTER TABLE `discount`
--   ADD COLUMN IF NOT EXISTS `discount_type` tinyint(1) NOT NULL DEFAULT '0' COMMENT '折扣类型（0:base、1:tag）' AFTER `discount_desc`,
--   MODIFY COLUMN `discount_type` tinyint(1) NOT NULL DEFAULT '0' COMMENT '折扣类型（0:base、1:tag）';


