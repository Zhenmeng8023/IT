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


-- 8. 项目协作平台扩展表（在保留原有数据库结构基础上追加）
-- 说明：
-- 1. 本节所有表均为新增扩展表，不会破坏原有 IT9_v2 主表结构。
-- 2. 主要用于补充项目扩展信息、成员协作流程、任务增强、发布管理、文档管理、统计分析、外部工具集成、审核留痕。
-- 3. 建表顺序已按外键依赖关系排列，可直接执行。

DROP TABLE IF EXISTS `project_profile`;
CREATE TABLE `project_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '扩展信息ID，主键',
  `project_id` bigint NOT NULL COMMENT '项目ID，关联project表，用于标识这份扩展资料属于哪个项目',
  `cover_image_url` varchar(500) DEFAULT NULL COMMENT '项目封面图地址，用于项目列表页、详情页展示',
  `repo_url` varchar(500) DEFAULT NULL COMMENT '代码仓库地址，例如GitHub、Gitee、GitLab仓库链接',
  `demo_url` varchar(500) DEFAULT NULL COMMENT '项目在线演示地址，例如前端预览地址、部署后的访问地址',
  `docs_url` varchar(500) DEFAULT NULL COMMENT '项目外部文档地址，例如在线说明文档、接口文档地址',
  `license_name` varchar(100) DEFAULT NULL COMMENT '项目许可证名称，例如MIT、Apache-2.0、GPL-3.0',
  `tech_stack` json DEFAULT NULL COMMENT '项目技术栈，JSON格式存储，例如前端框架、后端框架、数据库、中间件等',
  `environment_requirements` text COMMENT '运行环境要求，例如JDK版本、Node版本、MySQL版本、Redis版本等',
  `install_guide` longtext COMMENT '安装部署说明，记录项目从下载安装到启动运行的完整步骤',
  `usage_guide` longtext COMMENT '使用说明，记录项目功能介绍、使用步骤、注意事项等',
  `source_code_access` enum('public','private','request') DEFAULT 'public' COMMENT '源码访问方式：public公开可见，private私有不可见，request申请后可见',
  `contact_email` varchar(100) DEFAULT NULL COMMENT '项目联系邮箱，用于合作、沟通、问题反馈',
  `contact_qrcode_url` varchar(500) DEFAULT NULL COMMENT '联系二维码图片地址，例如微信二维码、QQ群二维码等',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '扩展信息创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '扩展信息最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_profile_project` (`project_id`),
  CONSTRAINT `project_profile_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目扩展信息表：补充项目封面、仓库地址、演示地址、技术栈、部署说明等展示和说明性资料';

DROP TABLE IF EXISTS `project_media`;
CREATE TABLE `project_media` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '媒体资源ID，主键',
  `project_id` bigint NOT NULL COMMENT '项目ID，关联project表，表示该媒体属于哪个项目',
  `media_type` enum('image','video') NOT NULL DEFAULT 'image' COMMENT '媒体类型：image图片，video视频',
  `title` varchar(255) DEFAULT NULL COMMENT '媒体标题，用于前端展示说明',
  `media_url` varchar(500) NOT NULL COMMENT '媒体资源地址，例如项目截图、演示视频地址',
  `thumbnail_url` varchar(500) DEFAULT NULL COMMENT '缩略图地址，视频可存封面图，图片可存压缩图',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序值，值越小越靠前，用于控制媒体展示顺序',
  `created_by` bigint DEFAULT NULL COMMENT '上传人ID，关联user_info表',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  KEY `idx_project_sort` (`project_id`,`sort_order`),
  KEY `idx_created_by` (`created_by`),
  CONSTRAINT `project_media_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_media_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目媒体资源表：存储项目截图、轮播图、演示视频等展示素材';

