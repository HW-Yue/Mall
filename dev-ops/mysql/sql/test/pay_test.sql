CREATE DATABASE IF NOT EXISTS `test_pay` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `test_pay`;

DROP TABLE IF EXISTS `pay_order`;
CREATE TABLE `pay_order` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(64) NOT NULL COMMENT '用户ID',
  `product_id` varchar(16) NOT NULL COMMENT '商品ID',
  `product_name` varchar(64) NOT NULL COMMENT '商品名称',
  `order_id` varchar(64) NOT NULL COMMENT '订单ID',
  `order_time` datetime NOT NULL COMMENT '下单时间',
  `total_amount` decimal(8,2) unsigned DEFAULT NULL COMMENT '订单金额',
  `status` varchar(32) NOT NULL COMMENT '订单状态',
  `pay_url` varchar(2014) DEFAULT NULL COMMENT '支付信息',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `market_type` varchar(32) DEFAULT NULL COMMENT '营销类型',
  `market_deduction_amount` decimal(8,2) DEFAULT NULL COMMENT '营销金额',
  `pay_amount` decimal(8,2) NOT NULL COMMENT '支付金额',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_order_id` (`order_id`),
  KEY `idx_user_id_product_id` (`user_id`,`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `pay_order` (`user_id`, `product_id`, `product_name`, `order_id`, `order_time`, `total_amount`, `status`, `market_type`, `market_deduction_amount`, `pay_amount`)
VALUES ('test-user', '1001', '测试商品', 'TEST_PAY_0001', '2000-01-01 00:00:00', 100.00, 'PAY_WAIT', 'group_buy', 20.00, 80.00);
