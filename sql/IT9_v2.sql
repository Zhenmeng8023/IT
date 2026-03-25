SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `it9_data` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `it9_data`;

-- =============================================
-- IT9 兼容升级版数据库
-- 设计原则：
-- 1. 以 IT8 为兼容主线，保留指定核心表字段。
-- 2. blog 去掉与 project 的直接关联。
-- 3. 吸收 datata 中新增模块，但修复其多态外键冲突问题。
-- 4. 允许在指定保留表后追加新字段，不改动原字段名与原有含义。
-- =============================================

-- 1. 基础字典表
DROP TABLE IF EXISTS `region`;
CREATE TABLE `region` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '地区ID，主键',
  `name` varchar(100) NOT NULL COMMENT '地区名称，例如：北京市、朝阳区',
  `parent_id` bigint DEFAULT NULL COMMENT '父级地区ID，用于构建省市区三级树状结构，顶级区域为NULL',
  `level` enum('province','city','district') NOT NULL COMMENT '地区层级：province-省, city-市, district-区县',
  `code` varchar(20) DEFAULT NULL COMMENT '地区编码，例如国标码 CN-110000',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_region_code` (`code`),
  KEY `parent_id` (`parent_id`),
  CONSTRAINT `region_ibfk_1` FOREIGN KEY (`parent_id`) REFERENCES `region` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='地区信息表，支持多级行政区域划分';

DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '角色ID，主键',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称，例如：管理员、审核员、普通用户',
  `description` text COMMENT '角色的详细描述',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '角色创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '角色信息最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `role_name` (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色表';

DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission` (
  `id` int NOT NULL AUTO_INCREMENT,
  `permission_code` varchar(100) NOT NULL COMMENT '权限代码，例如：blog:review, user:disable',
  `description` text COMMENT '权限的详细描述',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '权限创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `permission_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统权限点表';

DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID，主键',
  `name` varchar(50) NOT NULL COMMENT '标签名称，例如：Java, Spring Boot, Web开发',
  `parent_id` bigint DEFAULT NULL COMMENT '父级标签ID，用于构建技术分类树，顶级分类为NULL',
  `category` enum('tech','language','framework','tool','other') DEFAULT 'tech' COMMENT '标签大类，方便顶层分类',
  `description` text COMMENT '标签的详细描述',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标签创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标签更新时间（新增，不影响IT8兼容）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_name_parent` (`name`,`parent_id`),
  KEY `parent_id` (`parent_id`),
  CONSTRAINT `tag_ibfk_1` FOREIGN KEY (`parent_id`) REFERENCES `tag` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标签信息表，支持树状层级结构，用于对博客和项目进行分类';

DROP TABLE IF EXISTS `membership_level`;
CREATE TABLE `membership_level` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会员等级ID，主键',
  `name` varchar(50) NOT NULL COMMENT '会员等级名称，如 月卡, 年卡',
  `description` text COMMENT '等级描述',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '价格',
  `duration_days` int NOT NULL COMMENT '有效期天数',
  `priority` int NOT NULL DEFAULT '1' COMMENT '优先级，用于排序',
  `benefits` json DEFAULT NULL COMMENT '会员权益，JSON格式存储',
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员等级表';

DROP TABLE IF EXISTS `ai_model`;
CREATE TABLE `ai_model` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'AI模型ID，主键',
  `model_name` varchar(100) NOT NULL COMMENT '模型名称，如 gpt-4, deepseek-chat',
  `model_type` enum('openai','baidu','qwen','deepseek','custom') NOT NULL COMMENT '模型类型',
  `api_key` varchar(255) NOT NULL COMMENT 'API密钥',
  `base_url` varchar(255) DEFAULT NULL COMMENT 'API基础URL',
  `default_params` json DEFAULT NULL COMMENT '模型调用默认参数',
  `is_enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `model_name` (`model_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI模型配置表';

DROP TABLE IF EXISTS `coupon`;
CREATE TABLE `coupon` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '优惠券ID，主键',
  `code` varchar(50) NOT NULL COMMENT '优惠券码，唯一',
  `name` varchar(50) NOT NULL COMMENT '优惠券名称',
  `type` enum('discount','amount_off') NOT NULL COMMENT '类型：discount-折扣, amount_off-减免现金',
  `value` decimal(10,2) NOT NULL COMMENT '优惠值',
  `min_amount` decimal(10,2) DEFAULT '0.00' COMMENT '使用门槛',
  `usage_limit_per_user` int DEFAULT '1' COMMENT '每个用户最多使用次数',
  `total_limit` int DEFAULT '100' COMMENT '总发放数量限制',
  `start_time` datetime NOT NULL COMMENT '开始生效时间',
  `end_time` datetime NOT NULL COMMENT '结束失效时间',
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券表';

DROP TABLE IF EXISTS `recommendation_rule`;
CREATE TABLE `recommendation_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID，主键',
  `name` varchar(100) NOT NULL COMMENT '规则名称',
  `description` text COMMENT '规则描述',
  `rule_type` enum('content_based','collaborative','hot_trending','user_based') DEFAULT 'content_based' COMMENT '推荐算法类型',
  `weight` decimal(5,2) DEFAULT '1.00' COMMENT '规则权重',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '规则是否启用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '规则创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '规则最后更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='推荐系统规则配置表';

-- 2. 菜单与角色权限（保留 IT8 主链路）
DROP TABLE IF EXISTS `menu`;
CREATE TABLE `menu` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '菜单ID，主键',
  `name` varchar(100) NOT NULL COMMENT '菜单名称',
  `parent_id` int DEFAULT NULL COMMENT '父级菜单ID，用于构建菜单树，顶级菜单为NULL',
  `path` varchar(200) DEFAULT NULL COMMENT '前端路由路径',
  `component` varchar(200) DEFAULT NULL COMMENT '前端组件路径',
  `icon` varchar(50) DEFAULT NULL COMMENT '菜单图标',
  `sort_order` int DEFAULT '0' COMMENT '排序序号',
  `is_hidden` tinyint(1) DEFAULT '0' COMMENT '是否隐藏：TRUE-隐藏, FALSE-显示',
  `permission_id` int DEFAULT NULL COMMENT '关联的权限ID，用于控制菜单可见性',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '菜单创建时间',
  PRIMARY KEY (`id`),
  KEY `parent_id` (`parent_id`),
  KEY `permission_id` (`permission_id`),
  CONSTRAINT `menu_ibfk_1` FOREIGN KEY (`parent_id`) REFERENCES `menu` (`id`) ON DELETE CASCADE,
  CONSTRAINT `menu_ibfk_2` FOREIGN KEY (`permission_id`) REFERENCES `permission` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='前端菜单及权限配置表';

DROP TABLE IF EXISTS `role_permission`;
CREATE TABLE `role_permission` (
  `role_id` int NOT NULL COMMENT '角色ID',
  `menu_id` int NOT NULL,
  PRIMARY KEY (`role_id`,`menu_id`),
  KEY `permission_id` (`menu_id`),
  CONSTRAINT `fk_role_menu_menu_id` FOREIGN KEY (`menu_id`) REFERENCES `menu` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `role_permission_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色与菜单的多对多关联表，用于控制角色可以访问的菜单项';

-- 3. 用户主表（保留 IT8 字段，新增字段只追加，不改原名）
DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID，主键',
  `username` varchar(50) NOT NULL COMMENT '用户名，唯一标识',
  `password_hash` varchar(255) NOT NULL COMMENT '用户登录密码的哈希值',
  `email` varchar(100) DEFAULT NULL COMMENT '用户邮箱，唯一',
  `phone` varchar(20) DEFAULT NULL COMMENT '用户手机号',
  `region_id` bigint DEFAULT NULL COMMENT '所属地区ID，关联region表',
  `status` enum('active','inactive','deleted') DEFAULT 'active' COMMENT '账户状态：active-正常, inactive-禁用, deleted-已注销',
  `identity_card` varchar(18) DEFAULT NULL COMMENT '身份证号，唯一绑定，用于实名认证',
  `role_id` int DEFAULT '1' COMMENT '用户角色ID，关联role表，决定用户权限',
  `last_active_at` timestamp NULL DEFAULT NULL COMMENT '最后活跃时间，用于计算用户活跃度',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '账号创建时间',
  `last_login_at` timestamp NULL DEFAULT NULL COMMENT '最后登录时间',
  `login_count` int DEFAULT '0' COMMENT '累计登录次数',
  `avatar_url` varchar(500) DEFAULT NULL COMMENT '用户头像URL地址',
  `bio` text COMMENT '用户个人简介或签名',
  `nickname` varchar(100) DEFAULT NULL COMMENT '用户昵称',
  `birthday` date DEFAULT NULL COMMENT '用户生日',
  `gender` enum('male','female','other') DEFAULT NULL COMMENT '用户性别：male-男, female-女, other-其他',
  `author_tag_id` bigint DEFAULT NULL COMMENT '作者标签ID，关联tag表',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '资料更新时间（新增）',
  `occupation` varchar(100) DEFAULT NULL COMMENT '职业（新增）',
  `points` int DEFAULT '0' COMMENT '积分（新增）',
  `experience` int DEFAULT '0' COMMENT '经验值（新增）',
  `is_premium_member` tinyint(1) DEFAULT '0' COMMENT '是否为高级会员（新增）',
  `premium_expiry_date` timestamp NULL DEFAULT NULL COMMENT '高级会员到期时间（新增）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`),
  UNIQUE KEY `identity_card` (`identity_card`),
  KEY `region_id` (`region_id`),
  KEY `role_id` (`role_id`),
  KEY `fk_user_info_author_tag` (`author_tag_id`),
  CONSTRAINT `fk_user_info_author_tag` FOREIGN KEY (`author_tag_id`) REFERENCES `tag` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `user_info_ibfk_1` FOREIGN KEY (`region_id`) REFERENCES `region` (`id`) ON DELETE SET NULL,
  CONSTRAINT `user_info_ibfk_2` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户基本信息表';

DROP TABLE IF EXISTS `user_skill`;
CREATE TABLE `user_skill` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID，主键',
  `user_id` bigint NOT NULL COMMENT '用户ID，关联user_info表',
  `tag_id` bigint NOT NULL COMMENT '技能标签ID，关联tag表',
  `proficiency_level` enum('beginner','intermediate','advanced','expert') DEFAULT 'intermediate' COMMENT '熟练程度',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关联创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '关联更新时间（新增）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_tag` (`user_id`,`tag_id`),
  KEY `tag_id` (`tag_id`),
  CONSTRAINT `user_skill_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_skill_ibfk_2` FOREIGN KEY (`tag_id`) REFERENCES `tag` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户技能标签关联表，用于展示用户擅长的技术';

DROP TABLE IF EXISTS `membership`;
CREATE TABLE `membership` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会员记录ID，主键',
  `user_id` bigint NOT NULL COMMENT '用户ID，关联user_info表',
  `level_id` bigint NOT NULL COMMENT '会员等级ID，关联membership_level表',
  `start_time` datetime NOT NULL COMMENT '会员生效开始时间',
  `end_time` datetime NOT NULL COMMENT '会员生效结束时间',
  `status` enum('active','expired','cancelled') NOT NULL DEFAULT 'active' COMMENT '状态：active-生效中, expired-已过期, cancelled-已取消',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`,`status`),
  KEY `idx_level` (`level_id`),
  CONSTRAINT `membership_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `membership_ibfk_2` FOREIGN KEY (`level_id`) REFERENCES `membership_level` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户会员表';

-- 4. 项目与博客主业务
DROP TABLE IF EXISTS `project_template`;
CREATE TABLE `project_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID，主键',
  `name` varchar(255) NOT NULL COMMENT '模板名称',
  `description` text COMMENT '模板描述',
  `category` varchar(100) DEFAULT NULL COMMENT '模板分类',
  `creator_id` bigint NOT NULL COMMENT '创建者ID，关联user_info表',
  `is_public` tinyint(1) DEFAULT '0' COMMENT '是否为公共模板',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_template_creator` (`creator_id`),
  CONSTRAINT `project_template_ibfk_1` FOREIGN KEY (`creator_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目模板表';

DROP TABLE IF EXISTS `project`;
CREATE TABLE `project` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目ID，主键',
  `name` varchar(100) NOT NULL COMMENT '项目名称',
  `description` text COMMENT '项目详细描述',
  `category` varchar(50) DEFAULT NULL COMMENT '项目分类，例如：Web应用、移动应用、工具类',
  `size_mb` decimal(10,2) DEFAULT '0.00' COMMENT '项目文件大小，单位MB',
  `stars` int DEFAULT '0' COMMENT '项目星标数/点赞数',
  `downloads` int DEFAULT '0' COMMENT '项目总下载次数',
  `views` int DEFAULT '0' COMMENT '项目总浏览次数',
  `author_id` bigint NOT NULL COMMENT '项目创建者ID，关联user_info表',
  `status` enum('draft','pending','published','rejected','archived') DEFAULT 'draft' COMMENT '项目状态：draft-草稿, pending-待审核, published-已发布, rejected-已驳回, archived-已归档',
  `tags` json DEFAULT NULL COMMENT '项目关联的标签ID列表，以JSON数组形式存储，例如 [3, 4, 6]',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '项目创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '项目最后更新时间',
  `template_id` bigint DEFAULT NULL COMMENT '使用的项目模板ID',
  `visibility` enum('public','friends_only','private') DEFAULT 'public' COMMENT '项目可见性',
  PRIMARY KEY (`id`),
  KEY `idx_author_status` (`author_id`,`status`),
  KEY `idx_template_id` (`template_id`),
  CONSTRAINT `project_ibfk_1` FOREIGN KEY (`author_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_ibfk_2` FOREIGN KEY (`template_id`) REFERENCES `project_template` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目资源表';

DROP TABLE IF EXISTS `blog`;
CREATE TABLE `blog` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '博客ID，主键',
  `title` varchar(200) NOT NULL COMMENT '博客标题',
  `content` longtext NOT NULL COMMENT '博客正文内容',
  `cover_image_url` varchar(500) DEFAULT NULL COMMENT '封面图片URL地址',
  `tags` json DEFAULT NULL COMMENT '博客关联的标签ID列表，以JSON数组形式存储，例如 [1, 2, 5]',
  `author_id` bigint NOT NULL COMMENT '作者ID，关联user_info表',
  `status` enum('draft','pending','published','rejected','archived') DEFAULT 'draft' COMMENT '博客状态：draft-草稿, pending-待审核, published-已发布, rejected-已驳回, archived-已归档',
  `is_marked` tinyint(1) DEFAULT '0' COMMENT '是否被标记为待审核，用于快速筛选未处理内容',
  `publish_time` timestamp NULL DEFAULT NULL COMMENT '博客正式发布时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '博客创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '博客最后更新时间',
  `view_count` int DEFAULT '0' COMMENT '浏览量',
  `like_count` int DEFAULT '0' COMMENT '点赞数',
  `collect_count` int DEFAULT '0' COMMENT '收藏数',
  `download_count` int DEFAULT '0' COMMENT '下载数',
  PRIMARY KEY (`id`),
  KEY `idx_author_status` (`author_id`,`status`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_is_marked` (`is_marked`),
  CONSTRAINT `blog_ibfk_1` FOREIGN KEY (`author_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='博客内容表';

DROP TABLE IF EXISTS `project_file`;
CREATE TABLE `project_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件ID，主键',
  `project_id` bigint NOT NULL COMMENT '所属项目ID，关联project表',
  `file_name` varchar(255) NOT NULL COMMENT '文件原始名称',
  `file_path` varchar(500) NOT NULL COMMENT '文件在服务器上的存储路径',
  `file_size_bytes` bigint DEFAULT NULL COMMENT '文件大小，单位字节(Bytes)',
  `file_type` varchar(50) DEFAULT NULL COMMENT '文件类型，例如 zip, jar, war, exe, pdf',
  `upload_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '文件上传时间',
  `is_main` tinyint(1) DEFAULT '0' COMMENT '是否为主要文件，默认FALSE。一个项目应有且仅有一个主文件',
  `version` varchar(20) DEFAULT '1.0' COMMENT '当前文件版本号',
  `is_latest` tinyint(1) DEFAULT '1' COMMENT '是否为最新版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_file_name` (`project_id`,`file_name`),
  KEY `idx_project_upload_time` (`project_id`,`upload_time`),
  CONSTRAINT `project_file_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目附件文件表';

DROP TABLE IF EXISTS `project_template_file`;
CREATE TABLE `project_template_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板文件ID，主键',
  `template_id` bigint NOT NULL COMMENT '所属模板ID，关联project_template表',
  `file_name` varchar(255) NOT NULL COMMENT '文件原始名称',
  `file_path` varchar(500) NOT NULL COMMENT '文件在服务器上的存储路径',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小，单位字节',
  `mime_type` varchar(100) DEFAULT NULL COMMENT '文件MIME类型',
  `version` varchar(20) DEFAULT '1.0' COMMENT '文件版本号',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `template_id` (`template_id`),
  CONSTRAINT `project_template_file_ibfk_1` FOREIGN KEY (`template_id`) REFERENCES `project_template` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目模板文件表';

DROP TABLE IF EXISTS `project_member`;
CREATE TABLE `project_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '成员关系ID，主键',
  `project_id` bigint NOT NULL COMMENT '项目ID，关联project表',
  `user_id` bigint NOT NULL COMMENT '用户ID，关联user_info表',
  `role` enum('owner','admin','member','viewer') NOT NULL DEFAULT 'member' COMMENT '成员角色：owner-所有者, admin-管理员, member-成员, viewer-查看者',
  `joined_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `status` enum('active','inactive') DEFAULT 'active' COMMENT '成员状态：active-活跃, inactive-非活跃',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_user` (`project_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `project_member_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_member_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目成员表';

DROP TABLE IF EXISTS `project_task`;
CREATE TABLE `project_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID，主键',
  `project_id` bigint NOT NULL COMMENT '所属项目ID，关联project表',
  `title` varchar(255) NOT NULL COMMENT '任务标题',
  `description` text COMMENT '任务描述',
  `status` enum('todo','in_progress','done') DEFAULT 'todo' COMMENT '任务状态：todo-待办, in_progress-进行中, done-已完成',
  `priority` enum('low','medium','high','urgent') DEFAULT 'medium' COMMENT '任务优先级：low-低, medium-中, high-高, urgent-紧急',
  `assignee_id` bigint DEFAULT NULL COMMENT '负责人ID，关联user_info表',
  `due_date` datetime DEFAULT NULL COMMENT '截止日期',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID，关联user_info表',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `project_id` (`project_id`),
  KEY `assignee_id` (`assignee_id`),
  KEY `created_by` (`created_by`),
  CONSTRAINT `project_task_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_task_ibfk_2` FOREIGN KEY (`assignee_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL,
  CONSTRAINT `project_task_ibfk_3` FOREIGN KEY (`created_by`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目任务表';

DROP TABLE IF EXISTS `project_file_version`;
CREATE TABLE `project_file_version` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '版本ID，主键',
  `file_id` bigint NOT NULL COMMENT '所属文件ID，关联project_file表',
  `version` varchar(20) NOT NULL COMMENT '版本号，如 1.0.1',
  `server_path` varchar(500) NOT NULL COMMENT '该版本文件在服务器上的存储路径',
  `file_size_bytes` bigint DEFAULT NULL COMMENT '该版本文件大小，单位字节',
  `uploaded_by` bigint DEFAULT NULL COMMENT '上传者ID，关联user_info表',
  `commit_message` text COMMENT '本次版本更新的提交信息',
  `uploaded_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_version` (`file_id`,`version`),
  KEY `uploaded_by` (`uploaded_by`),
  CONSTRAINT `project_file_version_ibfk_1` FOREIGN KEY (`file_id`) REFERENCES `project_file` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_file_version_ibfk_2` FOREIGN KEY (`uploaded_by`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目文件版本表';

-- 5. 圈子与社交
DROP TABLE IF EXISTS `circle`;
CREATE TABLE `circle` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '圈子ID，主键',
  `name` varchar(100) NOT NULL COMMENT '圈子名称',
  `type` enum('official','private','public') DEFAULT 'public' COMMENT '圈子类型：official-官方, private-私密, public-公开',
  `description` text COMMENT '圈子描述',
  `creator_id` bigint NOT NULL COMMENT '创建者ID，关联user_info表',
  `visibility` enum('public','private') DEFAULT 'public' COMMENT '可见性：public-公开, private-私有',
  `max_members` int DEFAULT '500' COMMENT '最大成员数量限制',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '圈子创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '圈子最后更新时间',
  PRIMARY KEY (`id`),
  KEY `creator_id` (`creator_id`),
  CONSTRAINT `circle_ibfk_1` FOREIGN KEY (`creator_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='兴趣圈子表，支持群聊功能';

DROP TABLE IF EXISTS `conversation`;
CREATE TABLE `conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID，主键',
  `type` enum('private','group') NOT NULL COMMENT '会话类型：private-私信, group-群聊',
  `name` varchar(100) DEFAULT NULL COMMENT '会话名称（群聊时使用）',
  `creator_id` bigint DEFAULT NULL COMMENT '创建者ID（群聊时使用）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '会话创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '会话最后更新时间（最后一条消息时间）',
  `ai_type` enum('openai','baidu','qwen','deepseek','custom') DEFAULT NULL COMMENT 'AI类型（新增）',
  `ai_model_name` varchar(100) DEFAULT NULL COMMENT '本次会话使用的AI模型名称（新增）',
  `ai_config_params` json DEFAULT NULL COMMENT '本次会话使用的AI配置参数（新增）',
  PRIMARY KEY (`id`),
  KEY `creator_id` (`creator_id`),
  CONSTRAINT `conversation_ibfk_1` FOREIGN KEY (`creator_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='私信/群聊会话表';

DROP TABLE IF EXISTS `follow`;
CREATE TABLE `follow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关注关系ID，主键',
  `follower_id` bigint NOT NULL COMMENT '关注者ID，关联user_info表',
  `followee_id` bigint NOT NULL COMMENT '被关注者ID，关联user_info表',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注建立时间',
  `status` enum('active','blocked') DEFAULT 'active' COMMENT '关注状态：active-正常关注, blocked-已拉黑',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follower_followee` (`follower_id`,`followee_id`),
  KEY `idx_followee` (`followee_id`),
  CONSTRAINT `follow_ibfk_1` FOREIGN KEY (`follower_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `follow_ibfk_2` FOREIGN KEY (`followee_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户关注关系表（单向）';

DROP TABLE IF EXISTS `friendship`;
CREATE TABLE `friendship` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '好友关系ID，主键',
  `user1_id` bigint NOT NULL COMMENT '用户A的ID，关联user_info表',
  `user2_id` bigint NOT NULL COMMENT '用户B的ID，关联user_info表',
  `status` enum('pending','accepted','rejected','blocked') DEFAULT 'pending' COMMENT '好友请求状态',
  `request_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '请求发起时间',
  `response_time` timestamp NULL DEFAULT NULL COMMENT '请求响应时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_pair` (`user1_id`,`user2_id`),
  KEY `idx_user1_status` (`user1_id`,`status`),
  KEY `idx_user2_status` (`user2_id`,`status`),
  CONSTRAINT `friendship_ibfk_1` FOREIGN KEY (`user1_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `friendship_ibfk_2` FOREIGN KEY (`user2_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户好友关系表（双向确认）';

DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID，主键',
  `content` text NOT NULL COMMENT '评论正文内容',
  `parent_comment_id` bigint DEFAULT NULL COMMENT '父级评论ID，用于实现多级评论回复，顶级评论为NULL',
  `post_id` bigint NOT NULL COMMENT '被评论内容的ID',
  `author_id` bigint NOT NULL COMMENT '评论者ID，关联user_info表',
  `likes` int DEFAULT '0' COMMENT '该条评论的点赞数',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论创建时间',
  `status` enum('normal','hidden','deleted') DEFAULT 'normal' COMMENT '评论状态：normal-正常显示, hidden-隐藏, deleted-已删除',
  `post_type` enum('blog','project') DEFAULT 'blog' COMMENT '评论目标类型（新增，解决 post_id 语义冲突）',
  PRIMARY KEY (`id`),
  KEY `idx_post_type_post_id` (`post_id`) COMMENT '按ID快速查找评论',
  KEY `idx_author` (`author_id`),
  KEY `idx_parent` (`parent_comment_id`),
  KEY `idx_comment_post_type_post_id` (`post_type`,`post_id`),
  CONSTRAINT `comment_ibfk_1` FOREIGN KEY (`author_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `comment_ibfk_2` FOREIGN KEY (`parent_comment_id`) REFERENCES `comment` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评论表，支持对博客和项目进行评论和回复';

DROP TABLE IF EXISTS `circle_comment`;
CREATE TABLE `circle_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '圈子评论/帖子ID，主键',
  `content` text NOT NULL COMMENT '评论/帖子正文内容',
  `parent_comment_id` bigint DEFAULT NULL COMMENT '父级评论ID，用于区分是主题帖还是回帖。NULL表示这是一个独立的主题帖',
  `post_id` bigint NOT NULL COMMENT '被评论的圈子动态ID或圈子ID本身，当parent_comment_id为NULL时，指代被评论的圈子动态；当parent_comment_id有值时，此ID与父评论的post_id相同',
  `circle_id` bigint DEFAULT NULL COMMENT '关联的圈子ID，指向circle表',
  `author_id` bigint NOT NULL COMMENT '作者ID，关联user_info表',
  `likes` int DEFAULT '0' COMMENT '该评论/帖子的点赞数',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论/帖子创建时间',
  `status` enum('normal','hidden','deleted') DEFAULT 'normal' COMMENT '评论/帖子状态：normal-正常显示, hidden-隐藏, deleted-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_comment_id`) COMMENT '按父级评论ID索引，快速查找对某个主题的回复',
  KEY `idx_author` (`author_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_circle_id` (`circle_id`),
  CONSTRAINT `circle_comment_ibfk_1` FOREIGN KEY (`author_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `circle_comment_ibfk_2` FOREIGN KEY (`parent_comment_id`) REFERENCES `circle_comment` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_circle_comment_circle` FOREIGN KEY (`circle_id`) REFERENCES `circle` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='圈子评论表，用于存储圈子内的主题帖和对主题的回帖';

DROP TABLE IF EXISTS `circle_member`;
CREATE TABLE `circle_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '成员关系ID，主键',
  `circle_id` bigint NOT NULL COMMENT '圈子ID，关联circle表',
  `user_id` bigint NOT NULL COMMENT '用户ID，关联user_info表',
  `join_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入圈子的时间',
  `status` enum('active','banned','left') DEFAULT 'active' COMMENT '成员状态：active-活跃, banned-被封禁, left-已退出',
  `role` enum('owner','admin','moderator','member') DEFAULT 'member' COMMENT '成员角色：owner-圈主, admin-管理员, moderator-版主, member-普通成员',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_circle_user` (`circle_id`,`user_id`) COMMENT '确保一个用户在一个圈子中只有一条记录',
  KEY `idx_user_status` (`user_id`,`status`),
  CONSTRAINT `circle_member_ibfk_1` FOREIGN KEY (`circle_id`) REFERENCES `circle` (`id`) ON DELETE CASCADE,
  CONSTRAINT `circle_member_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='圈子成员关系及权限表';

DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID，主键',
  `conversation_id` bigint NOT NULL COMMENT '所属会话ID，关联conversation表',
  `sender_id` bigint NOT NULL COMMENT '发送者ID，关联user_info表',
  `content` text NOT NULL COMMENT '消息内容',
  `message_type` enum('text','image','file','emoji') DEFAULT 'text' COMMENT '消息类型',
  `sent_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息发送时间',
  `is_read` tinyint(1) DEFAULT '0' COMMENT '是否已读',
  `is_ai_response` tinyint(1) DEFAULT '0' COMMENT '是否为AI生成的回复（新增）',
  `ai_model_used` varchar(100) DEFAULT NULL COMMENT '生成此消息所使用的AI模型名称（新增）',
  PRIMARY KEY (`id`),
  KEY `sender_id` (`sender_id`),
  KEY `idx_conversation_sent` (`conversation_id`,`sent_at`),
  CONSTRAINT `message_ibfk_1` FOREIGN KEY (`conversation_id`) REFERENCES `conversation` (`id`) ON DELETE CASCADE,
  CONSTRAINT `message_ibfk_2` FOREIGN KEY (`sender_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='私信/群聊消息表';

DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID，主键',
  `receiver_id` bigint NOT NULL COMMENT '接收者ID，关联user_info表',
  `sender_id` bigint DEFAULT NULL COMMENT '发送者ID，关联user_info表，系统通知则为NULL',
  `type` enum('comment','like','follow','reply','system','friend_request','message','order','membership') NOT NULL COMMENT '通知类型',
  `content` text NOT NULL COMMENT '通知的具体内容',
  `read_status` tinyint(1) DEFAULT '0' COMMENT '阅读状态：FALSE-未读, TRUE-已读',
  `target_type` enum('blog','project','comment','circle','conversation','paid_content','order','membership_level') DEFAULT NULL COMMENT '被操作的目标类型',
  `target_id` bigint DEFAULT NULL COMMENT '被操作的目标ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '通知创建时间',
  PRIMARY KEY (`id`),
  KEY `sender_id` (`sender_id`),
  KEY `idx_receiver_read` (`receiver_id`,`read_status`),
  KEY `idx_created_at` (`created_at` DESC),
  CONSTRAINT `notification_ibfk_1` FOREIGN KEY (`receiver_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `notification_ibfk_2` FOREIGN KEY (`sender_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户消息通知中心表';

DROP TABLE IF EXISTS `collect_record`;
CREATE TABLE `collect_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏记录ID，主键',
  `user_id` bigint NOT NULL COMMENT '收藏者ID，关联user_info表',
  `target_type` enum('blog','project') NOT NULL COMMENT '被收藏的目标类型：blog-博客, project-项目',
  `target_id` bigint NOT NULL COMMENT '被收藏目标的ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`,`target_type`,`target_id`) COMMENT '防止同一用户重复收藏同一内容',
  KEY `idx_target` (`target_type`,`target_id`),
  CONSTRAINT `collect_record_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户收藏夹记录表';

DROP TABLE IF EXISTS `like_record`;
CREATE TABLE `like_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '点赞记录ID，主键',
  `user_id` bigint NOT NULL COMMENT '点赞用户ID，关联user_info表',
  `target_type` enum('blog','comment') NOT NULL COMMENT '被点赞的目标类型：blog-博客, comment-评论',
  `target_id` bigint NOT NULL COMMENT '被点赞目标的ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞发生时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`,`target_type`,`target_id`) COMMENT '防止同一用户重复对同一目标点赞',
  KEY `idx_target` (`target_type`,`target_id`),
  CONSTRAINT `like_record_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户点赞行为记录表';

-- 6. 推荐、审计、搜索、行为、浏览
DROP TABLE IF EXISTS `recommendation_result`;
CREATE TABLE `recommendation_result` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '推荐结果ID，主键',
  `user_id` bigint NOT NULL COMMENT '目标用户ID，关联user_info表',
  `algorithm_version` varchar(50) NOT NULL COMMENT '生成本次推荐的算法版本号',
  `recommended_items` json NOT NULL COMMENT '推荐项目列表，JSON格式',
  `generated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '推荐结果生成时间',
  `consumed` tinyint(1) DEFAULT '0' COMMENT '是否已被消费（例如：是否已推送给用户）',
  `consumed_at` timestamp NULL DEFAULT NULL COMMENT '消费时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_generated` (`user_id`,`generated_at` DESC),
  KEY `idx_consumed` (`consumed`),
  CONSTRAINT `recommendation_result_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='为用户生成的个性化推荐列表缓存表';

DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '审计日志ID，主键',
  `user_id` bigint DEFAULT NULL COMMENT '操作者ID，关联user_info表，系统操作则为NULL',
  `action` varchar(50) NOT NULL COMMENT '执行的操作，例如：create_blog, delete_project, update_user_profile',
  `target_type` varchar(50) DEFAULT NULL COMMENT '操作目标的类型，例如：blog, project, user',
  `target_id` bigint DEFAULT NULL COMMENT '操作目标的ID',
  `ip_address` varchar(45) DEFAULT NULL COMMENT '操作者的IP地址',
  `user_agent` text COMMENT '操作者使用的浏览器或客户端信息',
  `details` json DEFAULT NULL COMMENT '操作的详细信息，以JSON格式记录，例如旧值、新值等',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作发生时间',
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `audit_log_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统操作审计日志表，用于安全审计和问题追溯';

DROP TABLE IF EXISTS `report`;
CREATE TABLE `report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '举报ID，主键',
  `reporter_id` bigint NOT NULL COMMENT '举报人ID，关联user_info表',
  `target_type` enum('blog','project','comment','user','circle','paid_content','order') NOT NULL COMMENT '被举报目标类型',
  `target_id` bigint NOT NULL COMMENT '被举报目标的ID',
  `reason` varchar(500) NOT NULL COMMENT '举报原因描述',
  `status` enum('pending','processed','ignored') DEFAULT 'pending' COMMENT '处理状态',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报提交时间',
  `processed_at` timestamp NULL DEFAULT NULL COMMENT '处理完成时间',
  `processor_id` bigint DEFAULT NULL COMMENT '处理该举报的管理员ID，关联user_info表',
  PRIMARY KEY (`id`),
  KEY `reporter_id` (`reporter_id`),
  KEY `processor_id` (`processor_id`),
  KEY `idx_status` (`status`),
  KEY `idx_target` (`target_type`,`target_id`),
  CONSTRAINT `report_ibfk_1` FOREIGN KEY (`reporter_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `report_ibfk_2` FOREIGN KEY (`processor_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户举报内容记录表';

DROP TABLE IF EXISTS `search_index`;
CREATE TABLE `search_index` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '索引ID，主键',
  `content` text COMMENT '用于搜索的文本内容',
  `doc_type` enum('blog','project','user','circle') NOT NULL COMMENT '文档类型',
  `doc_id` bigint NOT NULL COMMENT '对应文档的ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '索引创建时间',
  PRIMARY KEY (`id`),
  FULLTEXT KEY `idx_content` (`content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='全文搜索引擎索引表';

DROP TABLE IF EXISTS `user_behavior`;
CREATE TABLE `user_behavior` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '行为记录ID，主键',
  `user_id` bigint NOT NULL COMMENT '用户ID，关联user_info表',
  `behavior_type` enum('login','logout','view','like','collect','comment','share','upload','download','purchase') NOT NULL COMMENT '行为类型',
  `target_type` enum('blog','project','comment','circle','user','paid_content','order') DEFAULT NULL COMMENT '行为对象类型',
  `target_id` bigint DEFAULT NULL COMMENT '行为对象ID',
  `extra_data` json DEFAULT NULL COMMENT '额外的行为数据，如搜索关键词、操作参数等',
  `ip_address` varchar(45) DEFAULT NULL COMMENT '用户IP地址',
  `user_agent` text COMMENT '用户代理字符串',
  `occurred_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '行为发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_behavior` (`user_id`,`behavior_type`),
  KEY `idx_occurred_at` (`occurred_at` DESC),
  CONSTRAINT `user_behavior_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户行为详细记录表';

DROP TABLE IF EXISTS `view_log`;
CREATE TABLE `view_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '浏览记录ID，主键',
  `user_id` bigint DEFAULT NULL COMMENT '浏览者ID，关联user_info表，匿名浏览则为NULL',
  `target_type` enum('blog','project','paid_content') NOT NULL COMMENT '被浏览的目标类型',
  `target_id` bigint NOT NULL COMMENT '被浏览目标的ID',
  `ip_address` varchar(45) DEFAULT NULL COMMENT '浏览者IP地址，用于统计和安全分析',
  `user_agent` text COMMENT '用户代理字符串，记录浏览器等客户端信息',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_target` (`user_id`,`target_type`,`target_id`),
  KEY `idx_created_at` (`created_at` DESC),
  CONSTRAINT `view_log_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内容浏览日志表';

-- 7. 付费内容与订单（修复 datata 中的多态外键冲突）
DROP TABLE IF EXISTS `paid_content`;
CREATE TABLE `paid_content` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '付费内容ID，主键',
  `content_type` enum('blog','project') NOT NULL COMMENT '内容类型：blog-博客, project-项目',
  `content_id` bigint NOT NULL COMMENT '逻辑内容ID，与 blog_id 或 project_id 保持一致',
  `blog_id` bigint DEFAULT NULL COMMENT '当 content_type=blog 时关联 blog.id',
  `project_id` bigint DEFAULT NULL COMMENT '当 content_type=project 时关联 project.id',
  `title` varchar(255) NOT NULL COMMENT '内容标题（冗余字段，方便查询）',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '价格',
  `access_type` enum('one_time','member_only') NOT NULL DEFAULT 'one_time' COMMENT '访问方式：one_time-一次性购买, member_only-仅限会员',
  `required_membership_level_id` bigint DEFAULT NULL COMMENT '如果 access_type=member_only，此项指定所需会员等级ID',
  `description` text COMMENT '付费内容描述',
  `status` enum('draft','published','disabled') NOT NULL DEFAULT 'draft' COMMENT '状态：draft-草稿, published-已发布, disabled-已禁用',
  `created_by` bigint DEFAULT NULL COMMENT '创建者ID，关联user_info表',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_paid_content_blog` (`blog_id`),
  UNIQUE KEY `uk_paid_content_project` (`project_id`),
  KEY `idx_content_type_id` (`content_type`,`content_id`),
  KEY `idx_created_by` (`created_by`),
  KEY `idx_required_level` (`required_membership_level_id`),
  CONSTRAINT `paid_content_ibfk_1` FOREIGN KEY (`blog_id`) REFERENCES `blog` (`id`) ON DELETE CASCADE,
  CONSTRAINT `paid_content_ibfk_2` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `paid_content_ibfk_3` FOREIGN KEY (`created_by`) REFERENCES `user_info` (`id`) ON DELETE SET NULL,
  CONSTRAINT `paid_content_ibfk_4` FOREIGN KEY (`required_membership_level_id`) REFERENCES `membership_level` (`id`) ON DELETE SET NULL,
  CONSTRAINT `chk_paid_content_ref` CHECK (
    (`content_type` = 'blog' AND `blog_id` IS NOT NULL AND `project_id` IS NULL AND `content_id` = `blog_id`) OR
    (`content_type` = 'project' AND `project_id` IS NOT NULL AND `blog_id` IS NULL AND `content_id` = `project_id`)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='付费内容表，修复了 datata 中一个字段同时外键到 blog/project 的冲突问题';

DROP TABLE IF EXISTS `order`;
CREATE TABLE `order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID，主键',
  `order_no` varchar(64) NOT NULL COMMENT '订单号，全局唯一',
  `user_id` bigint NOT NULL COMMENT '购买用户ID，关联user_info表',
  `type` enum('content','membership') NOT NULL COMMENT '订单类型：content-内容购买, membership-会员订阅',
  `target_id` bigint DEFAULT NULL COMMENT '逻辑目标ID，兼容统一查询',
  `paid_content_id` bigint DEFAULT NULL COMMENT '当 type=content 时关联 paid_content.id',
  `membership_level_id` bigint DEFAULT NULL COMMENT '当 type=membership 时关联 membership_level.id',
  `amount` decimal(10,2) NOT NULL COMMENT '订单总金额',
  `payment_method` varchar(50) DEFAULT NULL COMMENT '支付方式，如 wechat, alipay',
  `status` enum('pending','paid','refunded','failed') NOT NULL DEFAULT 'pending' COMMENT '订单状态',
  `pay_time` datetime DEFAULT NULL COMMENT '支付成功时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_order_content` (`paid_content_id`),
  KEY `idx_order_membership` (`membership_level_id`),
  KEY `idx_type_target` (`type`,`target_id`),
  CONSTRAINT `order_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `order_ibfk_2` FOREIGN KEY (`paid_content_id`) REFERENCES `paid_content` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `order_ibfk_3` FOREIGN KEY (`membership_level_id`) REFERENCES `membership_level` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_order_ref` CHECK (
    (`type` = 'content' AND `paid_content_id` IS NOT NULL AND `membership_level_id` IS NULL AND (`target_id` IS NULL OR `target_id` = `paid_content_id`)) OR
    (`type` = 'membership' AND `membership_level_id` IS NOT NULL AND `paid_content_id` IS NULL AND (`target_id` IS NULL OR `target_id` = `membership_level_id`))
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单表，修复了 datata 中一个字段同时外键到 paid_content/membership_level 的冲突问题';

DROP TABLE IF EXISTS `payment_record`;
CREATE TABLE `payment_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '支付记录ID，主键',
  `order_id` bigint NOT NULL COMMENT '关联订单ID，关联order表',
  `payment_platform` varchar(50) NOT NULL COMMENT '支付平台，如 wechat_pay, alipay',
  `transaction_id` varchar(100) DEFAULT NULL COMMENT '支付平台的交易ID',
  `payment_status` varchar(50) NOT NULL COMMENT '支付状态，如 success, failed',
  `payment_amount` decimal(10,2) NOT NULL COMMENT '实际支付金额',
  `payment_time` datetime DEFAULT NULL COMMENT '支付完成时间',
  `callback_data` json DEFAULT NULL COMMENT '支付平台回调的原始数据',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  CONSTRAINT `payment_record_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `order` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付记录表';

DROP TABLE IF EXISTS `revenue_record`;
CREATE TABLE `revenue_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收益记录ID，主键',
  `order_id` bigint NOT NULL COMMENT '关联订单ID，关联order表',
  `source_user_id` bigint DEFAULT NULL COMMENT '收益来源用户ID（通常是内容作者）',
  `platform_revenue` decimal(10,2) NOT NULL COMMENT '平台分成金额',
  `author_revenue` decimal(10,2) NOT NULL COMMENT '作者分成金额',
  `settlement_status` enum('unsettled','settled') NOT NULL DEFAULT 'unsettled' COMMENT '结算状态',
  `settled_at` datetime DEFAULT NULL COMMENT '结算时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_source_user` (`source_user_id`),
  KEY `idx_settlement_status` (`settlement_status`),
  CONSTRAINT `revenue_record_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `order` (`id`) ON DELETE CASCADE,
  CONSTRAINT `revenue_record_ibfk_2` FOREIGN KEY (`source_user_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收益记录表';

DROP TABLE IF EXISTS `user_purchase`;
CREATE TABLE `user_purchase` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '购买记录ID，主键',
  `user_id` bigint NOT NULL COMMENT '购买用户ID，关联user_info表',
  `paid_content_id` bigint NOT NULL COMMENT '购买的付费内容ID，关联paid_content表',
  `order_id` bigint NOT NULL COMMENT '关联的订单ID，关联order表',
  `purchase_time` datetime NOT NULL COMMENT '购买时间',
  `access_expired_at` datetime DEFAULT NULL COMMENT '访问过期时间（如果是一次性购买）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_content` (`user_id`,`paid_content_id`),
  KEY `idx_paid_content_id` (`paid_content_id`),
  KEY `idx_order_id` (`order_id`),
  CONSTRAINT `user_purchase_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_purchase_ibfk_2` FOREIGN KEY (`paid_content_id`) REFERENCES `paid_content` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_purchase_ibfk_3` FOREIGN KEY (`order_id`) REFERENCES `order` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户购买记录表';

DROP TABLE IF EXISTS `content_access`;
CREATE TABLE `content_access` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问记录ID，主键',
  `user_id` bigint NOT NULL COMMENT '访问用户ID，关联user_info表',
  `paid_content_id` bigint NOT NULL COMMENT '访问的付费内容ID，关联paid_content表',
  `access_time` datetime NOT NULL COMMENT '访问时间',
  `ip_address` varchar(45) DEFAULT NULL COMMENT '访问者IP地址',
  `user_agent` text COMMENT '访问者User-Agent信息',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_content` (`user_id`,`paid_content_id`),
  KEY `idx_paid_content_time` (`paid_content_id`,`access_time`),
  CONSTRAINT `content_access_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `content_access_ibfk_2` FOREIGN KEY (`paid_content_id`) REFERENCES `paid_content` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内容访问记录表';

SET FOREIGN_KEY_CHECKS = 1;
