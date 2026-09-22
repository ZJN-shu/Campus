-- V2: Add audit log table for operation tracking

CREATE TABLE IF NOT EXISTS `tb_audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint DEFAULT NULL COMMENT '操作用户ID',
  `username` varchar(50) DEFAULT NULL COMMENT '用户昵称',
  `module` varchar(50) NOT NULL COMMENT '操作模块',
  `operation` varchar(50) NOT NULL COMMENT '操作类型',
  `method` varchar(200) DEFAULT NULL COMMENT '请求方法',
  `params` text DEFAULT NULL COMMENT '请求参数',
  `ip` varchar(50) DEFAULT NULL COMMENT '操作IP',
  `status` tinyint DEFAULT 1 COMMENT '状态 1成功 0失败',
  `error_msg` text DEFAULT NULL COMMENT '错误信息',
  `duration` bigint DEFAULT NULL COMMENT '执行时长(ms)',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_module` (`module`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作审计日志表';
