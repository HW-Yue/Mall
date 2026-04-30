-- ============================================================
-- order_service：订单服务独立库
-- 只存用户订单记录，冗余 goods_name 避免跨服务查询
-- ============================================================

CREATE DATABASE IF NOT EXISTS `order_service` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `order_service`;

-- ------------------------------------------------------------
-- t_order：订单主表
--
-- 设计说明：
--   1. goods_name 冗余商品名称 — 下单时写入，查询我的订单无需跨服务
--   2. market_type 区分营销类型（normal / group_buy / seckill）
--   3. pay_url 存支付链接，避免重复调 pay 服务
--   4. 跨服务扩展字段（team_id / seckill_id 等）各营销服务自行维护扩展表，
--      通过 order_id 关联，不写入本表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order` (
  `id`              bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `order_id`        varchar(12)     NOT NULL COMMENT '订单ID',
  `user_id`         varchar(64)     NOT NULL COMMENT '用户ID',
  `goods_id`        varchar(16)     NOT NULL COMMENT '商品ID',
  `goods_name`      varchar(128)    NOT NULL COMMENT '商品名称（冗余，下单时写入，避免跨服务查询）',
  `goods_image_url` varchar(512)             DEFAULT NULL  COMMENT '商品图片URL（冗余，下单时写入）',
  `source`          varchar(8)      NOT NULL COMMENT '渠道',
  `channel`         varchar(8)      NOT NULL COMMENT '来源',
  `original_price`  decimal(10,2)   NOT NULL COMMENT '原始价格',
  `deduction_price` decimal(10,2)   NOT NULL DEFAULT 0 COMMENT '折扣金额',
  `pay_price`       decimal(10,2)   NOT NULL COMMENT '实付金额',
  `status`          tinyint(1)      NOT NULL DEFAULT 0 COMMENT '状态：0锁定待支付、1支付成功、2超时关闭、3待退款、4已退款',
  `out_trade_no`    varchar(64)     NOT NULL COMMENT '外部交易单号（幂等键）',
  `out_trade_time`  datetime                 DEFAULT NULL COMMENT '外部交易时间',
  `biz_id`          varchar(128)    NOT NULL COMMENT '业务唯一ID',
  `notify_type`     varchar(8)      NOT NULL DEFAULT 'MQ'  COMMENT '回调类型（HTTP / MQ）',
  `notify_url`      varchar(512)             DEFAULT NULL  COMMENT 'HTTP 回调地址',
  `pay_url`         text                     COMMENT '支付链接（拿到后缓存，避免重复请求）',
  `market_type`     varchar(32)     NOT NULL COMMENT '营销类型：normal / group_buy / seckill',
  `create_time`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_order_id` (`order_id`),
  UNIQUE KEY `uq_user_out_trade_no` (`user_id`, `out_trade_no`),
  KEY `idx_out_trade_no` (`out_trade_no`),
  KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单主表';

-- ------------------------------------------------------------
-- 开发/联调用示例订单（与 mall_db 商品 1001–2005、渠道 s01/c01 对齐）
-- status：0 待支付、1 已支付、2 超时关单、3 待退款、4 已退款
-- biz_id 规则：{goods_id}_{user_id}_{out_trade_no}
-- ------------------------------------------------------------
INSERT INTO `t_order` (
  `order_id`, `user_id`, `goods_id`, `goods_name`, `goods_image_url`,
  `source`, `channel`, `original_price`, `deduction_price`, `pay_price`, `status`,
  `out_trade_no`, `out_trade_time`, `biz_id`, `notify_type`, `notify_url`, `pay_url`, `market_type`, `create_time`
) VALUES
  ('901111000001', 'xfg_user_01', '1001', 'GPT-4 100万Token', NULL, 's01', 'c01', 199.00, 0.00, 199.00, 1,
   '100000000001', '2026-04-20 10:00:00', '1001_xfg_user_01_100000000001', 'MQ', NULL, NULL, 'normal',    '2026-04-20 09:55:00'),
  ('901111000002', 'xfg_user_02', '1002', 'GPT-4 500万Token', NULL, 's01', 'c01', 899.00, 0.00, 899.00, 0,
   '100000000002', NULL,                 '1002_xfg_user_02_100000000002', 'MQ', NULL, NULL, 'normal',    '2026-04-21 11:00:00'),
  ('901111000003', 'xfg_user_03', '1001', 'GPT-4 100万Token', NULL, 's01', 'c01', 199.00, 20.00, 179.00, 1,
   '100000000003', '2026-04-18 15:30:00', '1001_xfg_user_03_100000000003', 'MQ', NULL, NULL, 'group_buy', '2026-04-18 15:20:00'),
  ('901111000004', 'xfg_user_04', '1003', 'Claude 100万Token', NULL, 's01', 'c01', 159.00, 50.00, 109.00, 2,
   '100000000004', NULL,                 '1003_xfg_user_04_100000000004', 'MQ', NULL, NULL, 'seckill',  '2026-04-10 12:00:00'),
  ('901111000005', 'xfg_user_05', '2004', '云存储 1GB',         NULL, 's01', 'c01',  69.00, 30.00,  39.00, 1,
   '100000000005', '2026-04-19 08:00:00', '2004_xfg_user_05_100000000005', 'MQ', NULL, NULL, 'seckill',  '2026-04-19 07:50:00'),
  ('901111000006', 'xiaofuge',     '1005', '通义千问 100万Token', NULL, 's01', 'c01',  89.00, 20.00,  69.00, 4,
   '100000000006', '2026-04-15 16:00:00', '1005_xiaofuge_100000000006',    'MQ', NULL, NULL, 'group_buy','2026-04-15 15:00:00'),
  ('901111000007', 'liergou',      '2002', '云存储 200MB',       NULL, 's01', 'c01',  18.00,  3.00,  15.00, 0,
   '100000000007', NULL,                 '2002_liergou_100000000007',     'MQ', NULL, NULL, 'group_buy','2026-04-22 14:00:00'),
  ('901111000008', 'xfg03',        '2005', '云存储 1TB',         NULL, 's01', 'c01', 299.00, 30.00, 269.00, 3,
   '100000000008', NULL,                 '2005_xfg03_100000000008',       'MQ', NULL, NULL, 'group_buy','2026-04-22 16:00:00');
