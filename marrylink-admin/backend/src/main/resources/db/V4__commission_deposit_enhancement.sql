-- V4: 佣金模块完善 - 新增定金相关字段
-- 订单表新增定金金额字段
ALTER TABLE `order` ADD COLUMN `deposit_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '定金金额（订单总额的30%）' AFTER `amount`;

-- 平台托管表新增订单全额字段（用于佣金计算）
ALTER TABLE `platform_escrow` ADD COLUMN `total_order_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '订单全额（用于佣金计算）' AFTER `amount`;
