CREATE DATABASE IF NOT EXISTS `test_group_buy_service` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `test_group_buy_service`;

DROP TABLE IF EXISTS `group_buy_discount`;
CREATE TABLE `group_buy_discount` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `discount_id` varchar(8) NOT NULL,
  `discount_name` varchar(64) NOT NULL,
  `discount_desc` varchar(256) NOT NULL,
  `discount_type` tinyint(1) NOT NULL DEFAULT '0',
  `market_plan` varchar(4) NOT NULL DEFAULT 'ZJ',
  `market_expr` varchar(32) NOT NULL,
  `tag_id` varchar(8) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_discount_id` (`discount_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='折扣配置';

INSERT INTO `group_buy_discount` (`discount_id`, `discount_name`, `discount_desc`, `discount_type`, `market_plan`, `market_expr`, `tag_id`)
VALUES ('25120207', '测试拼团直减20', '测试拼团立减20元', 0, 'ZJ', '20', NULL);

DROP TABLE IF EXISTS `group_buy_activity`;
CREATE TABLE `group_buy_activity` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `activity_id` bigint NOT NULL,
  `activity_name` varchar(128) NOT NULL,
  `discount_id` varchar(8) NOT NULL,
  `group_type` tinyint(1) NOT NULL DEFAULT 0,
  `take_limit_count` int NOT NULL DEFAULT 1,
  `target` int NOT NULL DEFAULT 1,
  `valid_time` int NOT NULL DEFAULT 15,
  `status` tinyint(1) NOT NULL DEFAULT 0,
  `start_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `end_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `tag_id` varchar(8) DEFAULT NULL,
  `tag_scope` varchar(4) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_activity_id` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='拼团活动';

INSERT INTO `group_buy_activity` (`activity_id`, `activity_name`, `discount_id`, `group_type`, `take_limit_count`, `target`, `valid_time`, `status`, `start_time`, `end_time`)
VALUES (100123, '测试拼团活动', '25120207', 0, 1, 3, 15, 1, '2000-01-01 00:00:00', '2099-12-31 23:59:59');

DROP TABLE IF EXISTS `sc_sku_activity`;
CREATE TABLE `sc_sku_activity` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `source` varchar(8) NOT NULL,
  `channel` varchar(8) NOT NULL,
  `activity_id` bigint NOT NULL,
  `activity_type` varchar(32) NOT NULL,
  `goods_id` varchar(16) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_sc_goods` (`source`, `channel`, `goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='渠道商品活动映射（拼团）';

INSERT INTO `sc_sku_activity` (`source`, `channel`, `activity_id`, `activity_type`, `goods_id`)
VALUES ('s01', 'c01', 100123, 'group_buy', '1001');

DROP TABLE IF EXISTS `team_order`;
CREATE TABLE `team_order` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `team_id` varchar(8) NOT NULL,
  `activity_id` bigint NOT NULL,
  `source` varchar(8) NOT NULL,
  `channel` varchar(8) NOT NULL,
  `original_price` decimal(8,2) NOT NULL,
  `deduction_price` decimal(8,2) NOT NULL,
  `pay_price` decimal(8,2) NOT NULL,
  `target_count` int NOT NULL,
  `complete_count` int NOT NULL,
  `lock_count` int NOT NULL,
  `status` tinyint(1) NOT NULL DEFAULT 0,
  `valid_start_time` datetime NOT NULL,
  `valid_end_time` datetime NOT NULL,
  `notify_type` varchar(8) NOT NULL DEFAULT 'MQ',
  `notify_url` varchar(512) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_team_id` (`team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='拼团团队订单';

INSERT INTO `team_order` (`team_id`, `activity_id`, `source`, `channel`, `original_price`, `deduction_price`, `pay_price`, `target_count`, `complete_count`, `lock_count`, `status`, `valid_start_time`, `valid_end_time`)
VALUES ('88888888', 100123, 's01', 'c01', 100.00, 20.00, 80.00, 3, 0, 1, 0, '2000-01-01 00:00:00', '2099-12-31 23:59:59');

DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order` (
  `order_id` varchar(12) NOT NULL,
  `user_id` varchar(64) NOT NULL,
  `team_id` varchar(8) NOT NULL,
  `activity_id` bigint NOT NULL,
  `goods_id` varchar(16) NOT NULL,
  `out_trade_no` varchar(32) NOT NULL,
  `status` tinyint(1) NOT NULL DEFAULT 0,
  `original_price` decimal(8,2) NOT NULL,
  `deduction_price` decimal(8,2) NOT NULL,
  `pay_price` decimal(8,2) NOT NULL,
  `out_trade_time` datetime DEFAULT NULL,
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`order_id`),
  KEY `idx_team_id` (`team_id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_user_activity` (`user_id`, `activity_id`),
  KEY `idx_out_trade_no` (`out_trade_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='拼团个人订单';

INSERT INTO `t_order` (`order_id`, `user_id`, `team_id`, `activity_id`, `goods_id`, `out_trade_no`, `status`, `original_price`, `deduction_price`, `pay_price`, `start_time`, `end_time`)
VALUES ('TESTGB000001', 'test-user', '88888888', 100123, '1001', 'TEST_OUT_000001', 0, 100.00, 20.00, 80.00, '2000-01-01 00:00:00', '2099-12-31 23:59:59');

DROP TABLE IF EXISTS `notify_task`;
CREATE TABLE `notify_task` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `activity_id` bigint NOT NULL,
  `team_id` varchar(8) NOT NULL,
  `notify_category` varchar(64) DEFAULT NULL,
  `notify_type` varchar(8) NOT NULL DEFAULT 'MQ',
  `notify_mq` varchar(32) DEFAULT NULL,
  `notify_url` varchar(128) DEFAULT NULL,
  `notify_count` int NOT NULL,
  `notify_status` tinyint(1) NOT NULL,
  `parameter_json` varchar(256) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='回调通知任务';

DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(32) NOT NULL,
  `code` varchar(32) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品类目';

INSERT INTO `category` (`id`, `name`, `code`) VALUES (1, 'AI模型', 'ai_model');

DROP TABLE IF EXISTS `sku`;
CREATE TABLE `sku` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `goods_id` varchar(16) NOT NULL,
  `goods_name` varchar(64) NOT NULL,
  `goods_image_url` varchar(512) NOT NULL,
  `goods_detail` varchar(1024) DEFAULT NULL,
  `category_id` int unsigned DEFAULT NULL,
  `original_price` decimal(8,2) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品SKU';

INSERT INTO `sku` (`goods_id`, `goods_name`, `goods_image_url`, `goods_detail`, `category_id`, `original_price`)
VALUES ('1001', '测试商品', 'https://example.com/test.png', 'test sku', 1, 100.00);

DROP TABLE IF EXISTS `sku_resource_detail`;
CREATE TABLE `sku_resource_detail` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `goods_id` varchar(16) NOT NULL,
  `res_key` varchar(32) NOT NULL,
  `res_value` varchar(512) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SKU资源明细';

INSERT INTO `sku_resource_detail` (`goods_id`, `res_key`, `res_value`)
VALUES ('1001', 'resource_url', 'https://example.com/resource');