DROP TABLE IF EXISTS `project_join_request`;
CREATE TABLE `project_join_request` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '加入申请ID，主键',
  `project_id` bigint NOT NULL COMMENT '申请加入的项目ID，关联project表',
  `applicant_id` bigint NOT NULL COMMENT '申请人用户ID，关联user_info表',
  `desired_role` enum('member','viewer') NOT NULL DEFAULT 'member' COMMENT '申请希望获得的角色：member普通成员，viewer只读成员',
  `apply_message` varchar(500) DEFAULT NULL COMMENT '申请说明或备注，例如申请理由、自我介绍等',
  `status` enum('pending','approved','rejected','cancelled','expired') NOT NULL DEFAULT 'pending' COMMENT '申请状态：pending待审核，approved已通过，rejected已拒绝，cancelled申请人取消，expired已过期',
  `reviewer_id` bigint DEFAULT NULL COMMENT '审核人ID，关联user_info表，通常为项目所有者或管理员',
  `review_message` varchar(500) DEFAULT NULL COMMENT '审核备注，例如拒绝理由、通过说明',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '申请最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_project_status` (`project_id`,`status`),
  KEY `idx_applicant_status` (`applicant_id`,`status`),
  KEY `idx_reviewer` (`reviewer_id`),
  CONSTRAINT `project_join_request_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_join_request_ibfk_2` FOREIGN KEY (`applicant_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_join_request_ibfk_3` FOREIGN KEY (`reviewer_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目加入申请表：用于用户主动申请加入项目，支持审批流';

DROP TABLE IF EXISTS `project_invitation`;
CREATE TABLE `project_invitation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '邀请记录ID，主键',
  `project_id` bigint NOT NULL COMMENT '被邀请加入的项目ID，关联project表',
  `inviter_id` bigint NOT NULL COMMENT '邀请人ID，关联user_info表，通常为项目owner或admin',
  `invitee_id` bigint DEFAULT NULL COMMENT '被邀请用户ID，适用于站内已有用户的邀请场景',
  `invitee_email` varchar(100) DEFAULT NULL COMMENT '被邀请邮箱，适用于通过邮箱邀请未注册用户或外部用户',
  `invite_role` enum('admin','member','viewer') NOT NULL DEFAULT 'member' COMMENT '邀请加入后的角色：admin管理员，member普通成员，viewer只读成员',
  `invite_code` varchar(64) NOT NULL COMMENT '邀请唯一编码，用于生成邀请链接、扫码加入等',
  `invite_message` varchar(500) DEFAULT NULL COMMENT '邀请附言，例如邀请说明、合作介绍',
  `status` enum('pending','accepted','rejected','cancelled','expired') NOT NULL DEFAULT 'pending' COMMENT '邀请状态：pending待处理，accepted已接受，rejected已拒绝，cancelled已取消，expired已过期',
  `expired_at` datetime DEFAULT NULL COMMENT '邀请过期时间',
  `responded_at` datetime DEFAULT NULL COMMENT '被邀请人响应时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '邀请创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '邀请最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invite_code` (`invite_code`),
  KEY `idx_project_status` (`project_id`,`status`),
  KEY `idx_invitee_status` (`invitee_id`,`status`),
  KEY `idx_inviter` (`inviter_id`),
  CONSTRAINT `project_invitation_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_invitation_ibfk_2` FOREIGN KEY (`inviter_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_invitation_ibfk_3` FOREIGN KEY (`invitee_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目邀请表：用于项目管理员主动邀请用户加入项目';

DROP TABLE IF EXISTS `project_activity_log`;
CREATE TABLE `project_activity_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目动态ID，主键',
  `project_id` bigint NOT NULL COMMENT '所属项目ID，关联project表',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID，关联user_info表，系统自动操作时可为空',
  `action` varchar(100) NOT NULL COMMENT '操作动作标识，例如create_project、add_member、upload_file、publish_release',
  `target_type` enum('project','profile','member','join_request','invitation','milestone','sprint','task','task_comment','file','file_version','release','doc','integration') NOT NULL COMMENT '操作目标类型，用于区分本条动态是针对什么对象产生的',
  `target_id` bigint DEFAULT NULL COMMENT '目标记录ID，即对应目标类型在其表中的主键ID',
  `summary` varchar(255) DEFAULT NULL COMMENT '动态摘要，前端可直接展示，例如“张三上传了版本 v1.0.2”',
  `details` json DEFAULT NULL COMMENT '动态详细数据，JSON格式，可保存字段变更前后值、附加信息等',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '动态创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_project_created` (`project_id`,`created_at` DESC),
  KEY `idx_operator` (`operator_id`),
  CONSTRAINT `project_activity_log_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_activity_log_ibfk_2` FOREIGN KEY (`operator_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目动态日志表：记录项目中的关键操作，供项目动态流、审计追踪、通知推送使用';

DROP TABLE IF EXISTS `project_milestone`;
CREATE TABLE `project_milestone` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '里程碑ID，主键',
  `project_id` bigint NOT NULL COMMENT '所属项目ID，关联project表',
  `name` varchar(255) NOT NULL COMMENT '里程碑名称，例如“第一阶段开发完成”',
  `description` text COMMENT '里程碑说明，描述该阶段目标或验收标准',
  `status` enum('planned','active','completed','cancelled') NOT NULL DEFAULT 'planned' COMMENT '里程碑状态：planned计划中，active进行中，completed已完成，cancelled已取消',
  `start_date` date DEFAULT NULL COMMENT '里程碑开始日期',
  `due_date` date DEFAULT NULL COMMENT '里程碑计划截止日期',
  `completed_at` datetime DEFAULT NULL COMMENT '里程碑实际完成时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID，关联user_info表',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_project_status` (`project_id`,`status`),
  KEY `idx_created_by` (`created_by`),
  CONSTRAINT `project_milestone_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_milestone_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目里程碑表：用于管理项目阶段目标、关键节点和验收点';

DROP TABLE IF EXISTS `project_sprint`;
CREATE TABLE `project_sprint` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '迭代ID，主键',
  `project_id` bigint NOT NULL COMMENT '所属项目ID，关联project表',
  `name` varchar(255) NOT NULL COMMENT '迭代名称，例如“Sprint 1”“第2周迭代”',
  `goal` text COMMENT '迭代目标说明，用于描述本次迭代主要完成的内容',
  `status` enum('planned','active','completed','cancelled') NOT NULL DEFAULT 'planned' COMMENT '迭代状态：planned计划中，active进行中，completed已完成，cancelled已取消',
  `start_date` date DEFAULT NULL COMMENT '迭代开始日期',
  `end_date` date DEFAULT NULL COMMENT '迭代结束日期',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID，关联user_info表',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_project_status` (`project_id`,`status`),
  KEY `idx_created_by` (`created_by`),
  CONSTRAINT `project_sprint_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_sprint_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目迭代表：用于按敏捷迭代方式组织任务和开发周期';

DROP TABLE IF EXISTS `project_task_comment`;
CREATE TABLE `project_task_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务评论ID，主键',
  `task_id` bigint NOT NULL COMMENT '所属任务ID，关联project_task表',
  `author_id` bigint NOT NULL COMMENT '评论作者ID，关联user_info表',
  `parent_comment_id` bigint DEFAULT NULL COMMENT '父评论ID，支持任务评论的回复功能，顶级评论为NULL',
  `content` text NOT NULL COMMENT '评论内容',
  `status` enum('normal','hidden','deleted') NOT NULL DEFAULT 'normal' COMMENT '评论状态：normal正常，hidden隐藏，deleted删除',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '评论更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_created` (`task_id`,`created_at`),
  KEY `idx_author` (`author_id`),
  KEY `idx_parent` (`parent_comment_id`),
  CONSTRAINT `project_task_comment_ibfk_1` FOREIGN KEY (`task_id`) REFERENCES `project_task` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_task_comment_ibfk_2` FOREIGN KEY (`author_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_task_comment_ibfk_3` FOREIGN KEY (`parent_comment_id`) REFERENCES `project_task_comment` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务评论表：用于任务讨论、回复、记录处理意见';

DROP TABLE IF EXISTS `project_task_attachment`;
CREATE TABLE `project_task_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务附件ID，主键',
  `task_id` bigint NOT NULL COMMENT '所属任务ID，关联project_task表',
  `file_name` varchar(255) NOT NULL COMMENT '附件原始文件名',
  `file_path` varchar(500) NOT NULL COMMENT '附件在服务器上的存储路径',
  `file_size_bytes` bigint DEFAULT NULL COMMENT '附件大小，单位字节',
  `file_type` varchar(50) DEFAULT NULL COMMENT '附件类型，例如png、pdf、docx、zip',
  `uploaded_by` bigint DEFAULT NULL COMMENT '上传者ID，关联user_info表',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_created` (`task_id`,`created_at`),
  KEY `idx_uploaded_by` (`uploaded_by`),
  CONSTRAINT `project_task_attachment_ibfk_1` FOREIGN KEY (`task_id`) REFERENCES `project_task` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_task_attachment_ibfk_2` FOREIGN KEY (`uploaded_by`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务附件表：用于给任务上传原型图、文档、压缩包、图片等附件';

DROP TABLE IF EXISTS `project_task_checklist_item`;
CREATE TABLE `project_task_checklist_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务检查项ID，主键',
  `task_id` bigint NOT NULL COMMENT '所属任务ID，关联project_task表',
  `content` varchar(500) NOT NULL COMMENT '检查项内容，例如“完成接口联调”“补充测试用例”',
  `is_checked` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已勾选完成：0未完成，1已完成',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序值，值越小越靠前',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID，关联user_info表',
  `checked_by` bigint DEFAULT NULL COMMENT '勾选人ID，关联user_info表',
  `checked_at` datetime DEFAULT NULL COMMENT '勾选完成时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_sort` (`task_id`,`sort_order`),
  KEY `idx_created_by` (`created_by`),
  KEY `idx_checked_by` (`checked_by`),
  CONSTRAINT `project_task_checklist_item_ibfk_1` FOREIGN KEY (`task_id`) REFERENCES `project_task` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_task_checklist_item_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `user_info` (`id`) ON DELETE SET NULL,
  CONSTRAINT `project_task_checklist_item_ibfk_3` FOREIGN KEY (`checked_by`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务检查项表：用于把一个任务拆成多个可勾选的子步骤';

DROP TABLE IF EXISTS `project_task_dependency`;
CREATE TABLE `project_task_dependency` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务依赖关系ID，主键',
  `project_id` bigint NOT NULL COMMENT '所属项目ID，关联project表',
  `predecessor_task_id` bigint NOT NULL COMMENT '前置任务ID，表示必须先完成或先开始的任务',
  `successor_task_id` bigint NOT NULL COMMENT '后续任务ID，表示依赖前置任务的任务',
  `dependency_type` enum('finish_to_start','start_to_start','finish_to_finish','start_to_finish') NOT NULL DEFAULT 'finish_to_start' COMMENT '依赖类型：finish_to_start完成到开始，start_to_start开始到开始，finish_to_finish完成到完成，start_to_finish开始到完成',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '依赖关系创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_dependency` (`predecessor_task_id`,`successor_task_id`),
  KEY `idx_project` (`project_id`),
  KEY `idx_successor` (`successor_task_id`),
  CONSTRAINT `project_task_dependency_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_task_dependency_ibfk_2` FOREIGN KEY (`predecessor_task_id`) REFERENCES `project_task` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_task_dependency_ibfk_3` FOREIGN KEY (`successor_task_id`) REFERENCES `project_task` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务依赖关系表：用于描述任务之间的先后依赖和排期约束';

DROP TABLE IF EXISTS `project_task_log`;
CREATE TABLE `project_task_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务操作日志ID，主键',
  `task_id` bigint NOT NULL COMMENT '所属任务ID，关联project_task表',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID，关联user_info表',
  `action` enum('create','update','assign','change_status','change_priority','comment','attach','complete','reopen','delete') NOT NULL COMMENT '操作类型：create创建，update修改，assign指派，change_status改状态，change_priority改优先级，comment评论，attach上传附件，complete完成，reopen重新开启，delete删除',
  `field_name` varchar(100) DEFAULT NULL COMMENT '发生变化的字段名，例如status、assignee_id、priority',
  `old_value` text COMMENT '变更前的值',
  `new_value` text COMMENT '变更后的值',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '日志创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_created` (`task_id`,`created_at`),
  KEY `idx_operator` (`operator_id`),
  CONSTRAINT `project_task_log_ibfk_1` FOREIGN KEY (`task_id`) REFERENCES `project_task` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_task_log_ibfk_2` FOREIGN KEY (`operator_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务操作日志表：用于记录任务的状态流转、指派变更、优先级调整等历史';

DROP TABLE IF EXISTS `project_release`;
CREATE TABLE `project_release` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目发布版本ID，主键',
  `project_id` bigint NOT NULL COMMENT '所属项目ID，关联project表',
  `version` varchar(50) NOT NULL COMMENT '发布版本号，例如v1.0.0、2026.03.25',
  `title` varchar(255) NOT NULL COMMENT '发布标题，例如“首个公开版本发布”',
  `description` text COMMENT '发布简要说明',
  `release_notes` longtext COMMENT '发布说明全文，用于记录更新内容、修复内容、升级提示等',
  `release_type` enum('draft','beta','stable','hotfix') NOT NULL DEFAULT 'draft' COMMENT '发布类型：draft草稿，beta测试版，stable稳定版，hotfix热修复',
  `status` enum('draft','published','archived') NOT NULL DEFAULT 'draft' COMMENT '发布状态：draft草稿，published已发布，archived已归档',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID，关联user_info表',
  `published_by` bigint DEFAULT NULL COMMENT '发布人ID，关联user_info表',
  `published_at` datetime DEFAULT NULL COMMENT '正式发布时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_release_version` (`project_id`,`version`),
  KEY `idx_project_status` (`project_id`,`status`),
  KEY `idx_created_by` (`created_by`),
  KEY `idx_published_by` (`published_by`),
  CONSTRAINT `project_release_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_release_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `user_info` (`id`) ON DELETE SET NULL,
  CONSTRAINT `project_release_ibfk_3` FOREIGN KEY (`published_by`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目发布版本表：用于管理项目正式发布记录、变更说明和版本历史';

DROP TABLE IF EXISTS `project_release_file`;
CREATE TABLE `project_release_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '发布文件ID，主键',
  `release_id` bigint NOT NULL COMMENT '所属发布版本ID，关联project_release表',
  `project_file_id` bigint DEFAULT NULL COMMENT '项目主文件ID，关联project_file表，可为空以兼容独立发布文件',
  `file_version_id` bigint DEFAULT NULL COMMENT '具体文件版本ID，关联project_file_version表，用于精确指向某次版本包',
  `file_name` varchar(255) NOT NULL COMMENT '发布文件名称，用于展示或下载',
  `file_path` varchar(500) NOT NULL COMMENT '发布文件存储路径',
  `file_size_bytes` bigint DEFAULT NULL COMMENT '发布文件大小，单位字节',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序值，控制一个发布版本下多个文件的展示顺序',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_release_sort` (`release_id`,`sort_order`),
  KEY `idx_project_file` (`project_file_id`),
  KEY `idx_file_version` (`file_version_id`),
  CONSTRAINT `project_release_file_ibfk_1` FOREIGN KEY (`release_id`) REFERENCES `project_release` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_release_file_ibfk_2` FOREIGN KEY (`project_file_id`) REFERENCES `project_file` (`id`) ON DELETE SET NULL,
  CONSTRAINT `project_release_file_ibfk_3` FOREIGN KEY (`file_version_id`) REFERENCES `project_file_version` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目发布文件表：用于把某次发布版本和对应的安装包、压缩包、补丁包关联起来';

DROP TABLE IF EXISTS `project_doc`;
CREATE TABLE `project_doc` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目文档ID，主键',
  `project_id` bigint NOT NULL COMMENT '所属项目ID，关联project表',
  `title` varchar(255) NOT NULL COMMENT '文档标题',
  `doc_type` enum('wiki','spec','meeting_note','design','manual','other') NOT NULL DEFAULT 'wiki' COMMENT '文档类型：wiki知识库，spec需求/规格，meeting_note会议纪要，design设计文档，manual使用手册，other其他',
  `status` enum('draft','published','archived') NOT NULL DEFAULT 'draft' COMMENT '文档状态：draft草稿，published已发布，archived已归档',
  `visibility` enum('project','team','private') NOT NULL DEFAULT 'project' COMMENT '文档可见范围：project项目内可见，team团队可见，private仅自己可见',
  `current_content` longtext COMMENT '当前版本文档内容，便于快速读取最新内容',
  `current_version` int NOT NULL DEFAULT '1' COMMENT '当前版本号，从1开始递增',
  `creator_id` bigint DEFAULT NULL COMMENT '创建人ID，关联user_info表',
  `editor_id` bigint DEFAULT NULL COMMENT '最后编辑人ID，关联user_info表',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_project_status` (`project_id`,`status`),
  KEY `idx_creator` (`creator_id`),
  KEY `idx_editor` (`editor_id`),
  CONSTRAINT `project_doc_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_doc_ibfk_2` FOREIGN KEY (`creator_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL,
  CONSTRAINT `project_doc_ibfk_3` FOREIGN KEY (`editor_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目文档表：用于存储项目内的说明文档、设计文档、会议纪要、使用手册等';

DROP TABLE IF EXISTS `project_doc_version`;
CREATE TABLE `project_doc_version` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文档版本ID，主键',
  `doc_id` bigint NOT NULL COMMENT '所属文档ID，关联project_doc表',
  `version_no` int NOT NULL COMMENT '版本号，从1开始递增，用于标识文档历史版本',
  `content_snapshot` longtext NOT NULL COMMENT '该版本的完整文档内容快照',
  `change_summary` varchar(500) DEFAULT NULL COMMENT '本次变更摘要，例如“补充部署步骤”“修复接口说明”',
  `edited_by` bigint DEFAULT NULL COMMENT '编辑人ID，关联user_info表',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '版本创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doc_version_no` (`doc_id`,`version_no`),
  KEY `idx_edited_by` (`edited_by`),
  CONSTRAINT `project_doc_version_ibfk_1` FOREIGN KEY (`doc_id`) REFERENCES `project_doc` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_doc_version_ibfk_2` FOREIGN KEY (`edited_by`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目文档版本表：用于保存项目文档每一次修改后的历史快照';

DROP TABLE IF EXISTS `project_integration`;
CREATE TABLE `project_integration` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工具集成ID，主键',
  `project_id` bigint NOT NULL COMMENT '所属项目ID，关联project表',
  `tool_type` enum('github','gitlab','gitee','feishu_doc','yuque','notion','jira','trello','webhook','other') NOT NULL COMMENT '工具类型，例如GitHub、飞书文档、语雀、Notion、Jira等',
  `name` varchar(100) NOT NULL COMMENT '集成名称，便于前端展示，例如“主仓库”“需求文档”“测试看板”',
  `access_mode` enum('link','api','webhook') NOT NULL DEFAULT 'link' COMMENT '接入方式：link仅保存外链，api通过接口访问，webhook通过回调推送',
  `base_url` varchar(500) DEFAULT NULL COMMENT '外部工具基础地址或链接地址',
  `external_project_id` varchar(255) DEFAULT NULL COMMENT '外部平台中的项目ID、仓库ID或文档ID',
  `access_token_encrypted` varchar(1000) DEFAULT NULL COMMENT '加密后的访问令牌，供后续接口调用使用',
  `config_json` json DEFAULT NULL COMMENT '附加配置，JSON格式，例如分支名、空间名、回调密钥等',
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用：1启用，0停用',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID，关联user_info表',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_project_tool` (`project_id`,`tool_type`),
  KEY `idx_created_by` (`created_by`),
  CONSTRAINT `project_integration_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_integration_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目工具集成表：用于配置项目与Git仓库、文档平台、任务平台、Webhook等外部工具的绑定关系';

DROP TABLE IF EXISTS `project_integration_event`;
CREATE TABLE `project_integration_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '集成事件ID，主键',
  `integration_id` bigint NOT NULL COMMENT '所属工具集成ID，关联project_integration表',
  `event_type` varchar(100) NOT NULL COMMENT '事件类型，例如push、merge_request、doc_updated、task_synced',
  `external_event_id` varchar(255) DEFAULT NULL COMMENT '外部平台事件唯一ID，用于幂等去重',
  `payload` json DEFAULT NULL COMMENT '事件原始数据，JSON格式，保存Webhook或API返回的完整内容',
  `status` enum('pending','processed','failed','ignored') NOT NULL DEFAULT 'pending' COMMENT '处理状态：pending待处理，processed已处理，failed处理失败，ignored忽略',
  `result_message` varchar(500) DEFAULT NULL COMMENT '处理结果说明或错误信息',
  `occurred_at` datetime DEFAULT NULL COMMENT '外部事件发生时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件入库时间',
  PRIMARY KEY (`id`),
  KEY `idx_integration_status` (`integration_id`,`status`),
  KEY `idx_external_event_id` (`external_event_id`),
  CONSTRAINT `project_integration_event_ibfk_1` FOREIGN KEY (`integration_id`) REFERENCES `project_integration` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目集成事件表：用于记录外部工具回调事件、同步任务和处理结果';

DROP TABLE IF EXISTS `project_stat_daily`;
CREATE TABLE `project_stat_daily` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目日统计ID，主键',
  `project_id` bigint NOT NULL COMMENT '所属项目ID，关联project表',
  `stat_date` date NOT NULL COMMENT '统计日期，按天聚合',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '当日浏览次数',
  `unique_visitor_count` int NOT NULL DEFAULT '0' COMMENT '当日独立访客数',
  `download_count` int NOT NULL DEFAULT '0' COMMENT '当日下载次数',
  `unique_download_user_count` int NOT NULL DEFAULT '0' COMMENT '当日独立下载用户数',
  `star_count` int NOT NULL DEFAULT '0' COMMENT '当日新增星标/点赞数',
  `comment_count` int NOT NULL DEFAULT '0' COMMENT '当日新增评论数',
  `member_active_count` int NOT NULL DEFAULT '0' COMMENT '当日活跃成员数',
  `new_member_count` int NOT NULL DEFAULT '0' COMMENT '当日新增成员数',
  `task_created_count` int NOT NULL DEFAULT '0' COMMENT '当日新增任务数',
  `task_completed_count` int NOT NULL DEFAULT '0' COMMENT '当日完成任务数',
  `revenue_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '当日产生的收益金额',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '统计记录创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '统计记录更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_stat_date` (`project_id`,`stat_date`),
  CONSTRAINT `project_stat_daily_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目日统计表：按天汇总浏览、下载、评论、成员活跃、任务完成、收益等数据';

DROP TABLE IF EXISTS `project_star_record`;
CREATE TABLE `project_star_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目星标记录ID，主键',
  `project_id` bigint NOT NULL COMMENT '被星标的项目ID，关联project表',
  `user_id` bigint NOT NULL COMMENT '执行星标操作的用户ID，关联user_info表',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '星标时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_user_star` (`project_id`,`user_id`),
  KEY `idx_user` (`user_id`),
  CONSTRAINT `project_star_record_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_star_record_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目星标记录表：用于记录用户对项目的点赞/星标原始行为，便于统计和防重';

DROP TABLE IF EXISTS `project_download_record`;
CREATE TABLE `project_download_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目下载记录ID，主键',
  `project_id` bigint NOT NULL COMMENT '所属项目ID，关联project表',
  `file_id` bigint DEFAULT NULL COMMENT '下载的项目文件ID，关联project_file表',
  `file_version_id` bigint DEFAULT NULL COMMENT '下载的具体文件版本ID，关联project_file_version表',
  `user_id` bigint DEFAULT NULL COMMENT '下载用户ID，关联user_info表，匿名下载则可为空',
  `ip_address` varchar(45) DEFAULT NULL COMMENT '下载者IP地址',
  `user_agent` text COMMENT '下载者User-Agent信息，用于浏览器或客户端识别',
  `downloaded_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下载时间',
  PRIMARY KEY (`id`),
  KEY `idx_project_downloaded` (`project_id`,`downloaded_at` DESC),
  KEY `idx_file_id` (`file_id`),
  KEY `idx_file_version_id` (`file_version_id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `project_download_record_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_download_record_ibfk_2` FOREIGN KEY (`file_id`) REFERENCES `project_file` (`id`) ON DELETE SET NULL,
  CONSTRAINT `project_download_record_ibfk_3` FOREIGN KEY (`file_version_id`) REFERENCES `project_file_version` (`id`) ON DELETE SET NULL,
  CONSTRAINT `project_download_record_ibfk_4` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目下载记录表：用于记录项目文件下载明细，支持下载统计、风控和审计';

DROP TABLE IF EXISTS `content_review_record`;
CREATE TABLE `content_review_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '内容审核记录ID，主键',
  `content_type` enum('blog','project','paid_content') NOT NULL COMMENT '审核内容类型：blog博客，project项目，paid_content付费内容',
  `content_id` bigint NOT NULL COMMENT '被审核内容的逻辑ID，对应具体内容表主键',
  `submitter_id` bigint DEFAULT NULL COMMENT '提交审核的用户ID，关联user_info表',
  `reviewer_id` bigint DEFAULT NULL COMMENT '审核人ID，关联user_info表，通常为管理员或审核员',
  `review_round` int NOT NULL DEFAULT '1' COMMENT '审核轮次，第几次提交审核',
  `review_status` enum('pending','approved','rejected','recalled') NOT NULL DEFAULT 'pending' COMMENT '审核状态：pending待审核，approved通过，rejected驳回，recalled撤回',
  `review_comment` text COMMENT '审核意见，例如驳回理由、整改建议',
  `snapshot_title` varchar(255) DEFAULT NULL COMMENT '审核时内容标题快照，避免后续标题变化导致追溯困难',
  `snapshot_data` json DEFAULT NULL COMMENT '审核时内容关键信息快照，JSON格式，便于后续审计追踪',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交审核时间',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审核完成时间',
  PRIMARY KEY (`id`),
  KEY `idx_content_status` (`content_type`,`content_id`,`review_status`),
  KEY `idx_submitter` (`submitter_id`),
  KEY `idx_reviewer` (`reviewer_id`),
  CONSTRAINT `content_review_record_ibfk_1` FOREIGN KEY (`submitter_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL,
  CONSTRAINT `content_review_record_ibfk_2` FOREIGN KEY (`reviewer_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内容审核记录表：用于记录博客、项目、付费内容的审核过程、结果与快照';


DROP TABLE IF EXISTS `coupon_redemption`;
DROP TABLE IF EXISTS `user_coupon`;
DROP TABLE IF EXISTS `ai_retrieval_log`;
DROP TABLE IF EXISTS `ai_call_log`;
DROP TABLE IF EXISTS `project_ai_assistant_kb`;
DROP TABLE IF EXISTS `knowledge_chunk`;
DROP TABLE IF EXISTS `knowledge_document`;
DROP TABLE IF EXISTS `project_ai_assistant`;
DROP TABLE IF EXISTS `ai_prompt_template`;
DROP TABLE IF EXISTS `knowledge_base`;

CREATE TABLE `knowledge_base` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '知识库ID，主键',
  `name` varchar(100) NOT NULL COMMENT '知识库名称',
  `description` text COMMENT '知识库描述，说明知识库用途和收录范围',
  `scope_type` enum('personal','project','platform') NOT NULL DEFAULT 'personal' COMMENT '知识库作用域：personal个人知识库，project项目知识库，platform平台公共知识库',
  `project_id` bigint DEFAULT NULL COMMENT '关联项目ID，当scope_type=project时使用，关联project表',
  `owner_id` bigint DEFAULT NULL COMMENT '知识库拥有者ID，通常为创建人，关联user_info表',
  `source_type` enum('manual','upload','project_file','project_doc','blog','mixed') NOT NULL DEFAULT 'mixed' COMMENT '知识来源类型：manual手工录入，upload上传文件，project_file项目文件，project_doc项目文档，blog博客，mixed混合来源',
  `embedding_provider` varchar(50) DEFAULT NULL COMMENT '向量化提供方，如openai、qwen、deepseek、本地模型等',
  `embedding_model` varchar(100) DEFAULT NULL COMMENT '向量化模型名称',
  `chunk_strategy` enum('fixed','paragraph','markdown','custom') NOT NULL DEFAULT 'paragraph' COMMENT '切片策略：fixed固定长度，paragraph按段落，markdown按标题层级，custom自定义',
  `default_top_k` int NOT NULL DEFAULT '5' COMMENT '默认检索返回片段数量',
  `visibility` enum('private','team','public') NOT NULL DEFAULT 'private' COMMENT '知识库可见性：private仅自己，team项目团队可见，public全站可见',
  `status` enum('draft','indexing','active','disabled') NOT NULL DEFAULT 'draft' COMMENT '知识库状态：draft草稿，indexing索引中，active可用，disabled停用',
  `doc_count` int NOT NULL DEFAULT '0' COMMENT '知识库内文档数量冗余统计',
  `chunk_count` int NOT NULL DEFAULT '0' COMMENT '知识库内切片数量冗余统计',
  `last_indexed_at` datetime DEFAULT NULL COMMENT '最后一次完成索引时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_scope_status` (`scope_type`,`status`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_owner_id` (`owner_id`),
  CONSTRAINT `knowledge_base_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `knowledge_base_ibfk_2` FOREIGN KEY (`owner_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库主表：用于定义个人、项目或平台级知识库，是知识库AI和项目AI助手的核心容器';

CREATE TABLE `ai_prompt_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '提示词模板ID，主键',
  `name` varchar(100) NOT NULL COMMENT '模板名称',
  `template_type` enum('general_chat','knowledge_qa','project_assistant','summary','code_explain','custom') NOT NULL DEFAULT 'custom' COMMENT '模板类型：general_chat普通聊天，knowledge_qa知识问答，project_assistant项目助手，summary摘要总结，code_explain代码解释，custom自定义',
  `scope_type` enum('platform','project','personal') NOT NULL DEFAULT 'platform' COMMENT '模板作用域：platform平台级，project项目级，personal个人级',
  `project_id` bigint DEFAULT NULL COMMENT '关联项目ID，当scope_type=project时使用，关联project表',
  `owner_id` bigint DEFAULT NULL COMMENT '模板创建者ID，关联user_info表',
  `default_model_id` bigint DEFAULT NULL COMMENT '默认使用的AI模型ID，关联ai_model表',
  `system_prompt` longtext NOT NULL COMMENT '系统提示词内容，用于定义AI角色、语气、规则和边界',
  `user_prompt_template` longtext COMMENT '用户提示词模板，支持预留变量占位符',
  `variables_schema` json DEFAULT NULL COMMENT '模板变量定义，JSON格式，如变量名、说明、是否必填',
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用：1启用，0停用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_scope_type` (`scope_type`,`template_type`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_default_model_id` (`default_model_id`),
  CONSTRAINT `ai_prompt_template_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `ai_prompt_template_ibfk_2` FOREIGN KEY (`owner_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL,
  CONSTRAINT `ai_prompt_template_ibfk_3` FOREIGN KEY (`default_model_id`) REFERENCES `ai_model` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI提示词模板表：用于配置平台、项目或个人级的系统提示词和变量模板';

CREATE TABLE `project_ai_assistant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目AI助手ID，主键',
  `project_id` bigint NOT NULL COMMENT '所属项目ID，关联project表',
  `name` varchar(100) NOT NULL COMMENT '项目AI助手名称，如项目助理、开发问答助手',
  `description` text COMMENT '项目AI助手说明',
  `assistant_type` enum('general','knowledge_qa','project_manager','code_helper','doc_helper','custom') NOT NULL DEFAULT 'general' COMMENT '助手类型：general通用，knowledge_qa知识问答，project_manager项目管理，code_helper代码助手，doc_helper文档助手，custom自定义',
  `ai_model_id` bigint DEFAULT NULL COMMENT '默认AI模型ID，关联ai_model表',
  `prompt_template_id` bigint DEFAULT NULL COMMENT '默认提示词模板ID，关联ai_prompt_template表',
  `system_prompt_override` longtext COMMENT '项目级系统提示词覆盖内容，优先级高于模板默认值',
  `temperature` decimal(4,2) DEFAULT NULL COMMENT '温度参数，控制回复随机性',
  `max_tokens` int DEFAULT NULL COMMENT '单次回复最大Token数',
  `top_k` int NOT NULL DEFAULT '5' COMMENT '默认知识检索返回片段数量',
  `access_scope` enum('owner_admin','project_member','public') NOT NULL DEFAULT 'project_member' COMMENT '可访问范围：owner_admin仅项目所有者和管理员，project_member项目成员，public公开',
  `conversation_mode` enum('single_turn','multi_turn') NOT NULL DEFAULT 'multi_turn' COMMENT '会话模式：single_turn单轮，multi_turn多轮',
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用：1启用，0停用',
  `created_by` bigint DEFAULT NULL COMMENT '创建者ID，关联user_info表',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_model_id` (`ai_model_id`),
  KEY `idx_prompt_template_id` (`prompt_template_id`),
  KEY `idx_created_by` (`created_by`),
  CONSTRAINT `project_ai_assistant_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_ai_assistant_ibfk_2` FOREIGN KEY (`ai_model_id`) REFERENCES `ai_model` (`id`) ON DELETE SET NULL,
  CONSTRAINT `project_ai_assistant_ibfk_3` FOREIGN KEY (`prompt_template_id`) REFERENCES `ai_prompt_template` (`id`) ON DELETE SET NULL,
  CONSTRAINT `project_ai_assistant_ibfk_4` FOREIGN KEY (`created_by`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目AI助手配置表：用于为具体项目配置AI模型、提示词、检索参数和访问权限';

CREATE TABLE `knowledge_document` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '知识文档ID，主键',
  `knowledge_base_id` bigint NOT NULL COMMENT '所属知识库ID，关联knowledge_base表',
  `title` varchar(255) NOT NULL COMMENT '文档标题',
  `source_type` enum('upload','project_file','project_doc','blog','manual','url','other') NOT NULL DEFAULT 'manual' COMMENT '文档来源类型：upload上传，project_file项目文件，project_doc项目文档，blog博客，manual手工录入，url网页链接，other其他',
  `source_ref_id` bigint DEFAULT NULL COMMENT '来源业务记录ID，如project_file.id、project_doc.id、blog.id等',
  `source_url` varchar(500) DEFAULT NULL COMMENT '来源URL，如网页抓取地址或外部文档地址',
  `file_name` varchar(255) DEFAULT NULL COMMENT '原始文件名',
  `mime_type` varchar(100) DEFAULT NULL COMMENT '文件MIME类型',
  `storage_path` varchar(500) DEFAULT NULL COMMENT '原文件存储路径或对象存储地址',
  `content_text` longtext COMMENT '解析后的纯文本内容，用于切片与检索',
  `content_hash` varchar(64) DEFAULT NULL COMMENT '文档内容哈希值，用于去重和变更检测',
  `language` varchar(20) DEFAULT NULL COMMENT '文档语言，如zh-CN、en-US',
  `status` enum('uploaded','parsing','indexed','failed','disabled') NOT NULL DEFAULT 'uploaded' COMMENT '文档处理状态：uploaded已上传，parsing解析中，indexed已建索引，failed失败，disabled停用',
  `error_message` varchar(500) DEFAULT NULL COMMENT '解析或索引失败原因',
  `uploaded_by` bigint DEFAULT NULL COMMENT '上传人ID，关联user_info表',
  `indexed_at` datetime DEFAULT NULL COMMENT '完成索引时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_knowledge_base_status` (`knowledge_base_id`,`status`),
  KEY `idx_source_type_ref` (`source_type`,`source_ref_id`),
  KEY `idx_uploaded_by` (`uploaded_by`),
  KEY `idx_content_hash` (`content_hash`),
  CONSTRAINT `knowledge_document_ibfk_1` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE CASCADE,
  CONSTRAINT `knowledge_document_ibfk_2` FOREIGN KEY (`uploaded_by`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识文档表：存储知识库中的原始文档元数据、来源信息和解析后的文本内容';

CREATE TABLE `knowledge_chunk` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '知识切片ID，主键',
  `knowledge_base_id` bigint NOT NULL COMMENT '所属知识库ID，关联knowledge_base表',
  `document_id` bigint NOT NULL COMMENT '所属知识文档ID，关联knowledge_document表',
  `chunk_index` int NOT NULL COMMENT '在文档中的切片序号，从0或1开始递增',
  `title` varchar(255) DEFAULT NULL COMMENT '切片标题，通常取段落标题或章节标题',
  `content` longtext NOT NULL COMMENT '切片正文内容，用于向量化和召回',
  `token_count` int DEFAULT NULL COMMENT '切片Token数量',
  `char_count` int DEFAULT NULL COMMENT '切片字符数',
  `embedding_provider` varchar(50) DEFAULT NULL COMMENT '该切片使用的向量化提供方',
  `embedding_model` varchar(100) DEFAULT NULL COMMENT '该切片使用的向量化模型名称',
  `vector_id` varchar(255) DEFAULT NULL COMMENT '外部向量库中的向量ID，如Milvus、PGVector、ES等',
  `metadata` json DEFAULT NULL COMMENT '切片附加元数据，JSON格式，如页码、标题层级、标签等',
  `status` enum('active','disabled') NOT NULL DEFAULT 'active' COMMENT '切片状态：active可用，disabled停用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_document_chunk_index` (`document_id`,`chunk_index`),
  KEY `idx_knowledge_base_id` (`knowledge_base_id`),
  KEY `idx_vector_id` (`vector_id`),
  CONSTRAINT `knowledge_chunk_ibfk_1` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE CASCADE,
  CONSTRAINT `knowledge_chunk_ibfk_2` FOREIGN KEY (`document_id`) REFERENCES `knowledge_document` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识切片表：存储文档切片及其向量化关联信息，是知识库检索与RAG问答的核心数据表';

CREATE TABLE `project_ai_assistant_kb` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目AI助手与知识库关联ID，主键',
  `assistant_id` bigint NOT NULL COMMENT '项目AI助手ID，关联project_ai_assistant表',
  `knowledge_base_id` bigint NOT NULL COMMENT '知识库ID，关联knowledge_base表',
  `priority` int NOT NULL DEFAULT '0' COMMENT '优先级，越大越优先参与检索',
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用该知识库关联：1启用，0停用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_assistant_kb` (`assistant_id`,`knowledge_base_id`),
  KEY `idx_kb_id` (`knowledge_base_id`),
  CONSTRAINT `project_ai_assistant_kb_ibfk_1` FOREIGN KEY (`assistant_id`) REFERENCES `project_ai_assistant` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_ai_assistant_kb_ibfk_2` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目AI助手知识库关联表：用于指定某个项目AI助手可以检索哪些知识库';

CREATE TABLE `ai_call_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'AI调用日志ID，主键',
  `user_id` bigint DEFAULT NULL COMMENT '发起调用的用户ID，关联user_info表',
  `project_id` bigint DEFAULT NULL COMMENT '所属项目ID，关联project表，用于项目场景统计',
  `assistant_id` bigint DEFAULT NULL COMMENT '项目AI助手ID，关联project_ai_assistant表',
  `conversation_id` bigint DEFAULT NULL COMMENT '会话ID，关联conversation表',
  `message_id` bigint DEFAULT NULL COMMENT '触发本次调用的消息ID，关联message表',
  `prompt_template_id` bigint DEFAULT NULL COMMENT '使用的提示词模板ID，关联ai_prompt_template表',
  `ai_model_id` bigint DEFAULT NULL COMMENT '本次调用使用的AI模型ID，关联ai_model表',
  `request_type` enum('chat','knowledge_qa','summary','rewrite','project_assistant','other') NOT NULL DEFAULT 'chat' COMMENT '调用类型：chat聊天，knowledge_qa知识问答，summary摘要，rewrite改写，project_assistant项目助手，other其他',
  `request_text` longtext COMMENT '用户原始请求文本',
  `response_text` longtext COMMENT '模型响应文本，可按需要裁剪存储',
  `request_params` json DEFAULT NULL COMMENT '本次调用的请求参数，JSON格式，如temperature、top_p等',
  `prompt_tokens` int DEFAULT NULL COMMENT '提示词Token数',
  `completion_tokens` int DEFAULT NULL COMMENT '回复Token数',
  `total_tokens` int DEFAULT NULL COMMENT '总Token数',
  `cost_amount` decimal(10,4) DEFAULT NULL COMMENT '本次调用的预估或实际成本金额',
  `latency_ms` int DEFAULT NULL COMMENT '调用耗时，单位毫秒',
  `status` enum('success','failed','timeout','cancelled') NOT NULL DEFAULT 'success' COMMENT '调用状态：success成功，failed失败，timeout超时，cancelled取消',
  `error_code` varchar(100) DEFAULT NULL COMMENT '失败时的错误码',
  `error_message` varchar(500) DEFAULT NULL COMMENT '失败时的错误信息',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '日志创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_created` (`user_id`,`created_at` DESC),
  KEY `idx_project_created` (`project_id`,`created_at` DESC),
  KEY `idx_assistant_created` (`assistant_id`,`created_at` DESC),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_message_id` (`message_id`),
  KEY `idx_model_id` (`ai_model_id`),
  CONSTRAINT `ai_call_log_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL,
  CONSTRAINT `ai_call_log_ibfk_2` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE SET NULL,
  CONSTRAINT `ai_call_log_ibfk_3` FOREIGN KEY (`assistant_id`) REFERENCES `project_ai_assistant` (`id`) ON DELETE SET NULL,
  CONSTRAINT `ai_call_log_ibfk_4` FOREIGN KEY (`conversation_id`) REFERENCES `conversation` (`id`) ON DELETE SET NULL,
  CONSTRAINT `ai_call_log_ibfk_5` FOREIGN KEY (`message_id`) REFERENCES `message` (`id`) ON DELETE SET NULL,
  CONSTRAINT `ai_call_log_ibfk_6` FOREIGN KEY (`prompt_template_id`) REFERENCES `ai_prompt_template` (`id`) ON DELETE SET NULL,
  CONSTRAINT `ai_call_log_ibfk_7` FOREIGN KEY (`ai_model_id`) REFERENCES `ai_model` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI调用日志表：记录AI请求、响应、模型、Token消耗、成本和错误信息，便于审计与统计';

CREATE TABLE `ai_retrieval_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'AI检索日志ID，主键',
  `call_log_id` bigint NOT NULL COMMENT '所属AI调用日志ID，关联ai_call_log表',
  `knowledge_base_id` bigint DEFAULT NULL COMMENT '命中的知识库ID，关联knowledge_base表',
  `document_id` bigint DEFAULT NULL COMMENT '命中的知识文档ID，关联knowledge_document表',
  `chunk_id` bigint DEFAULT NULL COMMENT '命中的知识切片ID，关联knowledge_chunk表',
  `query_text` text COMMENT '检索查询文本，通常是用户问题或改写后的检索query',
  `score` decimal(10,6) DEFAULT NULL COMMENT '召回相似度得分或排序分值',
  `rank_no` int DEFAULT NULL COMMENT '在本次检索结果中的排序名次',
  `retrieval_method` enum('vector','keyword','hybrid','manual') NOT NULL DEFAULT 'vector' COMMENT '检索方式：vector向量检索，keyword关键词检索，hybrid混合检索，manual手工指定',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '日志创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_call_log_id` (`call_log_id`),
  KEY `idx_knowledge_base_id` (`knowledge_base_id`),
  KEY `idx_document_id` (`document_id`),
  KEY `idx_chunk_id` (`chunk_id`),
  CONSTRAINT `ai_retrieval_log_ibfk_1` FOREIGN KEY (`call_log_id`) REFERENCES `ai_call_log` (`id`) ON DELETE CASCADE,
  CONSTRAINT `ai_retrieval_log_ibfk_2` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE SET NULL,
  CONSTRAINT `ai_retrieval_log_ibfk_3` FOREIGN KEY (`document_id`) REFERENCES `knowledge_document` (`id`) ON DELETE SET NULL,
  CONSTRAINT `ai_retrieval_log_ibfk_4` FOREIGN KEY (`chunk_id`) REFERENCES `knowledge_chunk` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI检索日志表：记录一次AI问答中实际召回的知识库、文档和切片结果，便于调优RAG效果';

CREATE TABLE `user_coupon` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户优惠券记录ID，主键',
  `coupon_id` bigint NOT NULL COMMENT '优惠券ID，关联coupon表',
  `user_id` bigint NOT NULL COMMENT '领取用户ID，关联user_info表',
  `source_type` enum('manual','system','campaign','exchange','register','membership') NOT NULL DEFAULT 'manual' COMMENT '领取来源：manual后台发放，system系统发放，campaign活动领取，exchange兑换码，register注册赠送，membership会员赠送',
  `receive_status` enum('received','locked','used','expired','void') NOT NULL DEFAULT 'received' COMMENT '领取状态：received已领取，locked锁定中，used已使用，expired已过期，void作废',
  `received_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  `start_time` datetime DEFAULT NULL COMMENT '该用户券实际生效时间，若为空可沿用coupon.start_time',
  `end_time` datetime DEFAULT NULL COMMENT '该用户券实际失效时间，若为空可沿用coupon.end_time',
  `used_at` datetime DEFAULT NULL COMMENT '使用时间',
  `order_id` bigint DEFAULT NULL COMMENT '最终使用到的订单ID，关联order表',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注信息，如发放原因、补偿说明等',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_coupon_user_status` (`coupon_id`,`user_id`,`receive_status`),
  KEY `idx_user_status` (`user_id`,`receive_status`),
  KEY `idx_order_id` (`order_id`),
  CONSTRAINT `user_coupon_ibfk_1` FOREIGN KEY (`coupon_id`) REFERENCES `coupon` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_coupon_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_coupon_ibfk_3` FOREIGN KEY (`order_id`) REFERENCES `order` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户优惠券领取表：记录用户实际领到的优惠券实例，是优惠券领取、状态流转和过期控制的核心表';

CREATE TABLE `coupon_redemption` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '优惠券核销记录ID，主键',
  `user_coupon_id` bigint NOT NULL COMMENT '用户优惠券实例ID，关联user_coupon表',
  `coupon_id` bigint NOT NULL COMMENT '优惠券ID，冗余保存，关联coupon表',
  `user_id` bigint NOT NULL COMMENT '使用优惠券的用户ID，关联user_info表',
  `order_id` bigint NOT NULL COMMENT '被抵扣的订单ID，关联order表',
  `original_amount` decimal(10,2) NOT NULL COMMENT '订单原始金额，使用优惠券前金额',
  `discount_amount` decimal(10,2) NOT NULL COMMENT '优惠抵扣金额',
  `final_amount` decimal(10,2) NOT NULL COMMENT '优惠后实付金额',
  `status` enum('locked','success','cancelled','rollback') NOT NULL DEFAULT 'success' COMMENT '核销状态：locked锁定待支付，success核销成功，cancelled取消，rollback回滚恢复',
  `redeemed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '核销时间',
  `rollback_at` datetime DEFAULT NULL COMMENT '回滚时间，订单关闭或退款时可用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注信息，如回滚原因、异常说明等',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_coupon_order` (`user_coupon_id`,`order_id`),
  KEY `idx_coupon_user` (`coupon_id`,`user_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `coupon_redemption_ibfk_1` FOREIGN KEY (`user_coupon_id`) REFERENCES `user_coupon` (`id`) ON DELETE CASCADE,
  CONSTRAINT `coupon_redemption_ibfk_2` FOREIGN KEY (`coupon_id`) REFERENCES `coupon` (`id`) ON DELETE CASCADE,
  CONSTRAINT `coupon_redemption_ibfk_3` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `coupon_redemption_ibfk_4` FOREIGN KEY (`order_id`) REFERENCES `order` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券核销记录表：记录优惠券在订单中的使用、抵扣金额和回滚信息，用于审计与统计';


SET FOREIGN_KEY_CHECKS = 1;
