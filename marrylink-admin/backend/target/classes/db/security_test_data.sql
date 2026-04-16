-- =====================================================
-- MarryLink 权限系统测试数据初始化脚本
-- 用于快速测试登录和权限功能
-- =====================================================

-- 清理旧数据（如果存在）
DELETE FROM sys_user_role WHERE account_id IN (
    SELECT id FROM account_mapping WHERE account_id IN ('admin', '13900139000', '13800138000')
);
DELETE FROM sys_role_permission;
DELETE FROM account_mapping WHERE account_id IN ('admin', '13900139000', '13800138000');
DELETE FROM sys_permission;
DELETE FROM sys_role;

-- 1. 插入测试角色
INSERT INTO sys_role (role_code, role_name, user_type, description, create_time, update_time, is_deleted) VALUES
('ROLE_ADMIN', '系统管理员', 'ADMIN', '拥有系统所有权限', NOW(), NOW(), 0),
('ROLE_HOST', '主持人', 'HOST', '主持人角色，可管理自己的档期和订单', NOW(), NOW(), 0),
('ROLE_CUSTOMER', '新人', 'CUSTOMER', '新人角色，可预约主持人和填写问卷', NOW(), NOW(), 0);

-- 2. 插入测试权限
INSERT INTO sys_permission (permission_code, permission_name, resource_type, create_time, update_time, is_deleted) VALUES
-- 管理员权限
('system:user:view', '查看用户', 'API', NOW(), NOW(), 0),
('system:user:edit', '编辑用户', 'API', NOW(), NOW(), 0),
('system:host:view', '查看主持人', 'API', NOW(), NOW(), 0),
('system:host:edit', '编辑主持人', 'API', NOW(), NOW(), 0),
('system:order:view', '查看订单', 'API', NOW(), NOW(), 0),
('system:order:manage', '管理订单', 'API', NOW(), NOW(), 0),
('system:tag:manage', '管理标签', 'API', NOW(), NOW(), 0),
('system:questionnaire:manage', '管理问卷', 'API', NOW(), NOW(), 0),

-- 主持人权限
('host:schedule:view', '查看档期', 'API', NOW(), NOW(), 0),
('host:schedule:manage', '管理档期', 'API', NOW(), NOW(), 0),
('host:order:view', '查看订单', 'API', NOW(), NOW(), 0),
('host:order:manage', '管理订单', 'API', NOW(), NOW(), 0),
('host:questionnaire:view', '查看问卷', 'API', NOW(), NOW(), 0),
('host:profile:edit', '编辑资料', 'API', NOW(), NOW(), 0),

-- 新人权限
('customer:host:view', '查看主持人', 'API', NOW(), NOW(), 0),
('customer:order:view', '查看订单', 'API', NOW(), NOW(), 0),
('customer:order:create', '创建订单', 'API', NOW(), NOW(), 0),
('customer:questionnaire:submit', '提交问卷', 'API', NOW(), NOW(), 0),
('customer:profile:edit', '编辑资料', 'API', NOW(), NOW(), 0);

-- 3. 角色-权限关联
-- 管理员角色拥有所有权限
INSERT INTO sys_role_permission (role_id, permission_id, create_time)
SELECT
    (SELECT id FROM sys_role WHERE role_code = 'ROLE_ADMIN'),
    id,
    NOW()
FROM sys_permission;

-- 主持人角色权限
INSERT INTO sys_role_permission (role_id, permission_id, create_time)
SELECT
    (SELECT id FROM sys_role WHERE role_code = 'ROLE_HOST'),
    id,
    NOW()
FROM sys_permission
WHERE permission_code IN (
    'host:schedule:view',
    'host:schedule:manage',
    'host:order:view',
    'host:order:manage',
    'host:questionnaire:view',
    'host:profile:edit'
);

-- 新人角色权限
INSERT INTO sys_role_permission (role_id, permission_id, create_time)
SELECT
    (SELECT id FROM sys_role WHERE role_code = 'ROLE_CUSTOMER'),
    id,
    NOW()
FROM sys_permission
WHERE permission_code IN (
    'customer:host:view',
    'customer:order:view',
    'customer:order:create',
    'customer:questionnaire:submit',
    'customer:profile:edit'
);

-- 4. 创建测试账号映射（密码统一为：123456）
-- 注意：实际使用时需要通过BCrypt加密，这里的密码是 BCrypt 加密后的 "123456"
-- $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi

-- 测试管理员账号
INSERT INTO account_mapping (account_id, password, user_type, ref_id, status, create_time, update_time, is_deleted) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'ADMIN', 1, 1, NOW(), NOW(), 0);

-- 测试主持人账号（假设 host 表中 id=16 的主持人手机号为 13900139000）
INSERT INTO account_mapping (account_id, password, user_type, ref_id, status, create_time, update_time, is_deleted) VALUES
('13900139000', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'HOST', 16, 1, NOW(), NOW(), 0);

-- 测试新人账号（假设 user 表中 id=1 的新人手机号为 13800138000）
INSERT INTO account_mapping (account_id, password, user_type, ref_id, status, create_time, update_time, is_deleted) VALUES
('13800138000', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'CUSTOMER', 1, 1, NOW(), NOW(), 0);

-- 5. 用户-角色关联
-- 管理员角色
INSERT INTO sys_user_role (account_id, role_id, create_time)
VALUES (
    (SELECT id FROM account_mapping WHERE account_id = 'admin' AND user_type = 'ADMIN'),
    (SELECT id FROM sys_role WHERE role_code = 'ROLE_ADMIN'),
    NOW()
);

-- 主持人角色
INSERT INTO sys_user_role (account_id, role_id, create_time)
VALUES (
    (SELECT id FROM account_mapping WHERE account_id = '13900139000' AND user_type = 'HOST'),
    (SELECT id FROM sys_role WHERE role_code = 'ROLE_HOST'),
    NOW()
);

-- 新人角色
INSERT INTO sys_user_role (account_id, role_id, create_time)
VALUES (
    (SELECT id FROM account_mapping WHERE account_id = '13800138000' AND user_type = 'CUSTOMER'),
    (SELECT id FROM sys_role WHERE role_code = 'ROLE_CUSTOMER'),
    NOW()
);

-- =====================================================
-- 测试账号说明：
-- 1. 管理员账号：admin / 123456 (userType: ADMIN)
-- 2. 主持人账号：13900139000 / 123456 (userType: HOST, refId=16)
-- 3. 新人账号：13800138000 / 123456 (userType: CUSTOMER, refId=1)
-- =====================================================
