-- Platform escrow: tracks money held for each order
CREATE TABLE IF NOT EXISTS platform_escrow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '关联订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    amount DECIMAL(10,2) NOT NULL COMMENT '托管金额',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=托管中 2=已结算 3=已退款',
    pay_time DATETIME COMMENT '用户支付时间',
    settle_time DATETIME COMMENT '结算时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    INDEX idx_order_id (order_id),
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台托管资金';

-- Host wallet
CREATE TABLE IF NOT EXISTS host_wallet (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    host_id BIGINT NOT NULL UNIQUE COMMENT '主持人ID',
    balance DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '可用余额',
    frozen_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '冻结金额(待支付佣金)',
    total_income DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计收入',
    total_commission DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计已付佣金',
    total_withdrawn DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计已提现',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    INDEX idx_host_id (host_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主持人钱包';

-- Commission order: platform charges host a commission after settlement
CREATE TABLE IF NOT EXISTS commission_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    commission_no VARCHAR(64) NOT NULL UNIQUE COMMENT '佣金单号',
    order_id BIGINT NOT NULL COMMENT '关联订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '关联订单号',
    host_id BIGINT NOT NULL COMMENT '主持人ID',
    host_name VARCHAR(64) COMMENT '主持人姓名',
    order_amount DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    commission_rate DECIMAL(5,2) NOT NULL COMMENT '佣金比例(%)',
    commission_amount DECIMAL(10,2) NOT NULL COMMENT '佣金金额',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=待支付 2=已支付 3=逾期',
    deadline DATETIME NOT NULL COMMENT '支付截止时间',
    pay_time DATETIME COMMENT '实际支付时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    INDEX idx_host_id (host_id),
    INDEX idx_order_id (order_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='佣金支付订单';

-- Settlement record: money from platform to host
CREATE TABLE IF NOT EXISTS settlement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    settlement_no VARCHAR(64) NOT NULL UNIQUE COMMENT '结算单号',
    order_id BIGINT NOT NULL COMMENT '关联订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '关联订单号',
    host_id BIGINT NOT NULL COMMENT '主持人ID',
    host_name VARCHAR(64) COMMENT '主持人姓名',
    amount DECIMAL(10,2) NOT NULL COMMENT '结算金额',
    commission_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '佣金金额',
    net_amount DECIMAL(10,2) NOT NULL COMMENT '实际到账金额(结算-佣金)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=待结算 2=已结算',
    settle_time DATETIME COMMENT '结算完成时间',
    operator VARCHAR(64) COMMENT '操作人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    INDEX idx_host_id (host_id),
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算记录';

-- Withdrawal request
CREATE TABLE IF NOT EXISTS withdrawal_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    withdrawal_no VARCHAR(64) NOT NULL UNIQUE COMMENT '提现单号',
    host_id BIGINT NOT NULL COMMENT '主持人ID',
    host_name VARCHAR(64) COMMENT '主持人姓名',
    amount DECIMAL(10,2) NOT NULL COMMENT '提现金额',
    account_type VARCHAR(32) COMMENT '账户类型(支付宝/微信/银行卡)',
    account_no VARCHAR(128) COMMENT '收款账号',
    account_name VARCHAR(64) COMMENT '收款人姓名',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=待审核 2=已通过 3=已拒绝 4=已打款',
    reject_reason VARCHAR(256) COMMENT '拒绝原因',
    audit_time DATETIME COMMENT '审核时间',
    pay_time DATETIME COMMENT '打款时间',
    operator VARCHAR(64) COMMENT '操作人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    INDEX idx_host_id (host_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现申请';

-- Platform settings for commission rate etc
CREATE TABLE IF NOT EXISTS platform_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(64) NOT NULL UNIQUE COMMENT '设置键',
    setting_value VARCHAR(256) NOT NULL COMMENT '设置值',
    description VARCHAR(256) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台设置';

-- Insert default commission rate
INSERT INTO platform_settings (setting_key, setting_value, description) VALUES
('commission_rate', '10.00', '平台佣金比例(%)'),
('commission_deadline_days', '7', '佣金支付截止天数');

-- Add accept_order field to host table
ALTER TABLE host ADD COLUMN IF NOT EXISTS can_accept_order TINYINT NOT NULL DEFAULT 1 COMMENT '是否可接单 0=禁止 1=允许';

-- Add payment_status to order table
ALTER TABLE `order` ADD COLUMN IF NOT EXISTS payment_status TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态 0=未支付 1=已支付 2=已退款';
ALTER TABLE `order` ADD COLUMN IF NOT EXISTS pay_time DATETIME COMMENT '支付时间';

-- ============================================
-- 财务管理权限数据
-- ============================================
INSERT INTO `sys_permission` (`permission_code`, `permission_name`, `resource_type`, `resource_path`, `parent_id`) VALUES
-- 结算管理
('admin:settlement:view', '查看结算列表', 'API', '/api/v1/settlement/page', 0),
('admin:settlement:manage', '管理结算', 'API', '/api/v1/settlement/*', 0),
-- 佣金管理
('admin:commission:view', '查看佣金列表', 'API', '/api/v1/commission/page', 0),
('admin:commission:manage', '管理佣金', 'API', '/api/v1/commission/*', 0),
-- 主持人钱包
('admin:host-wallet:view', '查看主持人钱包', 'API', '/api/v1/host-wallet/*', 0),
('admin:host-wallet:manage', '管理主持人钱包', 'API', '/api/v1/host-wallet/*', 0),
-- 提现管理
('admin:withdrawal:view', '查看提现列表', 'API', '/api/v1/host-wallet/withdrawals', 0),
('admin:withdrawal:manage', '审核提现', 'API', '/api/v1/host-wallet/withdrawal/*', 0),
-- 平台设置
('admin:platform-settings:view', '查看平台设置', 'API', '/api/v1/platform-settings', 0),
('admin:platform-settings:manage', '管理平台设置', 'API', '/api/v1/platform-settings/*', 0),
-- 主持人财务权限
('host:wallet:view', '查看我的钱包', 'API', '/api/v1/host-wallet/my', 0),
('host:wallet:withdraw', '申请提现', 'API', '/api/v1/host-wallet/withdraw', 0),
('host:commission:view', '查看我的佣金', 'API', '/api/v1/commission/my', 0),
('host:settlement:view', '查看我的结算', 'API', '/api/v1/settlement/my', 0);

-- 角色权限关联（管理员 - 新增财务权限）
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.role_code = 'ROLE_ADMIN' AND p.permission_code LIKE 'admin:%'
AND p.permission_code NOT IN (
    SELECT rp_inner.permission_id FROM sys_role_permission rp_inner
    JOIN sys_permission sp ON sp.id = rp_inner.permission_id
    WHERE rp_inner.role_id = r.id AND sp.permission_code = p.permission_code
);

-- 角色权限关联（主持人 - 新增财务权限）
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.role_code = 'ROLE_HOST' AND p.permission_code IN (
    'host:wallet:view', 'host:wallet:withdraw', 'host:commission:view', 'host:settlement:view'
);

-- 角色权限关联（超级管理员 - 新增权限）
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.role_code = 'ROLE_SUPER_ADMIN'
AND p.id NOT IN (
    SELECT rp2.permission_id FROM sys_role_permission rp2 WHERE rp2.role_id = r.id
);
