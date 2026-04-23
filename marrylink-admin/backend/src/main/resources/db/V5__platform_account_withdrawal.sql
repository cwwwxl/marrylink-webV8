-- 平台账户表（单行记录，存储平台佣金收入）
CREATE TABLE IF NOT EXISTS `platform_account` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `balance` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '可提现余额',
  `total_commission_income` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '累计佣金收入',
  `total_withdrawn` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '累计已提现',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '平台账户';

-- 初始化平台账户
INSERT IGNORE INTO `platform_account` (`id`, `balance`, `total_commission_income`, `total_withdrawn`) VALUES (1, 0.00, 0.00, 0.00);

-- 平台提现记录表
CREATE TABLE IF NOT EXISTS `platform_withdrawal` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `withdrawal_no` VARCHAR(64) NOT NULL COMMENT '提现单号',
  `amount` DECIMAL(12,2) NOT NULL COMMENT '提现金额',
  `account_type` VARCHAR(32) DEFAULT NULL COMMENT '账户类型（银行卡/支付宝/微信）',
  `account_no` VARCHAR(128) DEFAULT NULL COMMENT '账号',
  `account_name` VARCHAR(64) DEFAULT NULL COMMENT '户名',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1=待处理 2=已完成',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `operator` VARCHAR(64) DEFAULT NULL COMMENT '操作人',
  `complete_time` DATETIME DEFAULT NULL COMMENT '完成时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '平台提现记录';
