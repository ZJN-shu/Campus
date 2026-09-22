-- V1: Initial schema - baseline migration
-- All tables use IF NOT EXISTS for safe re-run on existing databases

-- ============================================================
-- 1. 学生表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_student` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_no` varchar(20) DEFAULT NULL COMMENT '学号',
  `phone` varchar(11) NOT NULL COMMENT '手机号',
  `password` varchar(32) DEFAULT NULL COMMENT '密码',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `nick_name` varchar(50) NOT NULL COMMENT '昵称',
  `avatar` varchar(255) DEFAULT 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png' COMMENT '头像',
  `college` varchar(50) DEFAULT NULL COMMENT '学院',
  `major` varchar(50) DEFAULT NULL COMMENT '专业',
  `grade` int DEFAULT NULL COMMENT '入学年份',
  `credit` int DEFAULT 100 COMMENT '信用分',
  `status` tinyint DEFAULT 1 COMMENT '状态 1正常 2禁言 3封号',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `student_no` (`student_no`),
  KEY `idx_phone` (`phone`),
  KEY `idx_college` (`college`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生表';

-- ============================================================
-- 2. 帖子表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_post` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '作者ID',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `content` text COMMENT '内容',
  `type` tinyint DEFAULT 1 COMMENT '类型 1普通 2二手',
  `category` varchar(50) NOT NULL COMMENT '板块',
  `tags` varchar(200) DEFAULT NULL COMMENT '标签',
  `images` json DEFAULT NULL COMMENT '图片列表',
  `view_count` int DEFAULT 0 COMMENT '浏览数',
  `like_count` int DEFAULT 0 COMMENT '点赞数',
  `comment_count` int DEFAULT 0 COMMENT '评论数',
  `share_count` int DEFAULT 0 COMMENT '分享数',
  `favorite_count` int DEFAULT 0 COMMENT '收藏数',
  `hot_score` decimal(10,3) DEFAULT 0.000 COMMENT '热度分',
  `is_top` tinyint DEFAULT 0 COMMENT '是否置顶 0否 1是',
  `is_essence` tinyint DEFAULT 0 COMMENT '是否精华 0否 1是',
  `status` tinyint DEFAULT 1 COMMENT '状态 1正常 2审核 3删除',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_category` (`category`),
  KEY `idx_hot` (`hot_score`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `tb_post_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `tb_student` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子表';

-- ============================================================
-- 3. 帖子评论表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_post_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '评论用户ID',
  `content` varchar(500) NOT NULL COMMENT '评论内容',
  `like_count` int DEFAULT 0 COMMENT '点赞数',
  `parent_id` bigint DEFAULT 0 COMMENT '父评论ID 0表示顶级评论',
  `reply_user_id` bigint DEFAULT NULL COMMENT '回复的用户ID',
  `status` tinyint DEFAULT 1 COMMENT '状态 1正常 2删除',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_post` (`post_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_parent` (`parent_id`),
  CONSTRAINT `tb_post_comment_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `tb_post` (`id`) ON DELETE CASCADE,
  CONSTRAINT `tb_post_comment_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `tb_student` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子评论表';

-- ============================================================
-- 4. 二手商品表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `seller_id` bigint NOT NULL COMMENT '卖家ID',
  `title` varchar(100) NOT NULL COMMENT '商品标题',
  `description` text COMMENT '商品描述',
  `price` decimal(10,2) NOT NULL COMMENT '价格',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价',
  `category` varchar(50) NOT NULL COMMENT '分类',
  `sub_category` varchar(50) DEFAULT NULL COMMENT '子分类',
  `quality` varchar(20) DEFAULT NULL COMMENT '成色',
  `images` json DEFAULT NULL COMMENT '图片列表',
  `location` varchar(100) DEFAULT NULL COMMENT '交易地点',
  `view_count` int DEFAULT 0 COMMENT '浏览次数',
  `favorite_count` int DEFAULT 0 COMMENT '收藏数',
  `comment_count` int DEFAULT 0 COMMENT '评论数',
  `status` tinyint DEFAULT 1 COMMENT '状态 1在售 2已售 3下架',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `reserved_stock` int DEFAULT 0 COMMENT '预扣库存（待支付订单占用）',
  `version` int DEFAULT 0 COMMENT '乐观锁版本号',
  `stock` int DEFAULT 0 COMMENT '实际库存',
  PRIMARY KEY (`id`),
  KEY `idx_seller` (`seller_id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `tb_product_ibfk_1` FOREIGN KEY (`seller_id`) REFERENCES `tb_student` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='二手商品表';

-- ============================================================
-- 5. 商品评论表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_product_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `user_id` bigint NOT NULL COMMENT '评论用户ID',
  `content` varchar(500) NOT NULL COMMENT '评论内容',
  `type` tinyint DEFAULT 1 COMMENT '类型 1咨询 2砍价 3回复',
  `parent_id` bigint DEFAULT 0 COMMENT '父评论ID',
  `reply_user_id` bigint DEFAULT NULL COMMENT '回复的用户ID',
  `status` tinyint DEFAULT 1 COMMENT '状态',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`),
  KEY `idx_user` (`user_id`),
  CONSTRAINT `tb_product_comment_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `tb_product` (`id`) ON DELETE CASCADE,
  CONSTRAINT `tb_product_comment_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `tb_student` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品评论表';

-- ============================================================
-- 6. 跑腿任务表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_errand_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `publisher_id` bigint NOT NULL COMMENT '发布者ID',
  `title` varchar(100) NOT NULL COMMENT '任务标题',
  `description` text COMMENT '任务描述',
  `category` varchar(50) DEFAULT NULL COMMENT '任务类型',
  `reward` decimal(10,2) NOT NULL COMMENT '报酬',
  `pickup_location` varchar(200) DEFAULT NULL COMMENT '取件地点',
  `delivery_location` varchar(200) DEFAULT NULL COMMENT '送达地点',
  `latitude` double DEFAULT NULL COMMENT '纬度',
  `longitude` double DEFAULT NULL COMMENT '经度',
  `deadline` datetime DEFAULT NULL COMMENT '截止时间',
  `status` tinyint DEFAULT 1 COMMENT '状态 1待接单 2已接单 3进行中 4已完成 5已取消',
  `acceptor_id` bigint DEFAULT NULL COMMENT '接单者ID',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `finish_time` timestamp NULL DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`),
  KEY `idx_publisher` (`publisher_id`),
  KEY `idx_acceptor` (`acceptor_id`),
  KEY `idx_status` (`status`),
  KEY `idx_location` (`latitude`,`longitude`),
  CONSTRAINT `tb_errand_task_ibfk_1` FOREIGN KEY (`publisher_id`) REFERENCES `tb_student` (`id`),
  CONSTRAINT `tb_errand_task_ibfk_2` FOREIGN KEY (`acceptor_id`) REFERENCES `tb_student` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跑腿任务表';

-- ============================================================
-- 7. 跑腿订单表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_errand_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `publisher_id` bigint NOT NULL COMMENT '发布者ID',
  `acceptor_id` bigint NOT NULL COMMENT '接单者ID',
  `reward` decimal(10,2) NOT NULL COMMENT '报酬',
  `status` tinyint DEFAULT 1 COMMENT '状态 1待支付 2已支付 3已完成',
  `pay_status` tinyint DEFAULT 0 COMMENT '支付状态 0未支付 1已支付',
  `pay_time` timestamp NULL DEFAULT NULL COMMENT '支付时间',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `publisher_evaluated` tinyint DEFAULT 0 COMMENT '发布者是否已评价 0-未评价 1-已评价',
  `acceptor_evaluated` tinyint DEFAULT 0 COMMENT '接单者是否已评价 0-未评价 1-已评价',
  PRIMARY KEY (`id`),
  KEY `idx_task` (`task_id`),
  KEY `idx_publisher` (`publisher_id`),
  KEY `idx_acceptor` (`acceptor_id`),
  CONSTRAINT `tb_errand_order_ibfk_1` FOREIGN KEY (`task_id`) REFERENCES `tb_errand_task` (`id`),
  CONSTRAINT `tb_errand_order_ibfk_2` FOREIGN KEY (`publisher_id`) REFERENCES `tb_student` (`id`),
  CONSTRAINT `tb_errand_order_ibfk_3` FOREIGN KEY (`acceptor_id`) REFERENCES `tb_student` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跑腿订单表';

-- ============================================================
-- 8. 跑腿评价表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_errand_evaluation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评价ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `from_user_id` bigint NOT NULL COMMENT '评价人ID',
  `to_user_id` bigint NOT NULL COMMENT '被评价人ID',
  `score` tinyint NOT NULL COMMENT '评分 1-5',
  `content` varchar(500) DEFAULT NULL COMMENT '评价内容',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task` (`task_id`),
  KEY `idx_to_user` (`to_user_id`),
  KEY `from_user_id` (`from_user_id`),
  CONSTRAINT `tb_errand_evaluation_ibfk_1` FOREIGN KEY (`task_id`) REFERENCES `tb_errand_task` (`id`),
  CONSTRAINT `tb_errand_evaluation_ibfk_2` FOREIGN KEY (`from_user_id`) REFERENCES `tb_student` (`id`),
  CONSTRAINT `tb_errand_evaluation_ibfk_3` FOREIGN KEY (`to_user_id`) REFERENCES `tb_student` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跑腿评价表';

-- ============================================================
-- 9. 商品订单表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) NOT NULL COMMENT '订单号',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `quantity` int DEFAULT 1 COMMENT '购买数量',
  `amount` decimal(10,2) DEFAULT NULL COMMENT '订单金额',
  `status` tinyint DEFAULT 0 COMMENT '0-待支付 1-已支付 2-已取消 3-已完成',
  `expire_time` timestamp NULL DEFAULT NULL COMMENT '订单过期时间',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `pay_time` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_no` (`order_no`),
  KEY `idx_user` (`user_id`),
  KEY `idx_status_expire` (`status`,`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品订单表';

-- ============================================================
-- 10. 点赞记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_like` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `target_type` varchar(20) NOT NULL COMMENT '目标类型 post/comment/product',
  `target_id` bigint NOT NULL COMMENT '目标ID',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`,`target_type`,`target_id`),
  KEY `idx_target` (`target_type`,`target_id`),
  CONSTRAINT `tb_like_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `tb_student` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='点赞记录表';

-- ============================================================
-- 11. 收藏记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_favorite` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `target_type` varchar(20) NOT NULL COMMENT '目标类型 post/product',
  `target_id` bigint NOT NULL COMMENT '目标ID',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`,`target_type`,`target_id`),
  KEY `idx_target` (`target_type`,`target_id`),
  CONSTRAINT `tb_favorite_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `tb_student` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收藏记录表';

-- ============================================================
-- 12. 关注记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_follow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `follower_id` bigint NOT NULL COMMENT '关注者',
  `followee_id` bigint NOT NULL COMMENT '被关注者',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follower_followee` (`follower_id`,`followee_id`),
  KEY `idx_follower` (`follower_id`),
  KEY `idx_followee` (`followee_id`),
  CONSTRAINT `tb_follow_ibfk_1` FOREIGN KEY (`follower_id`) REFERENCES `tb_student` (`id`) ON DELETE CASCADE,
  CONSTRAINT `tb_follow_ibfk_2` FOREIGN KEY (`followee_id`) REFERENCES `tb_student` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='关注记录表';

-- ============================================================
-- 13. 消息通知表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `from_user_id` bigint DEFAULT NULL COMMENT '发送者ID（NULL表示系统消息）',
  `to_user_id` bigint NOT NULL COMMENT '接收者ID',
  `content` varchar(500) NOT NULL COMMENT '内容',
  `type` tinyint DEFAULT 1 COMMENT '类型 1评论 2点赞 3收藏 4关注 5系统',
  `target_type` varchar(20) DEFAULT NULL COMMENT '关联类型',
  `target_id` bigint DEFAULT NULL COMMENT '关联ID',
  `is_read` tinyint DEFAULT 0 COMMENT '是否已读 0未读 1已读',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_to_user` (`to_user_id`,`is_read`),
  KEY `idx_from_user` (`from_user_id`),
  CONSTRAINT `tb_message_ibfk_1` FOREIGN KEY (`from_user_id`) REFERENCES `tb_student` (`id`) ON DELETE SET NULL,
  CONSTRAINT `tb_message_ibfk_2` FOREIGN KEY (`to_user_id`) REFERENCES `tb_student` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息通知表';

-- ============================================================
-- 14. 热度配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_hot_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `target_type` varchar(20) NOT NULL COMMENT '目标类型 post/product',
  `base_score` int DEFAULT 100 COMMENT '基础分',
  `view_weight` float DEFAULT 0.3 COMMENT '浏览权重',
  `like_weight` float DEFAULT 1 COMMENT '点赞权重',
  `comment_weight` float DEFAULT 1.5 COMMENT '评论权重',
  `share_weight` float DEFAULT 2 COMMENT '分享权重',
  `favorite_weight` float DEFAULT 1.2 COMMENT '收藏权重',
  `time_decay` float DEFAULT 1.5 COMMENT '时间衰减因子',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_target_type` (`target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='热度配置表';

-- ============================================================
-- 15. 死信队列表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_dead_letter` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `stream_key` varchar(100) NOT NULL COMMENT 'Stream名称',
  `message` text NOT NULL COMMENT '消息内容',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '错误信息',
  `retry_count` int DEFAULT 0 COMMENT '重试次数',
  `status` tinyint DEFAULT 0 COMMENT '0-待重试 1-已处理 2-最终失败',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='死信队列表';
