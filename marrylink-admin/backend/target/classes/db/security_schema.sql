-- MarryLink 权限管理表结构
-- 支持多用户类型（CUSTOMER/HOST/ADMIN）的统一认证和权限管理

-- ============================================
-- 1. 统一账号映射表
-- ============================================
CREATE TABLE IF NOT EXISTS `account_mapping` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID（权限表ID）',
    `account_id` VARCHAR(50) NOT NULL COMMENT '统一账号ID（手机号/邮箱）',
    `user_type` VARCHAR(20) NOT NULL COMMENT '用户类型: CUSTOMER/HOST/ADMIN',
    `ref_id` BIGINT NOT NULL COMMENT '关联表ID（user.id/host.id/sys_admin.id）',
    `password` VARCHAR(255) NOT NULL COMMENT '加密密码',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    `last_login_time` DATETIME COMMENT '最后登录时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_account_type` (`account_id`, `user_type`),
    INDEX `idx_ref_id` (`ref_id`),
    INDEX `idx_user_type` (`user_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一账号映射表';

-- ============================================
-- 2. 角色表
-- ============================================
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `role_code` VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    `role_name` VARCHAR(100) NOT NULL COMMENT '角色名称',
    `user_type` VARCHAR(20) NOT NULL COMMENT '适用用户类型: CUSTOMER/HOST/ADMIN',
    `description` VARCHAR(500) COMMENT '角色描述',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT DEFAULT 0,
    INDEX `idx_user_type` (`user_type`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ============================================
-- 3. 权限表
-- ============================================
CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `permission_code` VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
    `permission_name` VARCHAR(100) NOT NULL COMMENT '权限名称',
    `resource_type` VARCHAR(20) COMMENT '资源类型: MENU/API/BUTTON',
    `resource_path` VARCHAR(200) COMMENT '资源路径',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父权限ID',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT DEFAULT 0,
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_resource_type` (`resource_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- ============================================
-- 4. 角色权限关联表
-- ============================================
CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    INDEX `idx_role_id` (`role_id`),
    INDEX `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ============================================
-- 5. 用户角色关联表
-- ============================================
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `account_id` BIGINT NOT NULL COMMENT '账号映射表ID（account_mapping.id）',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_account_role` (`account_id`, `role_id`),
    INDEX `idx_account_id` (`account_id`),
    INDEX `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ============================================
-- 初始化角色数据
-- ============================================
INSERT INTO `sys_role` (`role_code`, `role_name`, `user_type`, `description`, `sort_order`) VALUES
('ROLE_CUSTOMER', '新人用户', 'CUSTOMER', '普通新人用户角色，可以浏览主持人、预约服务、填写问卷', 1),
('ROLE_HOST', '主持人', 'HOST', '婚礼主持人角色，可以管理档期、查看订单、处理问卷', 2),
('ROLE_HOST_PREMIUM', '高级主持人', 'HOST', '高级主持人，拥有更多权限和功能', 3),
('ROLE_ADMIN', '系统管理员', 'ADMIN', '系统管理员角色，拥有所有权限', 4),
('ROLE_SUPER_ADMIN', '超级管理员', 'ADMIN', '超级管理员，拥有最高权限', 5);

-- ============================================
-- 初始化权限数据
-- ============================================
INSERT INTO `sys_permission` (`permission_code`, `permission_name`, `resource_type`, `resource_path`, `parent_id`) VALUES
-- 新人用户权限
('customer:host:view', '查看主持人列表', 'API', '/api/v1/host/page', 0),
('customer:host:detail', '查看主持人详情', 'API', '/api/v1/host/{id}', 0),
('customer:order:create', '创建预约', 'API', '/api/v1/order', 0),
('customer:order:view', '查看我的预约', 'API', '/api/v1/order/my', 0),
('customer:questionnaire:fill', '填写问卷', 'API', '/api/v1/questionnaire-submission/submit', 0),
('customer:questionnaire:view', '查看我的问卷', 'API', '/api/v1/questionnaire-submission/user/*', 0),

-- 主持人权限
('host:schedule:view', '查看档期', 'API', '/api/v1/host/schedule', 0),
('host:schedule:add', '添加档期', 'API', '/api/v1/host/schedule', 0),
('host:schedule:update', '更新档期', 'API', '/api/v1/host/schedule/*', 0),
('host:schedule:delete', '删除档期', 'API', '/api/v1/host/schedule/*', 0),
('host:order:view', '查看订单', 'API', '/api/v1/order/host/*', 0),
('host:order:manage', '管理订单', 'API', '/api/v1/order/*/status', 0),
('host:questionnaire:view', '查看问卷', 'API', '/api/v1/questionnaire-submission/host/*', 0),
('host:questionnaire:reply', '回复问卷', 'API', '/api/v1/questionnaire-submission/*/status', 0),
('host:profile:view', '查看个人信息', 'API', '/api/v1/host/{id}', 0),
('host:profile:update', '更新个人信息', 'API', '/api/v1/host', 0),

-- 管理员权限
('admin:user:view', '查看用户列表', 'API', '/api/v1/user/page', 0),
('admin:user:manage', '管理用户', 'API', '/api/v1/user/*', 0),
('admin:host:view', '查看主持人列表', 'API', '/api/v1/host/page', 0),
('admin:host:manage', '管理主持人', 'API', '/api/v1/host/*', 0),
('admin:order:view', '查看订单列表', 'API', '/api/v1/order/page', 0),
('admin:order:manage', '管理订单', 'API', '/api/v1/order/*', 0),
('admin:tag:view', '查看标签', 'API', '/api/v1/tag/*', 0),
('admin:tag:manage', '管理标签', 'API', '/api/v1/tag/*', 0),
('admin:questionnaire:view', '查看问卷', 'API', '/api/v1/questionnaire-template/*', 0),
('admin:questionnaire:manage', '管理问卷', 'API', '/api/v1/questionnaire-template/*', 0),
('admin:system:config', '系统配置', 'API', '/api/v1/system/*', 0);

-- ============================================
-- 角色权限关联（新人用户）
-- ============================================
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.role_code = 'ROLE_CUSTOMER' AND p.permission_code LIKE 'customer:%';

-- ============================================
-- 角色权限关联（主持人）
-- ============================================
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.role_code = 'ROLE_HOST' AND p.permission_code LIKE 'host:%';

-- ============================================
-- 角色权限关联（管理员）
-- ============================================
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.role_code = 'ROLE_ADMIN' AND p.permission_code LIKE 'admin:%';

-- ============================================
-- 角色权限关联（超级管理员 - 所有权限）
-- ============================================
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.role_code = 'ROLE_SUPER_ADMIN';

-- ============================================
-- 初始化测试账号映射数据
-- ============================================
-- 管理员账号（admin/admin123）
INSERT INTO `account_mapping` (`account_id`, `user_type`, `ref_id`, `password`, `status`) VALUES
('admin', 'ADMIN', 1, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKfA8jOe', 1);

-- 新人用户账号（13800138000/123456）
INSERT INTO `account_mapping` (`account_id`, `user_type`, `ref_id`, `password`, `status`) VALUES
('13800138000', 'CUSTOMER', 1, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKfA8jOe', 1);

-- 主持人账号（13900139000/123456）
INSERT INTO `account_mapping` (`account_id`, `user_type`, `ref_id`, `password`, `status`) VALUES
('13900139000', 'HOST', 1, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKfA8jOe', 1);

-- ============================================
-- 初始化用户角色关联
-- ============================================
-- 管理员角色
INSERT INTO `sys_user_role` (`account_id`, `role_id`)
SELECT am.id, r.id FROM account_mapping am, sys_role r
WHERE am.account_id = 'admin' AND am.user_type = 'ADMIN' AND r.role_code = 'ROLE_SUPER_ADMIN';

-- 新人用户角色
INSERT INTO `sys_user_role` (`account_id`, `role_id`)
SELECT am.id, r.id FROM account_mapping am, sys_role r
WHERE am.account_id = '13800138000' AND am.user_type = 'CUSTOMER' AND r.role_code = 'ROLE_CUSTOMER';

-- 主持人角色
INSERT INTO `sys_user_role` (`account_id`, `role_id`)
SELECT am.id, r.id FROM account_mapping am, sys_role r
WHERE am.account_id = '13900139000' AND am.user_type = 'HOST' AND r.role_code = 'ROLE_HOST';