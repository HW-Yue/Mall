CREATE DATABASE IF NOT EXISTS `test_seckill_service` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `test_seckill_service`;

DROP TABLE IF EXISTS `seckill_activity`;
CREATE TABLE `seckill_activity` (
  `id`               bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增',
  `activity_id`      bigint          NOT NULL COMMENT '活动ID',
  `activity_name`    varchar(128)    NOT NULL COMMENT '活动名称',
  `seckill_price`    decimal(10,2)   NOT NULL COMMENT '活动一口价',
  `take_limit_count` int             NOT NULL DEFAULT 1 COMMENT '每人秒杀次数上限',
  `status`           tinyint(1)      NOT NULL DEFAULT 0 COMMENT '状态：0创建、1生效、2过期、3废弃',
  `start_time`       datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '活动开始时间',
  `end_time`         datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '活动结束时间',
  `tag_id`           varchar(8)               DEFAULT NULL COMMENT '人群标签规则标识',
  `tag_scope`        varchar(4)               DEFAULT NULL COMMENT '人群范围（1可见限制、2参与限制）',
  `create_time`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_activity_id` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='秒杀活动';

INSERT INTO `seckill_activity` (`activity_id`, `activity_name`, `seckill_price`, `take_limit_count`, `status`, `start_time`, `end_time`)
VALUES
  (200001, 'AI模型整点秒杀', 79.00, 1, 1, '2000-01-01 00:00:00', '2099-12-31 23:59:59'),
  (200002, '存储资源限时秒杀', 10.00, 2, 1, '2000-01-01 00:00:00', '2099-12-31 23:59:59');

DROP TABLE IF EXISTS `sc_sku_activity`;
CREATE TABLE `sc_sku_activity` (
  `id`            int unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `source`        varchar(8)   NOT NULL COMMENT '渠道',
  `channel`       varchar(8)   NOT NULL COMMENT '来源',
  `activity_id`   bigint       NOT NULL COMMENT '活动ID',
  `activity_type` varchar(32)  NOT NULL COMMENT '活动类型（固定为 seckill）',
  `sku_id`        varchar(16)  NOT NULL COMMENT 'SKU ID',
  `stock_count`   int          NOT NULL DEFAULT 0 COMMENT '活动商品库存',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_sc_sku` (`source`, `channel`, `sku_id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_activity_type` (`activity_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='渠道商品活动映射（秒杀）';

INSERT INTO `sc_sku_activity` (`source`, `channel`, `activity_id`, `activity_type`, `sku_id`, `stock_count`)
VALUES
  ('s01', 'c01', 200001, 'seckill', '1001', 5000),
  ('s01', 'c01', 200001, 'seckill', '1003', 5000),
  ('s01', 'c01', 200001, 'seckill', '1005', 5000),
  ('s01', 'c01', 200002, 'seckill', '2002', 5000),
  ('s01', 'c01', 200002, 'seckill', '2004', 5000),
  ('s01', 'c01', 200002, 'seckill', '2005', 5000);

DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id`          int unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `name`        varchar(32)  NOT NULL COMMENT '类目名称',
  `code`        varchar(32)  NOT NULL COMMENT '类目编码',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品类目';

INSERT INTO `category` (`id`, `name`, `code`)
VALUES
  (1, 'AI模型', 'ai_model'),
  (2, '存储资源', 'storage');

DROP TABLE IF EXISTS `sms_seckill_sku`;
CREATE TABLE `sms_seckill_sku` (
  `id`              int unsigned   NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `sku_id`          varchar(16)    NOT NULL COMMENT 'SKU ID',
  `spu_id`          varchar(16)    NOT NULL COMMENT 'SPU ID',
  `spu_name`        varchar(128)   NOT NULL COMMENT 'SPU名称',
  `sku_spec_json`   varchar(512)   NOT NULL COMMENT 'SKU规格JSON',
  `goods_image_url` varchar(512)            DEFAULT NULL COMMENT '商品图片URL',
  `goods_detail`    text                    COMMENT '商品详情介绍',
  `original_price`  decimal(10,2)  NOT NULL COMMENT '商品原价',
  `category_id`     int unsigned   NOT NULL COMMENT '所属类目ID',
  `create_time`     datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='秒杀商品SKU快照';

INSERT INTO `sms_seckill_sku` (`sku_id`, `spu_id`, `spu_name`, `sku_spec_json`, `original_price`, `category_id`)
VALUES
  ('1001', 'SP001', 'GPT-4', '{"token":"100万"}', 199.00, 1),
  ('1003', 'SP002', 'Claude', '{"token":"100万"}', 159.00, 1),
  ('1005', 'SP003', '通义千问', '{"token":"100万"}', 89.00, 1),
  ('2002', 'SP004', '云存储', '{"capacity":"200MB"}', 18.00, 2),
  ('2004', 'SP004', '云存储', '{"capacity":"1GB"}', 69.00, 2),
  ('2005', 'SP004', '云存储', '{"capacity":"1TB"}', 299.00, 2);
