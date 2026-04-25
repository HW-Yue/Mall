# ************************************************************
# Sequel Ace SQL dump
# 版本号： 20050
#
# https://sequel-ace.com/
# https://github.com/Sequel-Ace/Sequel-Ace
#
# 主机: 100.86.250.112 (MySQL 5.6.39)
# 数据库: s-pay-mall-ddd-market
# 生成时间: 2025-02-06 09:26:46 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
SET NAMES utf8mb4;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE='NO_AUTO_VALUE_ON_ZERO', SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

CREATE database if NOT EXISTS `s-pay-mall-ddd-market` default character set utf8mb4 ;
use `s-pay-mall-ddd-market`;

# 转储表 pay_order
# ------------------------------------------------------------

DROP TABLE IF EXISTS `pay_order`;

CREATE TABLE `pay_order` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(32) NOT NULL COMMENT '用户ID',
  `product_id` varchar(16) NOT NULL COMMENT '商品ID',
  `product_name` varchar(64) NOT NULL COMMENT '商品名称',
  `order_id` varchar(64) NOT NULL COMMENT '订单ID',
  `order_time` datetime NOT NULL COMMENT '下单时间',
  `total_amount` decimal(8,2) unsigned DEFAULT NULL COMMENT '订单金额',
  `status` varchar(32) NOT NULL COMMENT '订单状态；CREATE-创建完成、PAY_WAIT-等待支付、PAY_SUCCESS-支付成功、DEAL_DONE-交易完成、CLOSE-超时关单、PAY_AFTER_CLOSE-关单后收到付款待退款、WAIT_REFUND-退款处理中、REFUNDED-已退款',
  `pay_url` varchar(2014) DEFAULT NULL COMMENT '支付信息',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `market_type` varchar(32) DEFAULT NULL COMMENT '营销类型：normal-普通商品、group_buy-拼团业务、seckill-秒杀业务',
  `market_deduction_amount` decimal(8,2) DEFAULT NULL COMMENT '营销金额；优惠金额',
  `pay_amount` decimal(8,2) NOT NULL COMMENT '支付金额',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_order_id` (`order_id`),
  KEY `idx_user_id_product_id` (`user_id`,`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

LOCK TABLES `pay_order` WRITE;
/*!40000 ALTER TABLE `pay_order` DISABLE KEYS */;

