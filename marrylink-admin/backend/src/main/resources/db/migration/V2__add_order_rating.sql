-- 订单表新增评分字段
ALTER TABLE `order` ADD COLUMN `rating` TINYINT NULL DEFAULT NULL COMMENT '用户评分 1-5星' AFTER `status`;