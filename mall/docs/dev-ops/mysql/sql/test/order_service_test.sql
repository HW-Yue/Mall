CREATE DATABASE IF NOT EXISTS `test_order_service` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `test_order_service`;

DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `order_id` varchar(12) NOT NULL COMMENT '订单ID',
  `user_id` varchar(64) NOT NULL COMMENT '用户ID',
  `goods_id` varchar(16) NOT NULL COMMENT '商品ID',
  `goods_name` varchar(128) NOT NULL COMMENT '商品名称',
  `goods_image_url` varchar(512) DEFAULT NULL COMMENT '商品图片URL',
  `source` varchar(8) NOT NULL COMMENT '渠道',
  `channel` varchar(8) NOT NULL COMMENT '来源',
  `original_price` decimal(10,2) NOT NULL COMMENT '原始价格',
  `deduction_price` decimal(10,2) NOT NULL DEFAULT 0 COMMENT '折扣金额',
  `pay_price` decimal(10,2) NOT NULL COMMENT '实付金额',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '状态：0锁定、1支付完成、2退单',
  `out_trade_no` varchar(64) NOT NULL COMMENT '外部交易单号',
  `out_trade_time` datetime DEFAULT NULL COMMENT '外部交易时间',
  `biz_id` varchar(128) NOT NULL COMMENT '业务唯一ID',
  `notify_type` varchar(8) NOT NULL DEFAULT 'MQ' COMMENT '回调类型',
  `notify_url` varchar(512) DEFAULT NULL COMMENT 'HTTP 回调地址',
  `pay_url` text COMMENT '支付链接',
  `market_type` varchar(32) NOT NULL COMMENT '营销类型',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_order_id` (`order_id`),
  UNIQUE KEY `uq_user_out_trade_no` (`user_id`, `out_trade_no`),
  KEY `idx_out_trade_no` (`out_trade_no`),
  KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单主表';

INSERT INTO `t_order` (`order_id`, `user_id`, `goods_id`, `goods_name`, `source`, `channel`, `original_price`, `deduction_price`, `pay_price`, `status`, `out_trade_no`, `biz_id`, `notify_type`, `market_type`)
VALUES ('TEST00000001', 'test-user', '1001', '测试商品', 's01', 'c01', 100.00, 20.00, 80.00, 0, 'TEST_OUT_000001', 'TEST00000001', 'MQ', 'group_buy');
