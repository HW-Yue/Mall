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
