-- 婚礼智配中台数据库DDL

-- 管理员表
CREATE TABLE IF NOT EXISTS `sys_admin` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(50),
    `avatar` VARCHAR(255),
    `status` TINYINT DEFAULT 1 COMMENT '1:正常 0:禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 主持人表
CREATE TABLE IF NOT EXISTS `host` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `host_code` VARCHAR(20) NOT NULL UNIQUE COMMENT '主持人编号如MC0001',
    `name` VARCHAR(50) NOT NULL,
    `stage_name` VARCHAR(50) COMMENT '艺名',
    `phone` VARCHAR(20) NOT NULL,
    `email` VARCHAR(100),
    `avatar` VARCHAR(255),
    `price` DECIMAL(10,2) COMMENT '服务价格/场',
    `service_areas` JSON COMMENT '服务地区',
    `status` TINYINT DEFAULT 1 COMMENT '1:正常 2:待审核 0:禁用',
    `rating` DECIMAL(2,1) DEFAULT 5.0,
    `order_count` INT DEFAULT 0,
    `join_time` DATE,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户表(新人)
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_code` VARCHAR(20) NOT NULL UNIQUE COMMENT '用户编号如USR00001',
    `bride_name` VARCHAR(50) COMMENT '新娘姓名',
    `groom_name` VARCHAR(50) COMMENT '新郎姓名',
    `phone` VARCHAR(20) NOT NULL,
    `avatar` VARCHAR(255),
    `wedding_type` VARCHAR(50) COMMENT '婚礼类型',
    `status` TINYINT DEFAULT 1 COMMENT '1:正常 0:禁用',
    `order_count` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 标签分类表
CREATE TABLE IF NOT EXISTS `tag_category` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL,
    `code` VARCHAR(50) NOT NULL UNIQUE,
    `tag_count` INT DEFAULT 0,
    `status` TINYINT DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 标签表
CREATE TABLE IF NOT EXISTS `tag` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `category_id` BIGINT NOT NULL,
    `name` VARCHAR(50) NOT NULL,
    `code` VARCHAR(50) NOT NULL,
    `use_count` INT DEFAULT 0,
    `status` TINYINT DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT DEFAULT 0,
    FOREIGN KEY (`category_id`) REFERENCES `tag_category`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 主持人标签关联表
CREATE TABLE IF NOT EXISTS `host_tag` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `host_id` BIGINT NOT NULL,
    `tag_id` BIGINT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_host_tag` (`host_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单表
CREATE TABLE IF NOT EXISTS `order` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `order_no` VARCHAR(30) NOT NULL UNIQUE,
    `user_id` BIGINT NOT NULL,
    `host_id` BIGINT NOT NULL,
    `wedding_date` DATE NOT NULL,
    `wedding_type` VARCHAR(50),
    `amount` DECIMAL(10,2),
    `status` TINYINT DEFAULT 1 COMMENT '1:待确认 2:已确认 3:定金已付 4:已完成 5:已取消',
    `rating` TINYINT DEFAULT NULL COMMENT '用户评分 1-5星',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 问卷模板表
CREATE TABLE IF NOT EXISTS `questionnaire_template` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `type` VARCHAR(50) COMMENT '模板类型',
    `question_count` INT DEFAULT 0,
    `use_count` INT DEFAULT 0,
    `content` JSON COMMENT '问卷内容',
    `status` TINYINT DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 问卷提交记录表
CREATE TABLE IF NOT EXISTS `questionnaire_submission` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `submission_code` VARCHAR(30) NOT NULL UNIQUE,
    `user_id` BIGINT NOT NULL,
    `template_id` BIGINT NOT NULL,
    `host_id` BIGINT,
    `answers` JSON,
    `status` TINYINT DEFAULT 1 COMMENT '1:待处理 2:已回复 3:已确认',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化管理员
INSERT INTO `sys_admin` (`username`, `password`, `nickname`) VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '管理员');

-- 初始化测试用户
INSERT INTO `user` (`user_code`, `bride_name`, `groom_name`, `phone`, `wedding_type`, `status`)
VALUES ('USR00001', '李小美', '王大明', '13800138000', '中式婚礼', 1);

-- 初始化测试主持人
INSERT INTO `host` (`host_code`, `name`, `stage_name`, `phone`, `email`, `price`, `status`, `rating`, `join_time`)
VALUES ('MC0001', '张主持', '婚礼达人', '13900139000', 'host@example.com', 3000.00, 1, 4.8, '2024-01-01');

-- 初始化标签分类
INSERT INTO `tag_category` (`name`, `code`, `status`) VALUES
('主持人风格', '01', 1),
('问卷类型', '02', 1);

-- 初始化标签
INSERT INTO `tag` (`category_id`, `name`, `code`, `status`) VALUES
(1, '温馨浪漫', 'STYLE_ROMANTIC', 1),
(1, '幽默风趣', 'STYLE_HUMOROUS', 1),
(2, '婚礼基础信息', 'QUEST_BASIC', 1),
(2, '婚礼流程规划', 'QUEST_PROCESS', 1);

-- 初始化问卷模板
INSERT INTO `questionnaire_template` (`name`, `type`, `question_count`, `use_count`, `content`, `status`) VALUES
('婚礼基础信息问卷', '3', 8, 1, '[
  {
    "id": 1,
    "type": "text",
    "question": "新郎姓名",
    "placeholder": "请输入新郎的姓名",
    "required": true
  },
  {
    "id": 2,
    "type": "text",
    "question": "新娘姓名",
    "placeholder": "请输入新娘的姓名",
    "required": true
  },
  {
    "id": 3,
    "type": "date",
    "question": "婚礼日期",
    "required": true
  },
  {
    "id": 4,
    "type": "text",
    "question": "婚礼地点",
    "placeholder": "请输入婚礼举办地点",
    "required": true
  },
  {
    "id": 5,
    "type": "radio",
    "question": "婚礼风格",
    "options": ["中式传统", "西式浪漫", "现代简约", "混搭创意"],
    "required": true
  },
  {
    "id": 6,
    "type": "number",
    "question": "预计宾客人数",
    "placeholder": "请输入预计参加婚礼的宾客人数",
    "min": 10,
    "max": 1000,
    "required": true
  },
  {
    "id": 7,
    "type": "checkbox",
    "question": "特殊需求（多选）",
    "options": ["需要双语主持", "有特殊仪式环节", "需要互动游戏", "有视频播放", "其他"],
    "required": false
  },
  {
    "id": 8,
    "type": "textarea",
    "question": "其他补充说明",
    "placeholder": "请填写您对婚礼的其他期望或特殊要求",
    "required": false
  }
]', 1);

-- 初始化待填写的问卷提交记录
INSERT INTO `questionnaire_submission` (`submission_code`, `user_id`, `template_id`, `host_id`, `answers`, `status`) VALUES
('QS202601070001', 1, 1, 1, NULL, 1);

-- 初始化测试订单数据（用于档期管理测试）
INSERT INTO `order` (`order_no`, `user_id`, `host_id`, `wedding_date`, `wedding_type`, `amount`, `status`) VALUES
('ORD202601150001', 1, 1, '2026-01-15', '中式婚礼', 3000.00, 1),
('ORD202601200001', 1, 1, '2026-01-20', '西式婚礼', 3500.00, 2),
('ORD202601250001', 1, 1, '2026-01-25', '现代简约', 4000.00, 3),
('ORD202602140001', 1, 1, '2026-02-14', '浪漫婚礼', 5000.00, 3),
('ORD202603080001', 1, 1, '2026-03-08', '春季婚礼', 3800.00, 1);