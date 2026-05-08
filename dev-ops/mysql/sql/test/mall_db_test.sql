CREATE DATABASE IF NOT EXISTS `test_mall_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `test_mall_db`;

DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id`          int unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `name`        varchar(32)  NOT NULL COMMENT '类目名称',
  `code`        varchar(32)  NOT NULL COMMENT '类目编码',
  `icon_url`    varchar(255)          DEFAULT NULL COMMENT '分类图标',
  `sort_order`  int                   DEFAULT 0 COMMENT '排序权重',
  `status`      tinyint(1)            DEFAULT 1 COMMENT '是否启用',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品类目表';

INSERT INTO `category` (`id`, `name`, `code`, `sort_order`, `status`)
VALUES
  (1, 'AI模型', 'ai_model', 10, 1),
  (2, '存储资源', 'storage', 20, 1);

DROP TABLE IF EXISTS `sku`;
CREATE TABLE `sku` (
  `id`              int unsigned   NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `goods_id`        varchar(16)    NOT NULL COMMENT '商品ID',
  `goods_name`      varchar(128)   NOT NULL COMMENT '商品名称',
  `goods_image_url` varchar(512)            DEFAULT NULL COMMENT '商品图片URL',
  `goods_detail`    text                    COMMENT '商品详情介绍',
  `original_price`  decimal(10,2)  NOT NULL COMMENT '商品官方定价',
  `category_id`     int unsigned   NOT NULL COMMENT '所属类目ID',
  `total_stock`     int unsigned   NOT NULL DEFAULT 0 COMMENT '总库存',
  `locked_stock`    int unsigned   NOT NULL DEFAULT 0 COMMENT '锁定量',
  `create_time`     datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_goods_id` (`goods_id`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品信息';

INSERT INTO `sku` (`goods_id`, `goods_name`, `original_price`, `category_id`, `total_stock`, `locked_stock`)
VALUES
  ('1001', 'GPT-4 100万Token',   199.00, 1, 5000, 0),
  ('1002', 'GPT-4 500万Token',   899.00, 1, 5000, 0),
  ('1003', 'Claude 100万Token',  159.00, 1, 5000, 0),
  ('1004', '文心一言 200万Token',  99.00, 1, 5000, 0),
  ('1005', '通义千问 100万Token',  89.00, 1, 5000, 0),
  ('2001', '云存储 100MB',         9.90, 2, 8000, 0),
  ('2002', '云存储 200MB',        18.00, 2, 8000, 0),
  ('2003', '云存储 500MB',        39.00, 2, 8000, 0),
  ('2004', '云存储 1GB',          69.00, 2, 8000, 0),
  ('2005', '云存储 1TB',         299.00, 2, 8000, 0);

DROP TABLE IF EXISTS `sku_resource_detail`;
CREATE TABLE `sku_resource_detail` (
  `id`        int unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `goods_id`  varchar(16)  NOT NULL COMMENT '关联sku.goods_id',
  `res_key`   varchar(32)  NOT NULL COMMENT '模型时=模型名；存储时=storage',
  `res_value` varchar(128) NOT NULL COMMENT '模型时=token数；存储时=容量数值',
  `unit`      varchar(16)           DEFAULT NULL COMMENT '单位: token, MB, GB',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_goods_res` (`goods_id`, `res_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品资源详情表';

INSERT INTO `sku_resource_detail` (`goods_id`, `res_key`, `res_value`, `unit`)
VALUES
  ('1001', 'gpt-4',    '1000000', 'token'),
  ('1002', 'gpt-4',    '5000000', 'token'),
  ('1003', 'claude-3', '1000000', 'token'),
  ('1004', 'wenxin',   '2000000', 'token'),
  ('1005', 'qwen',     '1000000', 'token'),
  ('2001', 'storage',  '100',     'MB'),
  ('2002', 'storage',  '200',     'MB'),
  ('2003', 'storage',  '500',     'MB'),
  ('2004', 'storage',  '1024',    'MB'),
  ('2005', 'storage',  '1048576', 'MB');

DROP TABLE IF EXISTS `activity_type`;
CREATE TABLE `activity_type` (
  `id`          int unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `type_name`   varchar(32)  NOT NULL COMMENT '活动名称',
  `type_code`   varchar(32)  NOT NULL COMMENT '活动编码',
  `status`      tinyint(1)            DEFAULT 1 COMMENT '状态（0禁用 1启用）',
  `create_time` datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_type_code` (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='活动类型字典表';

INSERT INTO `activity_type` (`id`, `type_name`, `type_code`, `status`)
VALUES
  (1, '秒杀', 'seckill', 1),
  (2, '拼团', 'group_buy', 1);
