-- ============================================================
-- seckill_service：秒杀营销服务独立库
--
-- 包含：
--   秒杀活动（seckill_activity，一口价）
--   渠道-商品-活动映射（sc_sku_activity，含库存）
--   商品快照（sms_seckill_sku）
--
-- 运行期说明：
--   1. 秒杀下单先扣 Redis 可售库存，支付成功后再扣 Redis 真实库存并异步落 MySQL
--   2. 超时关单 / 取消订单只恢复可售库存，不回滚真实库存
--   3. seckillToken、orderId -> token / orderMeta 属于 Redis 运行态元数据，不落库
-- ============================================================

CREATE DATABASE IF NOT EXISTS `seckill_service` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `seckill_service`;

-- ------------------------------------------------------------
-- seckill_activity：秒杀活动
-- ------------------------------------------------------------
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
  UNIQUE KEY `uq_activity_id` (`activity_id`),
  CONSTRAINT `chk_seckill_price_non_negative` CHECK (`seckill_price` >= 0),
  CONSTRAINT `chk_take_limit_count_positive` CHECK (`take_limit_count` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='秒杀活动';

INSERT INTO `seckill_activity` (`activity_id`, `activity_name`, `seckill_price`, `take_limit_count`, `status`, `start_time`, `end_time`, `tag_id`, `tag_scope`)
VALUES
  (200001, 'AI模型整点秒杀',   79.00, 1, 1, '2024-12-07 10:00:00', '2029-12-07 10:00:00', NULL, NULL),
  (200002, '存储资源限时秒杀', 10.00, 2, 1, NOW(), DATE_ADD(NOW(), INTERVAL 1 YEAR), NULL, NULL);


-- ------------------------------------------------------------
-- sc_sku_activity：渠道-商品-活动映射（秒杀条目）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sc_sku_activity`;
CREATE TABLE `sc_sku_activity` (
  `id`            int unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `source`        varchar(8)   NOT NULL COMMENT '渠道',
  `channel`       varchar(8)   NOT NULL COMMENT '来源',
  `activity_id`   bigint       NOT NULL COMMENT '活动ID',
  `activity_type` varchar(32)  NOT NULL COMMENT '活动类型（固定为 seckill）',
  `sku_id`        varchar(16)  NOT NULL COMMENT 'SKU ID（关联 sms_seckill_sku.sku_id）',
  `stock_count`   int          NOT NULL DEFAULT 0 COMMENT '活动商品库存',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_sc_sku` (`source`, `channel`, `sku_id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_activity_type` (`activity_type`),
  CONSTRAINT `chk_stock_count_non_negative` CHECK (`stock_count` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='渠道商品活动映射（秒杀）';

INSERT INTO `sc_sku_activity` (`source`, `channel`, `activity_id`, `activity_type`, `sku_id`, `stock_count`)
VALUES
  ('s01', 'c01', 200001, 'seckill', '1001', 100),
  ('s01', 'c01', 200001, 'seckill', '1003', 100),
  ('s01', 'c01', 200001, 'seckill', '1005', 100),
  ('s01', 'c01', 200002, 'seckill', '2002', 50),
  ('s01', 'c01', 200002, 'seckill', '2004', 50),
  ('s01', 'c01', 200002, 'seckill', '2005', 50);

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
-- sms_seckill_sku：秒杀商品SKU快照
--   冗余存储 mall 主库的 SPU/SKU 数据，供秒杀前端直接展示，无需请求 mall 服务。
--   sku_id / spu_id 对应 mall 主库 t_sku / t_spu 的主键，仅作关联参考。
--   前端展示商品名称 = spu_name + sku_spec_json 拼接（由前端/接口层处理）。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sms_seckill_sku`;
CREATE TABLE `sms_seckill_sku` (
  `id`              int unsigned   NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `sku_id`          varchar(16)    NOT NULL COMMENT 'SKU ID（对应 mall.t_sku.sku_id）',
  `spu_id`          varchar(16)    NOT NULL COMMENT 'SPU ID（对应 mall.t_spu.spu_id）',
  `spu_name`        varchar(128)   NOT NULL COMMENT 'SPU名称（商品主名称，如：GPT-4）',
  `sku_spec_json`   varchar(512)   NOT NULL COMMENT 'SKU规格JSON（如：{"token":"100万"}）',
  `goods_image_url` varchar(512)            DEFAULT NULL COMMENT '商品图片URL',
  `goods_detail`    text                    COMMENT '商品详情介绍',
  `original_price`  decimal(10,2)  NOT NULL COMMENT '商品原价',
  `category_id`     int unsigned   NOT NULL COMMENT '所属类目ID',
  `create_time`     datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_sku_id` (`sku_id`),
  KEY `idx_spu_id` (`spu_id`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='秒杀商品SKU快照（冗余存储，无需请求mall服务）';

INSERT INTO `sms_seckill_sku` (`sku_id`, `spu_id`, `spu_name`, `sku_spec_json`, `goods_image_url`, `goods_detail`, `original_price`, `category_id`)
VALUES
  ('1001', 'SP001', 'GPT-4',    '{"token":"100万"}', NULL, NULL, 199.00, 1),
  ('1003', 'SP002', 'Claude',   '{"token":"100万"}', NULL, NULL, 159.00, 1),
  ('1005', 'SP003', '通义千问', '{"token":"100万"}', NULL, NULL,  89.00, 1),
  ('2002', 'SP004', '云存储',   '{"capacity":"200MB"}', NULL, NULL,  18.00, 2),
  ('2004', 'SP004', '云存储',   '{"capacity":"1GB"}',   NULL, NULL,  69.00, 2),
  ('2005', 'SP004', '云存储',   '{"capacity":"1TB"}',   NULL, NULL, 299.00, 2);


-- ------------------------------------------------------------
-- 说明：旧 discount 表已移除
-- ------------------------------------------------------------
