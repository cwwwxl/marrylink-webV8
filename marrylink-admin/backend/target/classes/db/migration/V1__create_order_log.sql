CREATE TABLE IF NOT EXISTS `order_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(100) NOT NULL COMMENT '订单号',
  `old_status` INT COMMENT '原状态',
  `new_status` INT NOT NULL COMMENT '新状态',
  `operator` VARCHAR(100) NOT NULL COMMENT '操作人',
  `operator_ip` VARCHAR(50) COMMENT '操作IP',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单状态变更日志表';