INSERT INTO `pay_order` (`id`, `user_id`, `product_id`, `product_name`, `order_id`, `order_time`, `total_amount`, `status`, `pay_url`, `pay_time`, `market_type`, `market_deduction_amount`, `pay_amount`, `create_time`, `update_time`)
VALUES
	(11,'xiaofuge01','9890001','MyBatisBook','750681536632','2025-02-06 16:52:47',1.68,'PAY_WAIT','<form name=\"punchout_form\" method=\"post\" action=\"https://openapi-sandbox.dl.alipaydev.com/gateway.do?charset=utf-8&method=alipay.trade.page.pay&sign=I1TPyoZd45%2Bv6k4ka399jJO%2FrGF%2BAwrbPS%2FsLKCi7X9cMk5sxF2tTAkjRyKdDLKe9Xe0Dwycv2u8KuRSPqoxXGLqqAWdnIQiRnBuRA9ipDjqNN%2BxG3wvE8brfoG27c%2BehuznktexibaAvAGTjKsEy%2F33lEiUGZgN80uNvsMCrbnRTApvX9T4E3H3hfFrAGKstr5umM72O0fzsygBQZnvhSuOLJEU5FFxYwBIBVaDvzwyreCOIQ05AhGhT94v6WNEuoQn8BGxEklP6VATMeVDwRQIyVJ12dx3k0WyBVM%2FzAHe3Q7CIMAdbpOGBtUWc%2Fm0c%2BuRtZ3Fyl3V4bUdzNtptg%3D%3D&return_url=https%3A%2F%2Fgaga.plus&notify_url=http%3A%2F%2Fxfg-studio.natapp1.cc%2Fapi%2Fv1%2Falipay%2Falipay_notify_url&version=1.0&app_id=9021000132689924&sign_type=RSA2&timestamp=2025-02-06+16%3A52%3A48&alipay_sdk=alipay-sdk-java-4.38.157.ALL&format=json\">\n<input type=\"hidden\" name=\"biz_content\" value=\"{&quot;out_trade_no&quot;:&quot;750681536632&quot;,&quot;total_amount&quot;:90.00,&quot;subject&quot;:&quot;MyBatisBook&quot;,&quot;product_code&quot;:&quot;FAST_INSTANT_TRADE_PAY&quot;}\">\n<input type=\"submit\" value=\"立即支付\" style=\"display:none\" >\n</form>\n<script>document.forms[0].submit();</script>',NULL,'group_buy',10.00,90.00,'2025-02-06 16:52:47','2025-02-06 16:52:48'),
	(12,'xiaofuge02','9890001','MyBatisBook','556269893069','2025-02-06 16:56:11',100.00,'PAY_WAIT','<form name=\"punchout_form\" method=\"post\" action=\"https://openapi-sandbox.dl.alipaydev.com/gateway.do?charset=utf-8&method=alipay.trade.page.pay&sign=Br0nTsfQfAC9p1VvKICvwRhbp0j%2B4OQ5fJBs2dB6Mb9K0u9V083lfgM6Gb4Ob9qthtz0a%2BsaOWlXLx4TFvg7%2Flk8QUsmR4%2Bs%2F6VO8%2B9vMrjRzsi8ZfniZfhnhi7KbIAqN6VWl1kWpEyQ9hWdWTf36znyVUvXcAzuc75e8qKyxMhtBlVsjDTb7Zll1KkYRgNHVNxiJZ%2F7OeHncaMN7M3oqjxuvROt21V0j3le6%2Flit7ZJSmhKYw6Fq2CvC2nuDK21i67TKqo%2Bs5%2FRasgiglkXw24AtO8%2Fs3BL5MHPgmsULRuxIRhEaXR97pZuIobUQAi5ssb%2BE4Vy1r8QYxxvbXNQPA%3D%3D&return_url=https%3A%2F%2Fgaga.plus&notify_url=http%3A%2F%2Fxfg-studio.natapp1.cc%2Fapi%2Fv1%2Falipay%2Falipay_notify_url&version=1.0&app_id=9021000132689924&sign_type=RSA2&timestamp=2025-02-06+16%3A56%3A11&alipay_sdk=alipay-sdk-java-4.38.157.ALL&format=json\">\n<input type=\"hidden\" name=\"biz_content\" value=\"{&quot;out_trade_no&quot;:&quot;556269893069&quot;,&quot;total_amount&quot;:90.00,&quot;subject&quot;:&quot;MyBatisBook&quot;,&quot;product_code&quot;:&quot;FAST_INSTANT_TRADE_PAY&quot;}\">\n<input type=\"submit\" value=\"立即支付\" style=\"display:none\" >\n</form>\n<script>document.forms[0].submit();</script>',NULL,'group_buy',10.00,90.00,'2025-02-06 16:56:10','2025-02-06 16:56:11'),
	(13,'xfg_user_01','1001','GPT-4 100万Token','901111000001','2026-04-20 09:55:00',199.00,'PAY_SUCCESS',NULL,'2026-04-20 10:00:00','normal',0.00,199.00,'2026-04-20 09:55:00','2026-04-20 10:00:00'),
	(14,'xfg_user_02','1002','GPT-4 500万Token','901111000002','2026-04-21 11:00:00',899.00,'PAY_WAIT',NULL,NULL,'normal',0.00,899.00,'2026-04-21 11:00:00','2026-04-21 11:00:00'),
	(15,'xfg_user_03','1001','GPT-4 100万Token','901111000003','2026-04-18 15:20:00',199.00,'DEAL_DONE',NULL,'2026-04-18 15:30:00','group_buy',20.00,179.00,'2026-04-18 15:20:00','2026-04-18 15:30:00'),
	(16,'xfg_user_04','1003','Claude 100万Token','901111000004','2026-04-10 12:00:00',159.00,'CLOSE',NULL,NULL,'seckill',50.00,109.00,'2026-04-10 12:00:00','2026-04-10 12:00:00'),
	(17,'xfg_user_05','2004','云存储 1GB','901111000005','2026-04-19 07:50:00',69.00,'PAY_SUCCESS',NULL,'2026-04-19 08:00:00','seckill',30.00,39.00,'2026-04-19 07:50:00','2026-04-19 08:00:00'),
	(18,'xiaofuge','1005','通义千问 100万Token','901111000006','2026-04-15 15:00:00',89.00,'REFUNDED',NULL,'2026-04-15 16:00:00','group_buy',20.00,69.00,'2026-04-15 15:00:00','2026-04-15 16:00:00'),
	(19,'xfg01','1001','GPT-4 100万Token','901300100001','2026-04-18 10:00:00',199.00,'PAY_SUCCESS',NULL,'2026-04-18 10:02:00','group_buy',20.00,179.00,'2026-04-18 10:00:00','2026-04-18 10:02:00'),
	(20,'xfg04','1003','Claude 100万Token','901300200001','2026-04-22 12:00:00',159.00,'PAY_WAIT',NULL,NULL,'group_buy',20.00,139.00,'2026-04-22 12:00:00','2026-04-22 12:00:00'),
	(21,'demo_buyer_01','2005','云存储 1TB','802000000001','2026-04-23 10:00:00',299.00,'CREATE',NULL,NULL,'normal',0.00,299.00,'2026-04-23 10:00:00','2026-04-23 10:00:00'),
	(22,'demo_buyer_02','1004','文心一言 200万Token','802000000002','2026-04-23 11:30:00',99.00,'WAIT_REFUND',NULL,NULL,'normal',0.00,99.00,'2026-04-23 11:30:00','2026-04-23 11:30:00');

/*!40000 ALTER TABLE `pay_order` ENABLE KEYS */;
UNLOCK TABLES;


# 用户账号表（微信 openid 绑定账号密码，登录后前端 token 显示 username）
# ------------------------------------------------------------
DROP TABLE IF EXISTS `user_account`;

CREATE TABLE `user_account` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `username` varchar(64) NOT NULL COMMENT '用户名',
  `password` varchar(128) NOT NULL COMMENT '密码摘要',
  `openid` varchar(128) NOT NULL COMMENT '微信 openid',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_username` (`username`),
  UNIQUE KEY `uq_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户账号表-微信绑定';


/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
