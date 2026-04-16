CREATE TABLE `message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `content` varchar(500) NOT NULL COMMENT '消息内容',
  `host_id` bigint DEFAULT NULL COMMENT '关联主持人ID',
  `admin_id` bigint DEFAULT NULL COMMENT '关联管理员ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '消息状态：1-未读，2-已读',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_host_id` (`host_id`),
  KEY `idx_admin_id` (`admin_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';