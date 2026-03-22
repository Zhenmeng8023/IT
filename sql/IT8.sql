-- MySQL dump 10.13  Distrib 8.0.39, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: it_data
-- ------------------------------------------------------
-- Server version	8.0.39

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `audit_log`
--

DROP TABLE IF EXISTS `audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_log`
--

LOCK TABLES `audit_log` WRITE;
/*!40000 ALTER TABLE `audit_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `audit_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `blog`
--

DROP TABLE IF EXISTS `blog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blog` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '博客ID，主键',
  `title` varchar(200) NOT NULL COMMENT '博客标题',
  `content` longtext NOT NULL COMMENT '博客正文内容',
  `cover_image_url` varchar(500) DEFAULT NULL COMMENT '封面图片URL地址',
  `tags` json DEFAULT NULL COMMENT '博客关联的标签ID列表，以JSON数组形式存储，例如 [1, 2, 5]',
  `author_id` bigint NOT NULL COMMENT '作者ID，关联user_info表',
  `project_id` bigint DEFAULT NULL COMMENT '关联的项目ID，如果博客是介绍某个项目，则关联此处',
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
  KEY `project_id` (`project_id`),
  KEY `idx_author_status` (`author_id`,`status`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_is_marked` (`is_marked`),
  CONSTRAINT `blog_ibfk_1` FOREIGN KEY (`author_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `blog_ibfk_2` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='博客内容表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `blog`
--

LOCK TABLES `blog` WRITE;
/*!40000 ALTER TABLE `blog` DISABLE KEYS */;
/*!40000 ALTER TABLE `blog` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `circle`
--

DROP TABLE IF EXISTS `circle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `circle`
--

LOCK TABLES `circle` WRITE;
/*!40000 ALTER TABLE `circle` DISABLE KEYS */;
/*!40000 ALTER TABLE `circle` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `circle_comment`
--

DROP TABLE IF EXISTS `circle_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `circle_comment`
--

LOCK TABLES `circle_comment` WRITE;
/*!40000 ALTER TABLE `circle_comment` DISABLE KEYS */;
/*!40000 ALTER TABLE `circle_comment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `circle_member`
--

DROP TABLE IF EXISTS `circle_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `circle_member`
--

LOCK TABLES `circle_member` WRITE;
/*!40000 ALTER TABLE `circle_member` DISABLE KEYS */;
/*!40000 ALTER TABLE `circle_member` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `collect_record`
--

DROP TABLE IF EXISTS `collect_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `collect_record`
--

LOCK TABLES `collect_record` WRITE;
/*!40000 ALTER TABLE `collect_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `collect_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `comment`
--

DROP TABLE IF EXISTS `comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID，主键',
  `content` text NOT NULL COMMENT '评论正文内容',
  `parent_comment_id` bigint DEFAULT NULL COMMENT '父级评论ID，用于实现多级评论回复，顶级评论为NULL',
  `post_id` bigint NOT NULL COMMENT '被评论内容的ID',
  `author_id` bigint NOT NULL COMMENT '评论者ID，关联user_info表',
  `likes` int DEFAULT '0' COMMENT '该条评论的点赞数',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论创建时间',
  `status` enum('normal','hidden','deleted') DEFAULT 'normal' COMMENT '评论状态：normal-正常显示, hidden-隐藏, deleted-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_post_type_post_id` (`post_id`) COMMENT '按ID快速查找评论',
  KEY `idx_author` (`author_id`),
  KEY `idx_parent` (`parent_comment_id`),
  CONSTRAINT `comment_ibfk_1` FOREIGN KEY (`author_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `comment_ibfk_2` FOREIGN KEY (`parent_comment_id`) REFERENCES `comment` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评论表，支持对博客和圈子进行评论和回复';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `comment`
--

LOCK TABLES `comment` WRITE;
/*!40000 ALTER TABLE `comment` DISABLE KEYS */;
/*!40000 ALTER TABLE `comment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `conversation`
--

DROP TABLE IF EXISTS `conversation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID，主键',
  `type` enum('private','group') NOT NULL COMMENT '会话类型：private-私信, group-群聊',
  `name` varchar(100) DEFAULT NULL COMMENT '会话名称（群聊时使用）',
  `creator_id` bigint DEFAULT NULL COMMENT '创建者ID（群聊时使用）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '会话创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '会话最后更新时间（最后一条消息时间）',
  PRIMARY KEY (`id`),
  KEY `creator_id` (`creator_id`),
  CONSTRAINT `conversation_ibfk_1` FOREIGN KEY (`creator_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='私信/群聊会话表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `conversation`
--

LOCK TABLES `conversation` WRITE;
/*!40000 ALTER TABLE `conversation` DISABLE KEYS */;
/*!40000 ALTER TABLE `conversation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `follow`
--

DROP TABLE IF EXISTS `follow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `follow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关注关系ID，主键',
  `follower_id` bigint NOT NULL COMMENT '关注者ID，关联user_info表',
  `followee_id` bigint NOT NULL COMMENT '被关注者ID，关联user_info表',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注建立时间',
  `status` enum('active','blocked') DEFAULT 'active' COMMENT '关注状态：active-正常关注, blocked-已拉黑',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follower_followee` (`follower_id`,`followee_id`) COMMENT '确保关注关系唯一',
  KEY `idx_followee` (`followee_id`),
  CONSTRAINT `follow_ibfk_1` FOREIGN KEY (`follower_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `follow_ibfk_2` FOREIGN KEY (`followee_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户关注关系表（单向）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `follow`
--

LOCK TABLES `follow` WRITE;
/*!40000 ALTER TABLE `follow` DISABLE KEYS */;
/*!40000 ALTER TABLE `follow` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `friendship`
--

DROP TABLE IF EXISTS `friendship`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `friendship`
--

LOCK TABLES `friendship` WRITE;
/*!40000 ALTER TABLE `friendship` DISABLE KEYS */;
/*!40000 ALTER TABLE `friendship` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `like_record`
--

DROP TABLE IF EXISTS `like_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `like_record`
--

LOCK TABLES `like_record` WRITE;
/*!40000 ALTER TABLE `like_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `like_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `menu`
--

DROP TABLE IF EXISTS `menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=121 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='前端菜单及权限配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `menu`
--

LOCK TABLES `menu` WRITE;
/*!40000 ALTER TABLE `menu` DISABLE KEYS */;
INSERT INTO `menu` VALUES (1,'首页',NULL,'/homepage','/','el-icon-s-home',0,0,18,'2026-03-16 21:44:12'),(2,'系统管理',NULL,'/','/','el-icon-s-tools',0,0,19,'2026-03-16 22:05:51'),(3,'博客管理',NULL,'/','/','el-icon-notebook-2',0,0,24,'2026-03-16 22:25:36'),(4,'圈子管理',NULL,'/','/','el-icon-s-promotion',0,0,25,'2026-03-16 22:29:36'),(5,'网页主页',NULL,'/webhome','/','',0,0,26,'2026-03-16 22:39:54'),(6,'博客页面',NULL,'/blog','/','',0,0,14,'2026-03-16 22:42:14'),(7,'博客编辑',NULL,'/blogwrite','/','',0,0,27,'2026-03-16 22:43:27'),(8,'写博客',NULL,'/blogwrite','/','',0,0,27,'2026-03-16 22:45:37'),(9,'圈子首页',NULL,'/circle','/','',0,0,15,'2026-03-16 22:50:27'),(10,'圈子详情',NULL,'/circle/:id','/','',0,0,28,'2026-03-16 22:52:08'),(11,'个人首页',NULL,'/user','/','',0,0,17,'2026-03-16 22:52:54'),(12,'收藏页面',12,'/collection','/','',0,0,16,'2026-03-16 22:54:08'),(13,'登录页',NULL,'/login','/','',0,0,29,'2026-03-16 22:56:43'),(14,'注册页',NULL,'/registe','/','',0,0,30,'2026-03-16 22:57:10'),(15,'用户管理',2,'/','/','el-icon-user',1,0,20,'2026-03-16 22:12:04'),(16,'角色管理',2,'/role','/','el-icon-user',1,0,12,'2026-03-16 22:16:53'),(17,'菜单管理',2,'/menu','/','el-icon-menu',1,0,22,'2026-03-16 22:18:43'),(18,'权限管理',2,'/permission','/','el-icon-s-fold',1,0,23,'2026-03-16 22:20:03'),(19,'日志管理',2,'/log','/','el-icon-s-management',1,0,10,'2026-03-16 22:22:01'),(20,'标签管理',2,'/label','/','el-icon-collection-tag',1,0,13,'2026-03-16 22:23:42'),(21,'账户管理',15,'/count','/','el-icon-s-custom',2,0,21,'2026-03-16 22:14:22'),(22,'用户信息管理',15,'/info','/','el-icon-document',2,0,11,'2026-03-16 22:16:09'),(23,'审核',3,'/audit','/','el-icon-check',1,0,2,'2026-03-16 22:27:22'),(24,'仪表盘',3,'/dashboard','/','el-icon-data-analysis',1,0,3,'2026-03-16 22:28:36'),(25,'圈子审核',4,'/circleaudit','/','el-icon-check',1,0,4,'2026-03-16 22:31:24'),(26,'圈子管理',4,'/circlemanage','/','el-icon-s-management',1,0,5,'2026-03-16 22:33:13'),(27,'批量通过博客审核',23,NULL,NULL,NULL,0,0,31,'2026-03-21 19:52:20'),(28,'批量拒绝博客审核',23,NULL,NULL,NULL,0,0,32,'2026-03-21 19:52:20'),(29,'批量置顶博客',23,NULL,NULL,NULL,0,0,33,'2026-03-21 19:52:20'),(30,'批量取消置顶博客',23,NULL,NULL,NULL,0,0,34,'2026-03-21 19:52:20'),(31,'导出博客数据',23,NULL,NULL,NULL,0,0,35,'2026-03-21 19:52:20'),(32,'查看博客详情',23,NULL,NULL,NULL,0,0,36,'2026-03-21 19:52:20'),(33,'单个审核通过',23,NULL,NULL,NULL,0,0,37,'2026-03-21 19:52:20'),(34,'单个审核拒绝',23,NULL,NULL,NULL,0,0,38,'2026-03-21 19:52:20'),(35,'置顶博客',23,NULL,NULL,NULL,0,0,39,'2026-03-21 19:52:20'),(36,'取消置顶博客',23,NULL,NULL,NULL,0,0,40,'2026-03-21 19:52:20'),(37,'删除博客',23,NULL,NULL,NULL,0,0,41,'2026-03-21 19:52:20'),(38,'查看已下架博客',23,NULL,NULL,NULL,0,0,42,'2026-03-21 19:52:20'),(39,'恢复已下架博客',23,NULL,NULL,NULL,0,0,43,'2026-03-21 19:52:20'),(40,'刷新热门博客数据',24,NULL,NULL,NULL,0,0,44,'2026-03-21 19:52:20'),(41,'刷新系统状态',24,NULL,NULL,NULL,0,0,45,'2026-03-21 19:52:20'),(42,'批量通过圈子审核',25,NULL,NULL,NULL,0,0,46,'2026-03-21 19:52:20'),(43,'批量关闭圈子',25,NULL,NULL,NULL,0,0,47,'2026-03-21 19:52:20'),(44,'批量删除圈子',25,NULL,NULL,NULL,0,0,48,'2026-03-21 19:52:20'),(45,'导出圈子数据',25,NULL,NULL,NULL,0,0,49,'2026-03-21 19:52:20'),(46,'查看圈子详情',25,NULL,NULL,NULL,0,0,50,'2026-03-21 19:52:20'),(47,'通过圈子审核',25,NULL,NULL,NULL,0,0,51,'2026-03-21 19:52:20'),(48,'推荐圈子',25,NULL,NULL,NULL,0,0,52,'2026-03-21 19:52:20'),(49,'取消推荐圈子',25,NULL,NULL,NULL,0,0,53,'2026-03-21 19:52:20'),(50,'关闭圈子',25,NULL,NULL,NULL,0,0,54,'2026-03-21 19:52:20'),(51,'编辑圈子',25,NULL,NULL,NULL,0,0,55,'2026-03-21 19:52:20'),(52,'删除圈子',25,NULL,NULL,NULL,0,0,56,'2026-03-21 19:52:20'),(53,'圈子成员管理',25,NULL,NULL,NULL,0,0,57,'2026-03-21 19:52:20'),(54,'设为管理员',25,NULL,NULL,NULL,0,0,58,'2026-03-21 19:52:20'),(55,'移除成员',25,NULL,NULL,NULL,0,0,59,'2026-03-21 19:52:20'),(56,'圈子帖子管理',25,NULL,NULL,NULL,0,0,60,'2026-03-21 19:52:20'),(57,'通过帖子审核',25,NULL,NULL,NULL,0,0,61,'2026-03-21 19:52:20'),(58,'删除帖子',25,NULL,NULL,NULL,0,0,62,'2026-03-21 19:52:20'),(59,'创建圈子',26,NULL,NULL,NULL,0,0,63,'2026-03-21 19:52:20'),(60,'刷新圈子列表',26,NULL,NULL,NULL,0,0,64,'2026-03-21 19:52:20'),(61,'导出圈子数据',26,NULL,NULL,NULL,0,0,65,'2026-03-21 19:52:20'),(62,'编辑圈子信息',26,NULL,NULL,NULL,0,0,66,'2026-03-21 19:52:20'),(63,'删除圈子',26,NULL,NULL,NULL,0,0,67,'2026-03-21 19:52:20'),(64,'批量删除帖子',26,NULL,NULL,NULL,0,0,68,'2026-03-21 19:52:20'),(65,'添加分类',26,NULL,NULL,NULL,0,0,69,'2026-03-21 19:52:20'),(66,'排序分类',26,NULL,NULL,NULL,0,0,70,'2026-03-21 19:52:20'),(67,'编辑分类',26,NULL,NULL,NULL,0,0,71,'2026-03-21 19:52:20'),(68,'设为热门分类',26,NULL,NULL,NULL,0,0,72,'2026-03-21 19:52:20'),(69,'取消热门分类',26,NULL,NULL,NULL,0,0,73,'2026-03-21 19:52:20'),(70,'禁用分类',26,NULL,NULL,NULL,0,0,74,'2026-03-21 19:52:20'),(71,'启用分类',26,NULL,NULL,NULL,0,0,75,'2026-03-21 19:52:20'),(72,'删除分类',26,NULL,NULL,NULL,0,0,76,'2026-03-21 19:52:20'),(73,'新增用户',21,NULL,NULL,NULL,0,0,91,'2026-03-21 19:52:20'),(74,'查询用户',21,NULL,NULL,NULL,0,0,92,'2026-03-21 19:52:20'),(75,'重置搜索',21,NULL,NULL,NULL,0,0,93,'2026-03-21 19:52:20'),(76,'编辑用户',21,NULL,NULL,NULL,0,0,94,'2026-03-21 19:52:20'),(77,'重置密码',21,NULL,NULL,NULL,0,0,95,'2026-03-21 19:52:20'),(78,'禁用账户',21,NULL,NULL,NULL,0,0,96,'2026-03-21 19:52:20'),(79,'启用账户',21,NULL,NULL,NULL,0,0,97,'2026-03-21 19:52:20'),(80,'删除账户',21,NULL,NULL,NULL,0,0,98,'2026-03-21 19:52:20'),(81,'查询用户信息',22,NULL,NULL,NULL,0,0,99,'2026-03-21 19:52:20'),(82,'重置搜索',22,NULL,NULL,NULL,0,0,100,'2026-03-21 19:52:20'),(83,'查看用户详情',22,NULL,NULL,NULL,0,0,101,'2026-03-21 19:52:20'),(84,'编辑用户信息',22,NULL,NULL,NULL,0,0,102,'2026-03-21 19:52:20'),(85,'保存用户信息',22,NULL,NULL,NULL,0,0,103,'2026-03-21 19:52:20'),(86,'新增角色',16,NULL,NULL,NULL,0,0,104,'2026-03-21 19:52:20'),(87,'刷新角色列表',16,NULL,NULL,NULL,0,0,105,'2026-03-21 19:52:20'),(88,'搜索角色',16,NULL,NULL,NULL,0,0,106,'2026-03-21 19:52:20'),(89,'编辑角色',16,NULL,NULL,NULL,0,0,107,'2026-03-21 19:52:20'),(90,'权限配置',16,NULL,NULL,NULL,0,0,108,'2026-03-21 19:52:20'),(91,'删除角色',16,NULL,NULL,NULL,0,0,109,'2026-03-21 19:52:20'),(92,'新增菜单',17,NULL,NULL,NULL,0,0,110,'2026-03-21 19:52:20'),(93,'刷新菜单列表',17,NULL,NULL,NULL,0,0,111,'2026-03-21 19:52:20'),(94,'搜索菜单',17,NULL,NULL,NULL,0,0,112,'2026-03-21 19:52:20'),(95,'编辑菜单',17,NULL,NULL,NULL,0,0,113,'2026-03-21 19:52:20'),(96,'添加子菜单',17,NULL,NULL,NULL,0,0,114,'2026-03-21 19:52:20'),(97,'删除菜单',17,NULL,NULL,NULL,0,0,115,'2026-03-21 19:52:20'),(98,'调整排序',17,NULL,NULL,NULL,0,0,116,'2026-03-21 19:52:20'),(99,'切换菜单状态',17,NULL,NULL,NULL,0,0,117,'2026-03-21 19:52:20'),(100,'新增权限',18,NULL,NULL,NULL,0,0,118,'2026-03-21 19:52:20'),(101,'搜索权限',18,NULL,NULL,NULL,0,0,119,'2026-03-21 19:52:20'),(102,'编辑权限',18,NULL,NULL,NULL,0,0,120,'2026-03-21 19:52:20'),(103,'删除权限',18,NULL,NULL,NULL,0,0,121,'2026-03-21 19:52:20'),(104,'查询日志',19,NULL,NULL,NULL,0,0,122,'2026-03-21 19:52:20'),(105,'重置搜索',19,NULL,NULL,NULL,0,0,123,'2026-03-21 19:52:20'),(106,'导出日志',19,NULL,NULL,NULL,0,0,124,'2026-03-21 19:52:20'),(107,'查看日志详情',19,NULL,NULL,NULL,0,0,125,'2026-03-21 19:52:20'),(108,'新增标签',20,NULL,NULL,NULL,0,0,126,'2026-03-21 19:52:20'),(109,'新增分类',20,NULL,NULL,NULL,0,0,127,'2026-03-21 19:52:20'),(110,'搜索标签',20,NULL,NULL,NULL,0,0,128,'2026-03-21 19:52:20'),(111,'查看标签下圈子',20,NULL,NULL,NULL,0,0,129,'2026-03-21 19:52:20'),(112,'编辑标签',20,NULL,NULL,NULL,0,0,130,'2026-03-21 19:52:20'),(113,'设为热门标签',20,NULL,NULL,NULL,0,0,131,'2026-03-21 19:52:20'),(114,'取消热门标签',20,NULL,NULL,NULL,0,0,132,'2026-03-21 19:52:20'),(115,'禁用标签',20,NULL,NULL,NULL,0,0,133,'2026-03-21 19:52:20'),(116,'启用标签',20,NULL,NULL,NULL,0,0,134,'2026-03-21 19:52:20'),(117,'删除标签',20,NULL,NULL,NULL,0,0,135,'2026-03-21 19:52:20'),(118,'查看推荐结果',1,NULL,NULL,NULL,0,0,152,'2026-03-21 19:52:20'),(119,'刷新推荐',1,NULL,NULL,NULL,0,0,153,'2026-03-21 19:52:20'),(120,'导出推荐数据',1,NULL,NULL,NULL,0,0,154,'2026-03-21 19:52:20');
/*!40000 ALTER TABLE `menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `message`
--

DROP TABLE IF EXISTS `message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID，主键',
  `conversation_id` bigint NOT NULL COMMENT '所属会话ID，关联conversation表',
  `sender_id` bigint NOT NULL COMMENT '发送者ID，关联user_info表',
  `content` text NOT NULL COMMENT '消息内容',
  `message_type` enum('text','image','file','emoji') DEFAULT 'text' COMMENT '消息类型',
  `sent_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息发送时间',
  `is_read` tinyint(1) DEFAULT '0' COMMENT '是否已读',
  PRIMARY KEY (`id`),
  KEY `sender_id` (`sender_id`),
  KEY `idx_conversation_sent` (`conversation_id`,`sent_at`),
  CONSTRAINT `message_ibfk_1` FOREIGN KEY (`conversation_id`) REFERENCES `conversation` (`id`) ON DELETE CASCADE,
  CONSTRAINT `message_ibfk_2` FOREIGN KEY (`sender_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='私信/群聊消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `message`
--

LOCK TABLES `message` WRITE;
/*!40000 ALTER TABLE `message` DISABLE KEYS */;
/*!40000 ALTER TABLE `message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification`
--

DROP TABLE IF EXISTS `notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID，主键',
  `receiver_id` bigint NOT NULL COMMENT '接收者ID，关联user_info表',
  `sender_id` bigint DEFAULT NULL COMMENT '发送者ID，关联user_info表，系统通知则为NULL',
  `type` enum('comment','like','follow','reply','system','friend_request','message') NOT NULL COMMENT '通知类型：comment-评论, like-点赞, follow-关注, reply-回复, system-系统通知, friend_request-好友请求, message-新消息',
  `content` text NOT NULL COMMENT '通知的具体内容',
  `read_status` tinyint(1) DEFAULT '0' COMMENT '阅读状态：FALSE-未读, TRUE-已读',
  `target_type` enum('blog','project','comment','circle','conversation') DEFAULT NULL COMMENT '被操作的目标类型',
  `target_id` bigint DEFAULT NULL COMMENT '被操作的目标ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '通知创建时间',
  PRIMARY KEY (`id`),
  KEY `sender_id` (`sender_id`),
  KEY `idx_receiver_read` (`receiver_id`,`read_status`) COMMENT '快速查找用户的未读通知',
  KEY `idx_created_at` (`created_at` DESC),
  CONSTRAINT `notification_ibfk_1` FOREIGN KEY (`receiver_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `notification_ibfk_2` FOREIGN KEY (`sender_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户消息通知中心表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification`
--

LOCK TABLES `notification` WRITE;
/*!40000 ALTER TABLE `notification` DISABLE KEYS */;
/*!40000 ALTER TABLE `notification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `permission`
--

DROP TABLE IF EXISTS `permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permission` (
  `id` int NOT NULL AUTO_INCREMENT,
  `permission_code` varchar(100) NOT NULL COMMENT '权限代码，例如：blog:review, user:disable',
  `description` text COMMENT '权限的详细描述',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '权限创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `permission_code` (`permission_code`)
) ENGINE=InnoDB AUTO_INCREMENT=155 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统权限点表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `permission`
--

LOCK TABLES `permission` WRITE;
/*!40000 ALTER TABLE `permission` DISABLE KEYS */;
INSERT INTO `permission` VALUES (1,'view:admin:algor-reco','算法推荐管理','2026-03-17 19:00:00'),(2,'view:admin:blog-audit','博客审核管理','2026-03-17 19:00:00'),(3,'view:admin:dashboard','后台仪表盘','2026-03-17 19:00:00'),(4,'view:admin:circle-audit','圈子审核管理','2026-03-17 19:00:00'),(5,'view:admin:circle-manage','圈子管理','2026-03-17 19:00:00'),(6,'view:admin:circle-sort','圈子排序管理','2026-03-17 19:00:00'),(7,'view:admin:friend-manage','好友管理','2026-03-17 19:00:00'),(8,'view:admin:official-manage','官方账号管理 / 项目缺失管理','2026-03-17 19:00:00'),(9,'view:admin:project-audit','项目审核管理','2026-03-17 19:00:00'),(10,'view:admin:system-log','系统日志管理 / 项目算法推送','2026-03-17 19:00:00'),(11,'view:admin:user-info','用户信息管理','2026-03-17 19:00:00'),(12,'view:admin:user-role','用户角色管理','2026-03-17 19:00:00'),(13,'view:admin:label-manage','标签管理','2026-03-17 19:00:00'),(14,'view:blog','博客列表','2026-03-17 11:30:09'),(15,'view:circle','圈子首页','2026-03-17 11:30:09'),(16,'view:collection','收藏页面','2026-03-17 11:30:09'),(17,'view:profile','个人主页','2026-03-17 11:30:09'),(18,'view:homepage','后台首页','2026-03-17 13:43:33'),(19,'view:systemmanage','后台系统管理','2026-03-17 14:05:18'),(20,'view:usermanage','后台用户管理','2026-03-17 14:11:08'),(21,'view:admin:user-count','用户账号管理','2026-03-17 14:13:29'),(22,'view:menu','后台菜单管理','2026-03-17 14:17:42'),(23,'view:permission','后台权限管理','2026-03-17 14:19:20'),(24,'view:blogmanage','后台博客管理（目录）','2026-03-17 14:24:22'),(25,'view:circlemanage','后台圈子管理（目录）','2026-03-17 14:32:05'),(26,'view:webhome','网页主页','2026-03-17 14:38:59'),(27,'view:writeblog','写博客','2026-03-17 14:45:06'),(28,'view:circledetail','帖子详情','2026-03-17 14:51:27'),(29,'view:login','登录页面','2026-03-17 14:55:41'),(30,'view:registe','注册页面','2026-03-17 14:56:09'),(31,'btn:blog-audit:batch-approve','批量通过博客审核','2026-03-22 03:52:20'),(32,'btn:blog-audit:batch-reject','批量拒绝博客审核','2026-03-22 03:52:20'),(33,'btn:blog-audit:batch-top','批量置顶博客','2026-03-22 03:52:20'),(34,'btn:blog-audit:batch-cancel-top','批量取消置顶博客','2026-03-22 03:52:20'),(35,'btn:blog-audit:export','导出博客数据','2026-03-22 03:52:20'),(36,'btn:blog-audit:view','查看博客详情','2026-03-22 03:52:20'),(37,'btn:blog-audit:approve','单个审核通过','2026-03-22 03:52:20'),(38,'btn:blog-audit:reject','单个审核拒绝','2026-03-22 03:52:20'),(39,'btn:blog-audit:top','置顶博客','2026-03-22 03:52:20'),(40,'btn:blog-audit:cancel-top','取消置顶博客','2026-03-22 03:52:20'),(41,'btn:blog-audit:delete','删除博客','2026-03-22 03:52:20'),(42,'btn:blog-audit:view-unpublished','查看已下架博客','2026-03-22 03:52:20'),(43,'btn:blog-audit:restore','恢复已下架博客','2026-03-22 03:52:20'),(44,'btn:dashboard:refresh-hot','刷新热门博客数据','2026-03-22 03:52:20'),(45,'btn:dashboard:refresh-status','刷新系统状态','2026-03-22 03:52:20'),(46,'btn:circle-audit:batch-approve','批量通过圈子审核','2026-03-22 03:52:20'),(47,'btn:circle-audit:batch-close','批量关闭圈子','2026-03-22 03:52:20'),(48,'btn:circle-audit:batch-delete','批量删除圈子','2026-03-22 03:52:20'),(49,'btn:circle-audit:export','导出圈子数据','2026-03-22 03:52:20'),(50,'btn:circle-audit:view','查看圈子详情','2026-03-22 03:52:20'),(51,'btn:circle-audit:approve','通过圈子审核','2026-03-22 03:52:20'),(52,'btn:circle-audit:recommend','推荐圈子','2026-03-22 03:52:20'),(53,'btn:circle-audit:cancel-recommend','取消推荐圈子','2026-03-22 03:52:20'),(54,'btn:circle-audit:close','关闭圈子','2026-03-22 03:52:20'),(55,'btn:circle-audit:edit','编辑圈子','2026-03-22 03:52:20'),(56,'btn:circle-audit:delete','删除圈子','2026-03-22 03:52:20'),(57,'btn:circle-audit:member-manage','圈子成员管理','2026-03-22 03:52:20'),(58,'btn:circle-audit:set-admin','设为管理员','2026-03-22 03:52:20'),(59,'btn:circle-audit:remove-member','移除成员','2026-03-22 03:52:20'),(60,'btn:circle-audit:post-manage','圈子帖子管理','2026-03-22 03:52:20'),(61,'btn:circle-audit:approve-post','通过帖子审核','2026-03-22 03:52:20'),(62,'btn:circle-audit:delete-post','删除帖子','2026-03-22 03:52:20'),(63,'btn:circle-manage:create','创建圈子','2026-03-22 03:52:20'),(64,'btn:circle-manage:refresh','刷新圈子列表','2026-03-22 03:52:20'),(65,'btn:circle-manage:export','导出圈子数据','2026-03-22 03:52:20'),(66,'btn:circle-manage:edit','编辑圈子信息','2026-03-22 03:52:20'),(67,'btn:circle-manage:delete','删除圈子','2026-03-22 03:52:20'),(68,'btn:circle-manage:batch-delete-post','批量删除帖子','2026-03-22 03:52:20'),(69,'btn:circle-category:create','添加分类','2026-03-22 03:52:20'),(70,'btn:circle-category:sort','排序分类','2026-03-22 03:52:20'),(71,'btn:circle-category:edit','编辑分类','2026-03-22 03:52:20'),(72,'btn:circle-category:set-hot','设为热门分类','2026-03-22 03:52:20'),(73,'btn:circle-category:cancel-hot','取消热门分类','2026-03-22 03:52:20'),(74,'btn:circle-category:disable','禁用分类','2026-03-22 03:52:20'),(75,'btn:circle-category:enable','启用分类','2026-03-22 03:52:20'),(76,'btn:circle-category:delete','删除分类','2026-03-22 03:52:20'),(77,'btn:friend:add','添加好友','2026-03-22 03:52:20'),(78,'btn:friend:refresh','刷新好友列表','2026-03-22 03:52:20'),(79,'btn:friend:edit-remark','编辑备注','2026-03-22 03:52:20'),(80,'btn:friend:change-group','修改分组','2026-03-22 03:52:20'),(81,'btn:friend:set-special','特别关注','2026-03-22 03:52:20'),(82,'btn:friend:cancel-special','取消特别关注','2026-03-22 03:52:20'),(83,'btn:friend:send-message','发送消息','2026-03-22 03:52:20'),(84,'btn:friend:view-profile','查看用户资料','2026-03-22 03:52:20'),(85,'btn:friend:delete','删除好友','2026-03-22 03:52:20'),(86,'btn:friend:request-approve','同意好友申请','2026-03-22 03:52:20'),(87,'btn:friend:request-reject','拒绝好友申请','2026-03-22 03:52:20'),(88,'btn:friend-group:create','添加分组','2026-03-22 03:52:20'),(89,'btn:friend-group:edit','编辑分组','2026-03-22 03:52:20'),(90,'btn:friend-group:delete','删除分组','2026-03-22 03:52:20'),(91,'btn:user:create','新增用户','2026-03-22 03:52:20'),(92,'btn:user:search','查询用户','2026-03-22 03:52:20'),(93,'btn:user:reset-search','重置搜索','2026-03-22 03:52:20'),(94,'btn:user:edit','编辑用户','2026-03-22 03:52:20'),(95,'btn:user:reset-password','重置密码','2026-03-22 03:52:20'),(96,'btn:user:disable','禁用账户','2026-03-22 03:52:20'),(97,'btn:user:enable','启用账户','2026-03-22 03:52:20'),(98,'btn:user:delete','删除账户','2026-03-22 03:52:20'),(99,'btn:user-info:search','查询用户信息','2026-03-22 03:52:20'),(100,'btn:user-info:reset','重置搜索','2026-03-22 03:52:20'),(101,'btn:user-info:view','查看用户详情','2026-03-22 03:52:20'),(102,'btn:user-info:edit','编辑用户信息','2026-03-22 03:52:20'),(103,'btn:user-info:save','保存用户信息','2026-03-22 03:52:20'),(104,'btn:role:create','新增角色','2026-03-22 03:52:20'),(105,'btn:role:refresh','刷新角色列表','2026-03-22 03:52:20'),(106,'btn:role:search','搜索角色','2026-03-22 03:52:20'),(107,'btn:role:edit','编辑角色','2026-03-22 03:52:20'),(108,'btn:role:assign-permission','权限配置','2026-03-22 03:52:20'),(109,'btn:role:delete','删除角色','2026-03-22 03:52:20'),(110,'btn:menu:create','新增菜单','2026-03-22 03:52:20'),(111,'btn:menu:refresh','刷新菜单列表','2026-03-22 03:52:20'),(112,'btn:menu:search','搜索菜单','2026-03-22 03:52:20'),(113,'btn:menu:edit','编辑菜单','2026-03-22 03:52:20'),(114,'btn:menu:add-child','添加子菜单','2026-03-22 03:52:20'),(115,'btn:menu:delete','删除菜单','2026-03-22 03:52:20'),(116,'btn:menu:sort','调整排序','2026-03-22 03:52:20'),(117,'btn:menu:toggle-status','切换菜单状态','2026-03-22 03:52:20'),(118,'btn:permission:create','新增权限','2026-03-22 03:52:20'),(119,'btn:permission:search','搜索权限','2026-03-22 03:52:20'),(120,'btn:permission:edit','编辑权限','2026-03-22 03:52:20'),(121,'btn:permission:delete','删除权限','2026-03-22 03:52:20'),(122,'btn:log:search','查询日志','2026-03-22 03:52:20'),(123,'btn:log:reset','重置搜索','2026-03-22 03:52:20'),(124,'btn:log:export','导出日志','2026-03-22 03:52:20'),(125,'btn:log:view-detail','查看日志详情','2026-03-22 03:52:20'),(126,'btn:tag:create','新增标签','2026-03-22 03:52:20'),(127,'btn:tag-category:create','新增分类','2026-03-22 03:52:20'),(128,'btn:tag:search','搜索标签','2026-03-22 03:52:20'),(129,'btn:tag:view-circles','查看标签下圈子','2026-03-22 03:52:20'),(130,'btn:tag:edit','编辑标签','2026-03-22 03:52:20'),(131,'btn:tag:set-hot','设为热门标签','2026-03-22 03:52:20'),(132,'btn:tag:cancel-hot','取消热门标签','2026-03-22 03:52:20'),(133,'btn:tag:disable','禁用标签','2026-03-22 03:52:20'),(134,'btn:tag:enable','启用标签','2026-03-22 03:52:20'),(135,'btn:tag:delete','删除标签','2026-03-22 03:52:20'),(136,'btn:project-audit:batch-approve','批量通过项目','2026-03-22 03:52:20'),(137,'btn:project-audit:batch-reject','批量拒绝项目','2026-03-22 03:52:20'),(138,'btn:project-audit:batch-recommend','批量推荐项目','2026-03-22 03:52:20'),(139,'btn:project-audit:batch-cancel-recommend','批量取消推荐','2026-03-22 03:52:20'),(140,'btn:project-audit:export','导出项目数据','2026-03-22 03:52:20'),(141,'btn:project-audit:view','查看项目详情','2026-03-22 03:52:20'),(142,'btn:project-audit:approve','通过项目审核','2026-03-22 03:52:20'),(143,'btn:project-audit:reject','拒绝项目审核','2026-03-22 03:52:20'),(144,'btn:project-audit:recommend','推荐项目','2026-03-22 03:52:20'),(145,'btn:project-audit:cancel-recommend','取消推荐项目','2026-03-22 03:52:20'),(146,'btn:project-audit:delete','删除项目','2026-03-22 03:52:20'),(147,'btn:project-miss:batch-restore','批量恢复项目','2026-03-22 03:52:20'),(148,'btn:project-miss:batch-permanent-delete','批量永久删除','2026-03-22 03:52:20'),(149,'btn:project-miss:restore','恢复项目','2026-03-22 03:52:20'),(150,'btn:project-miss:permanent-delete','永久删除项目','2026-03-22 03:52:20'),(151,'btn:project-miss:export','导出下架项目','2026-03-22 03:52:20'),(152,'btn:algo-reco:view','查看推荐结果','2026-03-22 03:52:20'),(153,'btn:algo-reco:refresh','刷新推荐','2026-03-22 03:52:20'),(154,'btn:algo-reco:export','导出推荐数据','2026-03-22 03:52:20');
/*!40000 ALTER TABLE `permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project`
--

DROP TABLE IF EXISTS `project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  PRIMARY KEY (`id`),
  KEY `idx_author_status` (`author_id`,`status`),
  CONSTRAINT `project_ibfk_1` FOREIGN KEY (`author_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目资源表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project`
--

LOCK TABLES `project` WRITE;
/*!40000 ALTER TABLE `project` DISABLE KEYS */;
/*!40000 ALTER TABLE `project` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_file`
--

DROP TABLE IF EXISTS `project_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件ID，主键',
  `project_id` bigint NOT NULL COMMENT '所属项目ID，关联project表',
  `file_name` varchar(255) NOT NULL COMMENT '文件原始名称',
  `file_path` varchar(500) NOT NULL COMMENT '文件在服务器上的存储路径',
  `file_size_bytes` bigint DEFAULT NULL COMMENT '文件大小，单位字节(Bytes)',
  `file_type` varchar(50) DEFAULT NULL COMMENT '文件类型，例如 zip, jar, war, exe, pdf',
  `upload_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '文件上传时间',
  `is_main` tinyint(1) DEFAULT '0' COMMENT '是否为主要文件，默认FALSE。一个项目应有且仅有一个主文件',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_file_name` (`project_id`,`file_name`) COMMENT '确保同一项目内文件名不重复',
  CONSTRAINT `project_file_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目附件文件表，存储项目的各种资源文件';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_file`
--

LOCK TABLES `project_file` WRITE;
/*!40000 ALTER TABLE `project_file` DISABLE KEYS */;
/*!40000 ALTER TABLE `project_file` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `recommendation_result`
--

DROP TABLE IF EXISTS `recommendation_result`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recommendation_result` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '推荐结果ID，主键',
  `user_id` bigint NOT NULL COMMENT '目标用户ID，关联user_info表',
  `algorithm_version` varchar(50) NOT NULL COMMENT '生成本次推荐的算法版本号',
  `recommended_items` json NOT NULL COMMENT '推荐的项目列表，JSON格式，例如 [{"type": "blog", "id": 1}, {"type": "project", "id": 2}]',
  `generated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '推荐结果生成时间',
  `consumed` tinyint(1) DEFAULT '0' COMMENT '是否已被消费（例如：是否已推送给用户）',
  `consumed_at` timestamp NULL DEFAULT NULL COMMENT '消费时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_generated` (`user_id`,`generated_at` DESC),
  KEY `idx_consumed` (`consumed`),
  CONSTRAINT `recommendation_result_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='为用户生成的个性化推荐列表缓存表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `recommendation_result`
--

LOCK TABLES `recommendation_result` WRITE;
/*!40000 ALTER TABLE `recommendation_result` DISABLE KEYS */;
/*!40000 ALTER TABLE `recommendation_result` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `recommendation_rule`
--

DROP TABLE IF EXISTS `recommendation_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recommendation_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID，主键',
  `name` varchar(100) NOT NULL COMMENT '规则名称',
  `description` text COMMENT '规则描述',
  `rule_type` enum('content_based','collaborative','hot_trending','user_based') DEFAULT 'content_based' COMMENT '推荐算法类型：content_based-基于内容, collaborative-协同过滤, hot_trending-热门趋势, user_based-基于用户',
  `weight` decimal(5,2) DEFAULT '1.00' COMMENT '该规则在综合推荐中的权重',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '规则是否启用：TRUE-启用, FALSE-禁用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '规则创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '规则最后更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='推荐系统规则配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `recommendation_rule`
--

LOCK TABLES `recommendation_rule` WRITE;
/*!40000 ALTER TABLE `recommendation_rule` DISABLE KEYS */;
/*!40000 ALTER TABLE `recommendation_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `region`
--

DROP TABLE IF EXISTS `region`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `region` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '地区ID，主键',
  `name` varchar(100) NOT NULL COMMENT '地区名称，例如：北京市、朝阳区',
  `parent_id` bigint DEFAULT NULL COMMENT '父级地区ID，用于构建省市区三级树状结构，顶级区域为NULL',
  `level` enum('province','city','district') NOT NULL COMMENT '地区层级：province-省, city-市, district-区县',
  `code` varchar(20) DEFAULT NULL COMMENT '地区编码，例如国标码 CN-110000',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  PRIMARY KEY (`id`),
  KEY `parent_id` (`parent_id`),
  CONSTRAINT `region_ibfk_1` FOREIGN KEY (`parent_id`) REFERENCES `region` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=804 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='地区信息表，支持多级行政区域划分';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `region`
--

LOCK TABLES `region` WRITE;
/*!40000 ALTER TABLE `region` DISABLE KEYS */;
INSERT INTO `region` VALUES (1,'北京市',NULL,'province','110000','2026-03-17 12:13:46'),(2,'天津市',NULL,'province','120000','2026-03-17 12:13:46'),(3,'河北省',NULL,'province','130000','2026-03-17 12:13:46'),(4,'山西省',NULL,'province','140000','2026-03-17 12:13:46'),(5,'内蒙古自治区',NULL,'province','150000','2026-03-17 12:13:46'),(6,'辽宁省',NULL,'province','210000','2026-03-17 12:13:46'),(7,'吉林省',NULL,'province','220000','2026-03-17 12:13:46'),(8,'黑龙江省',NULL,'province','230000','2026-03-17 12:13:46'),(9,'上海市',NULL,'province','310000','2026-03-17 12:13:46'),(10,'江苏省',NULL,'province','320000','2026-03-17 12:13:46'),(11,'浙江省',NULL,'province','330000','2026-03-17 12:13:46'),(12,'安徽省',NULL,'province','340000','2026-03-17 12:13:46'),(13,'福建省',NULL,'province','350000','2026-03-17 12:13:46'),(14,'江西省',NULL,'province','360000','2026-03-17 12:13:46'),(15,'山东省',NULL,'province','370000','2026-03-17 12:13:46'),(16,'河南省',NULL,'province','410000','2026-03-17 12:13:46'),(17,'湖北省',NULL,'province','420000','2026-03-17 12:13:46'),(18,'湖南省',NULL,'province','430000','2026-03-17 12:13:46'),(19,'广东省',NULL,'province','440000','2026-03-17 12:13:46'),(20,'广西壮族自治区',NULL,'province','450000','2026-03-17 12:13:46'),(21,'海南省',NULL,'province','460000','2026-03-17 12:13:46'),(22,'重庆市',NULL,'province','500000','2026-03-17 12:13:46'),(23,'四川省',NULL,'province','510000','2026-03-17 12:13:46'),(24,'贵州省',NULL,'province','520000','2026-03-17 12:13:46'),(25,'云南省',NULL,'province','530000','2026-03-17 12:13:46'),(26,'西藏自治区',NULL,'province','540000','2026-03-17 12:13:46'),(27,'陕西省',NULL,'province','610000','2026-03-17 12:13:46'),(28,'甘肃省',NULL,'province','620000','2026-03-17 12:13:46'),(29,'青海省',NULL,'province','630000','2026-03-17 12:13:46'),(30,'宁夏回族自治区',NULL,'province','640000','2026-03-17 12:13:46'),(31,'新疆维吾尔自治区',NULL,'province','650000','2026-03-17 12:13:46'),(32,'台湾省',NULL,'province','710000','2026-03-17 12:13:46'),(33,'香港特别行政区',NULL,'province','810000','2026-03-17 12:13:46'),(34,'澳门特别行政区',NULL,'province','820000','2026-03-17 12:13:46'),(35,'北京市',1,'city','110100','2026-03-17 12:15:59'),(36,'天津市',2,'city','120100','2026-03-17 12:15:59'),(37,'石家庄市',3,'city','130100','2026-03-17 12:15:59'),(38,'唐山市',3,'city','130200','2026-03-17 12:15:59'),(39,'秦皇岛市',3,'city','130300','2026-03-17 12:15:59'),(40,'邯郸市',3,'city','130400','2026-03-17 12:15:59'),(41,'邢台市',3,'city','130500','2026-03-17 12:15:59'),(42,'保定市',3,'city','130600','2026-03-17 12:15:59'),(43,'张家口市',3,'city','130700','2026-03-17 12:15:59'),(44,'承德市',3,'city','130800','2026-03-17 12:15:59'),(45,'沧州市',3,'city','130900','2026-03-17 12:15:59'),(46,'廊坊市',3,'city','131000','2026-03-17 12:15:59'),(47,'衡水市',3,'city','131100','2026-03-17 12:15:59'),(48,'太原市',4,'city','140100','2026-03-17 12:15:59'),(49,'大同市',4,'city','140200','2026-03-17 12:15:59'),(50,'阳泉市',4,'city','140300','2026-03-17 12:15:59'),(51,'长治市',4,'city','140400','2026-03-17 12:15:59'),(52,'晋城市',4,'city','140500','2026-03-17 12:15:59'),(53,'朔州市',4,'city','140600','2026-03-17 12:15:59'),(54,'晋中市',4,'city','140700','2026-03-17 12:15:59'),(55,'运城市',4,'city','140800','2026-03-17 12:15:59'),(56,'忻州市',4,'city','140900','2026-03-17 12:15:59'),(57,'临汾市',4,'city','141000','2026-03-17 12:15:59'),(58,'吕梁市',4,'city','141100','2026-03-17 12:15:59'),(59,'呼和浩特市',5,'city','150100','2026-03-17 12:15:59'),(60,'包头市',5,'city','150200','2026-03-17 12:15:59'),(61,'乌海市',5,'city','150300','2026-03-17 12:15:59'),(62,'赤峰市',5,'city','150400','2026-03-17 12:15:59'),(63,'通辽市',5,'city','150500','2026-03-17 12:15:59'),(64,'鄂尔多斯市',5,'city','150600','2026-03-17 12:15:59'),(65,'呼伦贝尔市',5,'city','150700','2026-03-17 12:15:59'),(66,'巴彦淖尔市',5,'city','150800','2026-03-17 12:15:59'),(67,'乌兰察布市',5,'city','150900','2026-03-17 12:15:59'),(68,'兴安盟',5,'city','152200','2026-03-17 12:15:59'),(69,'锡林郭勒盟',5,'city','152500','2026-03-17 12:15:59'),(70,'阿拉善盟',5,'city','152900','2026-03-17 12:15:59'),(71,'沈阳市',6,'city','210100','2026-03-17 12:15:59'),(72,'大连市',6,'city','210200','2026-03-17 12:15:59'),(73,'鞍山市',6,'city','210300','2026-03-17 12:15:59'),(74,'抚顺市',6,'city','210400','2026-03-17 12:15:59'),(75,'本溪市',6,'city','210500','2026-03-17 12:15:59'),(76,'丹东市',6,'city','210600','2026-03-17 12:15:59'),(77,'锦州市',6,'city','210700','2026-03-17 12:15:59'),(78,'营口市',6,'city','210800','2026-03-17 12:15:59'),(79,'阜新市',6,'city','210900','2026-03-17 12:15:59'),(80,'辽阳市',6,'city','211000','2026-03-17 12:15:59'),(81,'盘锦市',6,'city','211100','2026-03-17 12:15:59'),(82,'铁岭市',6,'city','211200','2026-03-17 12:15:59'),(83,'朝阳市',6,'city','211300','2026-03-17 12:15:59'),(84,'葫芦岛市',6,'city','211400','2026-03-17 12:15:59'),(85,'长春市',7,'city','220100','2026-03-17 12:15:59'),(86,'吉林市',7,'city','220200','2026-03-17 12:15:59'),(87,'四平市',7,'city','220300','2026-03-17 12:15:59'),(88,'辽源市',7,'city','220400','2026-03-17 12:15:59'),(89,'通化市',7,'city','220500','2026-03-17 12:15:59'),(90,'白山市',7,'city','220600','2026-03-17 12:15:59'),(91,'松原市',7,'city','220700','2026-03-17 12:15:59'),(92,'白城市',7,'city','220800','2026-03-17 12:15:59'),(93,'延边朝鲜族自治州',7,'city','222400','2026-03-17 12:15:59'),(94,'哈尔滨市',8,'city','230100','2026-03-17 12:15:59'),(95,'齐齐哈尔市',8,'city','230200','2026-03-17 12:15:59'),(96,'鸡西市',8,'city','230300','2026-03-17 12:15:59'),(97,'鹤岗市',8,'city','230400','2026-03-17 12:15:59'),(98,'双鸭山市',8,'city','230500','2026-03-17 12:15:59'),(99,'大庆市',8,'city','230600','2026-03-17 12:15:59'),(100,'伊春市',8,'city','230700','2026-03-17 12:15:59'),(101,'佳木斯市',8,'city','230800','2026-03-17 12:15:59'),(102,'七台河市',8,'city','230900','2026-03-17 12:15:59'),(103,'牡丹江市',8,'city','231000','2026-03-17 12:15:59'),(104,'黑河市',8,'city','231100','2026-03-17 12:15:59'),(105,'绥化市',8,'city','231200','2026-03-17 12:15:59'),(106,'大兴安岭地区',8,'city','232700','2026-03-17 12:15:59'),(107,'上海市',9,'city','310100','2026-03-17 12:15:59'),(108,'南京市',10,'city','320100','2026-03-17 12:15:59'),(109,'无锡市',10,'city','320200','2026-03-17 12:15:59'),(110,'徐州市',10,'city','320300','2026-03-17 12:15:59'),(111,'常州市',10,'city','320400','2026-03-17 12:15:59'),(112,'苏州市',10,'city','320500','2026-03-17 12:15:59'),(113,'南通市',10,'city','320600','2026-03-17 12:15:59'),(114,'连云港市',10,'city','320700','2026-03-17 12:15:59'),(115,'淮安市',10,'city','320800','2026-03-17 12:15:59'),(116,'盐城市',10,'city','320900','2026-03-17 12:15:59'),(117,'扬州市',10,'city','321000','2026-03-17 12:15:59'),(118,'镇江市',10,'city','321100','2026-03-17 12:15:59'),(119,'泰州市',10,'city','321200','2026-03-17 12:15:59'),(120,'宿迁市',10,'city','321300','2026-03-17 12:15:59'),(121,'杭州市',11,'city','330100','2026-03-17 12:15:59'),(122,'宁波市',11,'city','330200','2026-03-17 12:15:59'),(123,'温州市',11,'city','330300','2026-03-17 12:15:59'),(124,'嘉兴市',11,'city','330400','2026-03-17 12:15:59'),(125,'湖州市',11,'city','330500','2026-03-17 12:15:59'),(126,'绍兴市',11,'city','330600','2026-03-17 12:15:59'),(127,'金华市',11,'city','330700','2026-03-17 12:15:59'),(128,'衢州市',11,'city','330800','2026-03-17 12:15:59'),(129,'舟山市',11,'city','330900','2026-03-17 12:15:59'),(130,'台州市',11,'city','331000','2026-03-17 12:15:59'),(131,'丽水市',11,'city','331100','2026-03-17 12:15:59'),(132,'合肥市',12,'city','340100','2026-03-17 12:15:59'),(133,'芜湖市',12,'city','340200','2026-03-17 12:15:59'),(134,'蚌埠市',12,'city','340300','2026-03-17 12:15:59'),(135,'淮南市',12,'city','340400','2026-03-17 12:15:59'),(136,'马鞍山市',12,'city','340500','2026-03-17 12:15:59'),(137,'淮北市',12,'city','340600','2026-03-17 12:15:59'),(138,'铜陵市',12,'city','340700','2026-03-17 12:15:59'),(139,'安庆市',12,'city','340800','2026-03-17 12:15:59'),(140,'黄山市',12,'city','341000','2026-03-17 12:15:59'),(141,'滁州市',12,'city','341100','2026-03-17 12:15:59'),(142,'阜阳市',12,'city','341200','2026-03-17 12:15:59'),(143,'宿州市',12,'city','341300','2026-03-17 12:15:59'),(144,'六安市',12,'city','341500','2026-03-17 12:15:59'),(145,'亳州市',12,'city','341600','2026-03-17 12:15:59'),(146,'池州市',12,'city','341700','2026-03-17 12:15:59'),(147,'宣城市',12,'city','341800','2026-03-17 12:15:59'),(148,'福州市',13,'city','350100','2026-03-17 12:15:59'),(149,'厦门市',13,'city','350200','2026-03-17 12:15:59'),(150,'莆田市',13,'city','350300','2026-03-17 12:15:59'),(151,'三明市',13,'city','350400','2026-03-17 12:15:59'),(152,'泉州市',13,'city','350500','2026-03-17 12:15:59'),(153,'漳州市',13,'city','350600','2026-03-17 12:15:59'),(154,'南平市',13,'city','350700','2026-03-17 12:15:59'),(155,'龙岩市',13,'city','350800','2026-03-17 12:15:59'),(156,'宁德市',13,'city','350900','2026-03-17 12:15:59'),(157,'南昌市',14,'city','360100','2026-03-17 12:15:59'),(158,'景德镇市',14,'city','360200','2026-03-17 12:15:59'),(159,'萍乡市',14,'city','360300','2026-03-17 12:15:59'),(160,'九江市',14,'city','360400','2026-03-17 12:15:59'),(161,'新余市',14,'city','360500','2026-03-17 12:15:59'),(162,'鹰潭市',14,'city','360600','2026-03-17 12:15:59'),(163,'赣州市',14,'city','360700','2026-03-17 12:15:59'),(164,'吉安市',14,'city','360800','2026-03-17 12:15:59'),(165,'宜春市',14,'city','360900','2026-03-17 12:15:59'),(166,'抚州市',14,'city','361000','2026-03-17 12:15:59'),(167,'上饶市',14,'city','361100','2026-03-17 12:15:59'),(168,'济南市',15,'city','370100','2026-03-17 12:15:59'),(169,'青岛市',15,'city','370200','2026-03-17 12:15:59'),(170,'淄博市',15,'city','370300','2026-03-17 12:15:59'),(171,'枣庄市',15,'city','370400','2026-03-17 12:15:59'),(172,'东营市',15,'city','370500','2026-03-17 12:15:59'),(173,'烟台市',15,'city','370600','2026-03-17 12:15:59'),(174,'潍坊市',15,'city','370700','2026-03-17 12:15:59'),(175,'济宁市',15,'city','370800','2026-03-17 12:15:59'),(176,'泰安市',15,'city','370900','2026-03-17 12:15:59'),(177,'威海市',15,'city','371000','2026-03-17 12:15:59'),(178,'日照市',15,'city','371100','2026-03-17 12:15:59'),(179,'临沂市',15,'city','371300','2026-03-17 12:15:59'),(180,'德州市',15,'city','371400','2026-03-17 12:15:59'),(181,'聊城市',15,'city','371500','2026-03-17 12:15:59'),(182,'滨州市',15,'city','371600','2026-03-17 12:15:59'),(183,'菏泽市',15,'city','371700','2026-03-17 12:15:59'),(184,'郑州市',16,'city','410100','2026-03-17 12:15:59'),(185,'开封市',16,'city','410200','2026-03-17 12:15:59'),(186,'洛阳市',16,'city','410300','2026-03-17 12:15:59'),(187,'平顶山市',16,'city','410400','2026-03-17 12:15:59'),(188,'安阳市',16,'city','410500','2026-03-17 12:15:59'),(189,'鹤壁市',16,'city','410600','2026-03-17 12:15:59'),(190,'新乡市',16,'city','410700','2026-03-17 12:15:59'),(191,'焦作市',16,'city','410800','2026-03-17 12:15:59'),(192,'濮阳市',16,'city','410900','2026-03-17 12:15:59'),(193,'许昌市',16,'city','411000','2026-03-17 12:15:59'),(194,'漯河市',16,'city','411100','2026-03-17 12:15:59'),(195,'三门峡市',16,'city','411200','2026-03-17 12:15:59'),(196,'南阳市',16,'city','411300','2026-03-17 12:15:59'),(197,'商丘市',16,'city','411400','2026-03-17 12:15:59'),(198,'信阳市',16,'city','411500','2026-03-17 12:15:59'),(199,'周口市',16,'city','411600','2026-03-17 12:15:59'),(200,'驻马店市',16,'city','411700','2026-03-17 12:15:59'),(201,'济源市',16,'city','419001','2026-03-17 12:15:59'),(202,'武汉市',17,'city','420100','2026-03-17 12:15:59'),(203,'黄石市',17,'city','420200','2026-03-17 12:15:59'),(204,'十堰市',17,'city','420300','2026-03-17 12:15:59'),(205,'宜昌市',17,'city','420500','2026-03-17 12:15:59'),(206,'襄阳市',17,'city','420600','2026-03-17 12:15:59'),(207,'鄂州市',17,'city','420700','2026-03-17 12:15:59'),(208,'荆门市',17,'city','420800','2026-03-17 12:15:59'),(209,'孝感市',17,'city','420900','2026-03-17 12:15:59'),(210,'荆州市',17,'city','421000','2026-03-17 12:15:59'),(211,'黄冈市',17,'city','421100','2026-03-17 12:15:59'),(212,'咸宁市',17,'city','421200','2026-03-17 12:15:59'),(213,'随州市',17,'city','421300','2026-03-17 12:15:59'),(214,'恩施土家族苗族自治州',17,'city','422800','2026-03-17 12:15:59'),(215,'仙桃市',17,'city','429004','2026-03-17 12:15:59'),(216,'潜江市',17,'city','429005','2026-03-17 12:15:59'),(217,'天门市',17,'city','429006','2026-03-17 12:15:59'),(218,'神农架林区',17,'city','429021','2026-03-17 12:15:59'),(219,'长沙市',18,'city','430100','2026-03-17 12:15:59'),(220,'株洲市',18,'city','430200','2026-03-17 12:15:59'),(221,'湘潭市',18,'city','430300','2026-03-17 12:15:59'),(222,'衡阳市',18,'city','430400','2026-03-17 12:15:59'),(223,'邵阳市',18,'city','430500','2026-03-17 12:15:59'),(224,'岳阳市',18,'city','430600','2026-03-17 12:15:59'),(225,'常德市',18,'city','430700','2026-03-17 12:15:59'),(226,'张家界市',18,'city','430800','2026-03-17 12:15:59'),(227,'益阳市',18,'city','430900','2026-03-17 12:15:59'),(228,'郴州市',18,'city','431000','2026-03-17 12:15:59'),(229,'永州市',18,'city','431100','2026-03-17 12:15:59'),(230,'怀化市',18,'city','431200','2026-03-17 12:15:59'),(231,'娄底市',18,'city','431300','2026-03-17 12:15:59'),(232,'湘西土家族苗族自治州',18,'city','433100','2026-03-17 12:15:59'),(233,'广州市',19,'city','440100','2026-03-17 12:15:59'),(234,'韶关市',19,'city','440200','2026-03-17 12:15:59'),(235,'深圳市',19,'city','440300','2026-03-17 12:15:59'),(236,'珠海市',19,'city','440400','2026-03-17 12:15:59'),(237,'汕头市',19,'city','440500','2026-03-17 12:15:59'),(238,'佛山市',19,'city','440600','2026-03-17 12:15:59'),(239,'江门市',19,'city','440700','2026-03-17 12:15:59'),(240,'湛江市',19,'city','440800','2026-03-17 12:15:59'),(241,'茂名市',19,'city','440900','2026-03-17 12:15:59'),(242,'肇庆市',19,'city','441200','2026-03-17 12:15:59'),(243,'惠州市',19,'city','441300','2026-03-17 12:15:59'),(244,'梅州市',19,'city','441400','2026-03-17 12:15:59'),(245,'汕尾市',19,'city','441500','2026-03-17 12:15:59'),(246,'河源市',19,'city','441600','2026-03-17 12:15:59'),(247,'阳江市',19,'city','441700','2026-03-17 12:15:59'),(248,'清远市',19,'city','441800','2026-03-17 12:15:59'),(249,'东莞市',19,'city','441900','2026-03-17 12:15:59'),(250,'中山市',19,'city','442000','2026-03-17 12:15:59'),(251,'潮州市',19,'city','445100','2026-03-17 12:15:59'),(252,'揭阳市',19,'city','445200','2026-03-17 12:15:59'),(253,'云浮市',19,'city','445300','2026-03-17 12:15:59'),(254,'南宁市',20,'city','450100','2026-03-17 12:15:59'),(255,'柳州市',20,'city','450200','2026-03-17 12:15:59'),(256,'桂林市',20,'city','450300','2026-03-17 12:15:59'),(257,'梧州市',20,'city','450400','2026-03-17 12:15:59'),(258,'北海市',20,'city','450500','2026-03-17 12:15:59'),(259,'防城港市',20,'city','450600','2026-03-17 12:15:59'),(260,'钦州市',20,'city','450700','2026-03-17 12:15:59'),(261,'贵港市',20,'city','450800','2026-03-17 12:15:59'),(262,'玉林市',20,'city','450900','2026-03-17 12:15:59'),(263,'百色市',20,'city','451000','2026-03-17 12:15:59'),(264,'贺州市',20,'city','451100','2026-03-17 12:15:59'),(265,'河池市',20,'city','451200','2026-03-17 12:15:59'),(266,'来宾市',20,'city','451300','2026-03-17 12:15:59'),(267,'崇左市',20,'city','451400','2026-03-17 12:15:59'),(268,'海口市',21,'city','460100','2026-03-17 12:15:59'),(269,'三亚市',21,'city','460200','2026-03-17 12:15:59'),(270,'三沙市',21,'city','460300','2026-03-17 12:15:59'),(271,'儋州市',21,'city','460400','2026-03-17 12:15:59'),(272,'重庆市',22,'city','500100','2026-03-17 12:15:59'),(273,'成都市',23,'city','510100','2026-03-17 12:15:59'),(274,'自贡市',23,'city','510300','2026-03-17 12:15:59'),(275,'攀枝花市',23,'city','510400','2026-03-17 12:15:59'),(276,'泸州市',23,'city','510500','2026-03-17 12:15:59'),(277,'德阳市',23,'city','510600','2026-03-17 12:15:59'),(278,'绵阳市',23,'city','510700','2026-03-17 12:15:59'),(279,'广元市',23,'city','510800','2026-03-17 12:15:59'),(280,'遂宁市',23,'city','510900','2026-03-17 12:15:59'),(281,'内江市',23,'city','511000','2026-03-17 12:15:59'),(282,'乐山市',23,'city','511100','2026-03-17 12:15:59'),(283,'南充市',23,'city','511300','2026-03-17 12:15:59'),(284,'眉山市',23,'city','511400','2026-03-17 12:15:59'),(285,'宜宾市',23,'city','511500','2026-03-17 12:15:59'),(286,'广安市',23,'city','511600','2026-03-17 12:15:59'),(287,'达州市',23,'city','511700','2026-03-17 12:15:59'),(288,'雅安市',23,'city','511800','2026-03-17 12:15:59'),(289,'巴中市',23,'city','511900','2026-03-17 12:15:59'),(290,'资阳市',23,'city','512000','2026-03-17 12:15:59'),(291,'阿坝藏族羌族自治州',23,'city','513200','2026-03-17 12:15:59'),(292,'甘孜藏族自治州',23,'city','513300','2026-03-17 12:15:59'),(293,'凉山彝族自治州',23,'city','513400','2026-03-17 12:15:59'),(294,'贵阳市',24,'city','520100','2026-03-17 12:15:59'),(295,'六盘水市',24,'city','520200','2026-03-17 12:15:59'),(296,'遵义市',24,'city','520300','2026-03-17 12:15:59'),(297,'安顺市',24,'city','520400','2026-03-17 12:15:59'),(298,'毕节市',24,'city','520500','2026-03-17 12:15:59'),(299,'铜仁市',24,'city','520600','2026-03-17 12:15:59'),(300,'黔西南布依族苗族自治州',24,'city','522300','2026-03-17 12:15:59'),(301,'黔东南苗族侗族自治州',24,'city','522600','2026-03-17 12:15:59'),(302,'黔南布依族苗族自治州',24,'city','522700','2026-03-17 12:15:59'),(303,'昆明市',25,'city','530100','2026-03-17 12:15:59'),(304,'曲靖市',25,'city','530300','2026-03-17 12:15:59'),(305,'玉溪市',25,'city','530400','2026-03-17 12:15:59'),(306,'保山市',25,'city','530500','2026-03-17 12:15:59'),(307,'昭通市',25,'city','530600','2026-03-17 12:15:59'),(308,'丽江市',25,'city','530700','2026-03-17 12:15:59'),(309,'普洱市',25,'city','530800','2026-03-17 12:15:59'),(310,'临沧市',25,'city','530900','2026-03-17 12:15:59'),(311,'楚雄彝族自治州',25,'city','532300','2026-03-17 12:15:59'),(312,'红河哈尼族彝族自治州',25,'city','532500','2026-03-17 12:15:59'),(313,'文山壮族苗族自治州',25,'city','532600','2026-03-17 12:15:59'),(314,'西双版纳傣族自治州',25,'city','532800','2026-03-17 12:15:59'),(315,'大理白族自治州',25,'city','532900','2026-03-17 12:15:59'),(316,'德宏傣族景颇族自治州',25,'city','533100','2026-03-17 12:15:59'),(317,'怒江傈僳族自治州',25,'city','533300','2026-03-17 12:15:59'),(318,'迪庆藏族自治州',25,'city','533400','2026-03-17 12:15:59'),(319,'拉萨市',26,'city','540100','2026-03-17 12:15:59'),(320,'日喀则市',26,'city','540200','2026-03-17 12:15:59'),(321,'昌都市',26,'city','540300','2026-03-17 12:15:59'),(322,'林芝市',26,'city','540400','2026-03-17 12:15:59'),(323,'山南市',26,'city','540500','2026-03-17 12:15:59'),(324,'那曲市',26,'city','540600','2026-03-17 12:15:59'),(325,'阿里地区',26,'city','542500','2026-03-17 12:15:59'),(326,'西安市',27,'city','610100','2026-03-17 12:15:59'),(327,'铜川市',27,'city','610200','2026-03-17 12:15:59'),(328,'宝鸡市',27,'city','610300','2026-03-17 12:15:59'),(329,'咸阳市',27,'city','610400','2026-03-17 12:15:59'),(330,'渭南市',27,'city','610500','2026-03-17 12:15:59'),(331,'延安市',27,'city','610600','2026-03-17 12:15:59'),(332,'汉中市',27,'city','610700','2026-03-17 12:15:59'),(333,'榆林市',27,'city','610800','2026-03-17 12:15:59'),(334,'安康市',27,'city','610900','2026-03-17 12:15:59'),(335,'商洛市',27,'city','611000','2026-03-17 12:15:59'),(336,'兰州市',28,'city','620100','2026-03-17 12:15:59'),(337,'嘉峪关市',28,'city','620200','2026-03-17 12:15:59'),(338,'金昌市',28,'city','620300','2026-03-17 12:15:59'),(339,'白银市',28,'city','620400','2026-03-17 12:15:59'),(340,'天水市',28,'city','620500','2026-03-17 12:15:59'),(341,'武威市',28,'city','620600','2026-03-17 12:15:59'),(342,'张掖市',28,'city','620700','2026-03-17 12:15:59'),(343,'平凉市',28,'city','620800','2026-03-17 12:15:59'),(344,'酒泉市',28,'city','620900','2026-03-17 12:15:59'),(345,'庆阳市',28,'city','621000','2026-03-17 12:15:59'),(346,'定西市',28,'city','621100','2026-03-17 12:15:59'),(347,'陇南市',28,'city','621200','2026-03-17 12:15:59'),(348,'临夏回族自治州',28,'city','622900','2026-03-17 12:15:59'),(349,'甘南藏族自治州',28,'city','623000','2026-03-17 12:15:59'),(350,'西宁市',29,'city','630100','2026-03-17 12:15:59'),(351,'海东市',29,'city','630200','2026-03-17 12:15:59'),(352,'海北藏族自治州',29,'city','632200','2026-03-17 12:15:59'),(353,'黄南藏族自治州',29,'city','632300','2026-03-17 12:15:59'),(354,'海南藏族自治州',29,'city','632500','2026-03-17 12:15:59'),(355,'果洛藏族自治州',29,'city','632600','2026-03-17 12:15:59'),(356,'玉树藏族自治州',29,'city','632700','2026-03-17 12:15:59'),(357,'海西蒙古族藏族自治州',29,'city','632800','2026-03-17 12:15:59'),(358,'银川市',30,'city','640100','2026-03-17 12:15:59'),(359,'石嘴山市',30,'city','640200','2026-03-17 12:15:59'),(360,'吴忠市',30,'city','640300','2026-03-17 12:15:59'),(361,'固原市',30,'city','640400','2026-03-17 12:15:59'),(362,'中卫市',30,'city','640500','2026-03-17 12:15:59'),(363,'乌鲁木齐市',31,'city','650100','2026-03-17 12:15:59'),(364,'克拉玛依市',31,'city','650200','2026-03-17 12:15:59'),(365,'吐鲁番市',31,'city','650400','2026-03-17 12:15:59'),(366,'哈密市',31,'city','650500','2026-03-17 12:15:59'),(367,'昌吉回族自治州',31,'city','652300','2026-03-17 12:15:59'),(368,'博尔塔拉蒙古自治州',31,'city','652700','2026-03-17 12:15:59'),(369,'巴音郭楞蒙古自治州',31,'city','652800','2026-03-17 12:15:59'),(370,'阿克苏地区',31,'city','652900','2026-03-17 12:15:59'),(371,'克孜勒苏柯尔克孜自治州',31,'city','653000','2026-03-17 12:15:59'),(372,'喀什地区',31,'city','653100','2026-03-17 12:15:59'),(373,'和田地区',31,'city','653200','2026-03-17 12:15:59'),(374,'伊犁哈萨克自治州',31,'city','654000','2026-03-17 12:15:59'),(375,'塔城地区',31,'city','654200','2026-03-17 12:15:59'),(376,'阿勒泰地区',31,'city','654300','2026-03-17 12:15:59'),(377,'石河子市',31,'city','659001','2026-03-17 12:15:59'),(378,'阿拉尔市',31,'city','659002','2026-03-17 12:15:59'),(379,'图木舒克市',31,'city','659003','2026-03-17 12:15:59'),(380,'五家渠市',31,'city','659004','2026-03-17 12:15:59'),(381,'北屯市',31,'city','659005','2026-03-17 12:15:59'),(382,'铁门关市',31,'city','659006','2026-03-17 12:15:59'),(383,'双河市',31,'city','659007','2026-03-17 12:15:59'),(384,'可克达拉市',31,'city','659008','2026-03-17 12:15:59'),(385,'昆玉市',31,'city','659009','2026-03-17 12:15:59'),(386,'胡杨河市',31,'city','659010','2026-03-17 12:15:59'),(387,'新星市',31,'city','659011','2026-03-17 12:15:59'),(388,'白杨市',31,'city','659012','2026-03-17 12:15:59'),(389,'台北市',32,'city','710100','2026-03-17 12:15:59'),(390,'高雄市',32,'city','710200','2026-03-17 12:15:59'),(391,'基隆市',32,'city','710300','2026-03-17 12:15:59'),(392,'台中市',32,'city','710400','2026-03-17 12:15:59'),(393,'台南市',32,'city','710500','2026-03-17 12:15:59'),(394,'新竹市',32,'city','710600','2026-03-17 12:15:59'),(395,'嘉义市',32,'city','710700','2026-03-17 12:15:59'),(396,'香港特别行政区',33,'city','810100','2026-03-17 12:15:59'),(397,'澳门特别行政区',34,'city','820100','2026-03-17 12:15:59'),(439,'和平区',36,'district','120101','2026-03-17 12:21:47'),(440,'河东区',36,'district','120102','2026-03-17 12:21:47'),(441,'河西区',36,'district','120103','2026-03-17 12:21:47'),(442,'南开区',36,'district','120104','2026-03-17 12:21:47'),(443,'河北区',36,'district','120105','2026-03-17 12:21:47'),(444,'红桥区',36,'district','120106','2026-03-17 12:21:47'),(445,'东丽区',36,'district','120110','2026-03-17 12:21:47'),(446,'西青区',36,'district','120111','2026-03-17 12:21:47'),(447,'津南区',36,'district','120112','2026-03-17 12:21:47'),(448,'北辰区',36,'district','120113','2026-03-17 12:21:47'),(449,'武清区',36,'district','120114','2026-03-17 12:21:47'),(450,'宝坻区',36,'district','120115','2026-03-17 12:21:47'),(451,'滨海新区',36,'district','120116','2026-03-17 12:21:47'),(452,'宁河区',36,'district','120117','2026-03-17 12:21:47'),(453,'静海区',36,'district','120118','2026-03-17 12:21:47'),(454,'蓟州区',36,'district','120119','2026-03-17 12:21:47'),(455,'黄浦区',107,'district','310101','2026-03-17 12:21:47'),(456,'徐汇区',107,'district','310104','2026-03-17 12:21:47'),(457,'长宁区',107,'district','310105','2026-03-17 12:21:47'),(458,'静安区',107,'district','310106','2026-03-17 12:21:47'),(459,'普陀区',107,'district','310107','2026-03-17 12:21:47'),(460,'虹口区',107,'district','310109','2026-03-17 12:21:47'),(461,'杨浦区',107,'district','310110','2026-03-17 12:21:47'),(462,'闵行区',107,'district','310112','2026-03-17 12:21:47'),(463,'宝山区',107,'district','310113','2026-03-17 12:21:47'),(464,'嘉定区',107,'district','310114','2026-03-17 12:21:47'),(465,'浦东新区',107,'district','310115','2026-03-17 12:21:47'),(466,'金山区',107,'district','310116','2026-03-17 12:21:47'),(467,'松江区',107,'district','310117','2026-03-17 12:21:47'),(468,'青浦区',107,'district','310118','2026-03-17 12:21:47'),(469,'奉贤区',107,'district','310120','2026-03-17 12:21:47'),(470,'崇明区',107,'district','310151','2026-03-17 12:21:47'),(471,'万州区',272,'district','500101','2026-03-17 12:21:47'),(472,'涪陵区',272,'district','500102','2026-03-17 12:21:47'),(473,'渝中区',272,'district','500103','2026-03-17 12:21:47'),(474,'大渡口区',272,'district','500104','2026-03-17 12:21:47'),(475,'江北区',272,'district','500105','2026-03-17 12:21:47'),(476,'沙坪坝区',272,'district','500106','2026-03-17 12:21:47'),(477,'九龙坡区',272,'district','500107','2026-03-17 12:21:47'),(478,'南岸区',272,'district','500108','2026-03-17 12:21:47'),(479,'北碚区',272,'district','500109','2026-03-17 12:21:47'),(480,'綦江区',272,'district','500110','2026-03-17 12:21:47'),(481,'大足区',272,'district','500111','2026-03-17 12:21:47'),(482,'渝北区',272,'district','500112','2026-03-17 12:21:47'),(483,'巴南区',272,'district','500113','2026-03-17 12:21:47'),(484,'黔江区',272,'district','500114','2026-03-17 12:21:47'),(485,'长寿区',272,'district','500115','2026-03-17 12:21:47'),(486,'江津区',272,'district','500116','2026-03-17 12:21:47'),(487,'合川区',272,'district','500117','2026-03-17 12:21:47'),(488,'永川区',272,'district','500118','2026-03-17 12:21:47'),(489,'南川区',272,'district','500119','2026-03-17 12:21:47'),(490,'璧山区',272,'district','500120','2026-03-17 12:21:47'),(491,'铜梁区',272,'district','500151','2026-03-17 12:21:47'),(492,'潼南区',272,'district','500152','2026-03-17 12:21:47'),(493,'荣昌区',272,'district','500153','2026-03-17 12:21:47'),(494,'开州区',272,'district','500154','2026-03-17 12:21:47'),(495,'梁平区',272,'district','500155','2026-03-17 12:21:47'),(496,'武隆区',272,'district','500156','2026-03-17 12:21:47'),(497,'玄武区',108,'district','320102','2026-03-17 12:21:47'),(498,'秦淮区',108,'district','320104','2026-03-17 12:21:47'),(499,'建邺区',108,'district','320105','2026-03-17 12:21:47'),(500,'鼓楼区',108,'district','320106','2026-03-17 12:21:47'),(501,'浦口区',108,'district','320111','2026-03-17 12:21:47'),(502,'栖霞区',108,'district','320113','2026-03-17 12:21:47'),(503,'雨花台区',108,'district','320114','2026-03-17 12:21:47'),(504,'江宁区',108,'district','320115','2026-03-17 12:21:47'),(505,'六合区',108,'district','320116','2026-03-17 12:21:47'),(506,'溧水区',108,'district','320117','2026-03-17 12:21:47'),(507,'高淳区',108,'district','320118','2026-03-17 12:21:47'),(508,'虎丘区',112,'district','320505','2026-03-17 12:21:47'),(509,'吴中区',112,'district','320506','2026-03-17 12:21:47'),(510,'相城区',112,'district','320507','2026-03-17 12:21:47'),(511,'姑苏区',112,'district','320508','2026-03-17 12:21:47'),(512,'吴江区',112,'district','320509','2026-03-17 12:21:47'),(513,'常熟市',112,'district','320581','2026-03-17 12:21:47'),(514,'张家港市',112,'district','320582','2026-03-17 12:21:47'),(515,'昆山市',112,'district','320583','2026-03-17 12:21:47'),(516,'太仓市',112,'district','320585','2026-03-17 12:21:47'),(517,'锡山区',109,'district','320205','2026-03-17 12:21:47'),(518,'惠山区',109,'district','320206','2026-03-17 12:21:47'),(519,'滨湖区',109,'district','320211','2026-03-17 12:21:47'),(520,'梁溪区',109,'district','320213','2026-03-17 12:21:47'),(521,'新吴区',109,'district','320214','2026-03-17 12:21:47'),(522,'江阴市',109,'district','320281','2026-03-17 12:21:47'),(523,'宜兴市',109,'district','320282','2026-03-17 12:21:47'),(524,'天宁区',111,'district','320402','2026-03-17 12:21:47'),(525,'钟楼区',111,'district','320404','2026-03-17 12:21:47'),(526,'新北区',111,'district','320411','2026-03-17 12:21:47'),(527,'武进区',111,'district','320412','2026-03-17 12:21:47'),(528,'金坛区',111,'district','320413','2026-03-17 12:21:47'),(529,'溧阳市',111,'district','320481','2026-03-17 12:21:47'),(530,'上城区',121,'district','330102','2026-03-17 12:21:47'),(531,'下城区',121,'district','330103','2026-03-17 12:21:47'),(532,'江干区',121,'district','330104','2026-03-17 12:21:47'),(533,'拱墅区',121,'district','330105','2026-03-17 12:21:47'),(534,'西湖区',121,'district','330106','2026-03-17 12:21:47'),(535,'滨江区',121,'district','330108','2026-03-17 12:21:47'),(536,'萧山区',121,'district','330109','2026-03-17 12:21:47'),(537,'余杭区',121,'district','330110','2026-03-17 12:21:47'),(538,'富阳区',121,'district','330111','2026-03-17 12:21:47'),(539,'临安区',121,'district','330112','2026-03-17 12:21:47'),(540,'桐庐县',121,'district','330122','2026-03-17 12:21:47'),(541,'淳安县',121,'district','330127','2026-03-17 12:21:47'),(542,'建德市',121,'district','330182','2026-03-17 12:21:47'),(543,'海曙区',122,'district','330203','2026-03-17 12:21:47'),(544,'江北区',122,'district','330205','2026-03-17 12:21:47'),(545,'北仑区',122,'district','330206','2026-03-17 12:21:47'),(546,'镇海区',122,'district','330211','2026-03-17 12:21:47'),(547,'鄞州区',122,'district','330212','2026-03-17 12:21:47'),(548,'奉化区',122,'district','330213','2026-03-17 12:21:47'),(549,'象山县',122,'district','330225','2026-03-17 12:21:47'),(550,'宁海县',122,'district','330226','2026-03-17 12:21:47'),(551,'余姚市',122,'district','330281','2026-03-17 12:21:47'),(552,'慈溪市',122,'district','330282','2026-03-17 12:21:47'),(553,'鹿城区',123,'district','330302','2026-03-17 12:21:47'),(554,'龙湾区',123,'district','330303','2026-03-17 12:21:47'),(555,'瓯海区',123,'district','330304','2026-03-17 12:21:47'),(556,'洞头区',123,'district','330305','2026-03-17 12:21:47'),(557,'永嘉县',123,'district','330324','2026-03-17 12:21:47'),(558,'平阳县',123,'district','330326','2026-03-17 12:21:47'),(559,'苍南县',123,'district','330327','2026-03-17 12:21:47'),(560,'文成县',123,'district','330328','2026-03-17 12:21:47'),(561,'泰顺县',123,'district','330329','2026-03-17 12:21:47'),(562,'瑞安市',123,'district','330381','2026-03-17 12:21:47'),(563,'乐清市',123,'district','330382','2026-03-17 12:21:47'),(564,'龙港市',123,'district','330383','2026-03-17 12:21:47'),(565,'荔湾区',233,'district','440103','2026-03-17 12:21:47'),(566,'越秀区',233,'district','440104','2026-03-17 12:21:47'),(567,'海珠区',233,'district','440105','2026-03-17 12:21:47'),(568,'天河区',233,'district','440106','2026-03-17 12:21:47'),(569,'白云区',233,'district','440111','2026-03-17 12:21:47'),(570,'黄埔区',233,'district','440112','2026-03-17 12:21:47'),(571,'番禺区',233,'district','440113','2026-03-17 12:21:47'),(572,'花都区',233,'district','440114','2026-03-17 12:21:47'),(573,'南沙区',233,'district','440115','2026-03-17 12:21:47'),(574,'从化区',233,'district','440117','2026-03-17 12:21:47'),(575,'增城区',233,'district','440118','2026-03-17 12:21:47'),(576,'罗湖区',235,'district','440303','2026-03-17 12:21:48'),(577,'福田区',235,'district','440304','2026-03-17 12:21:48'),(578,'南山区',235,'district','440305','2026-03-17 12:21:48'),(579,'宝安区',235,'district','440306','2026-03-17 12:21:48'),(580,'龙岗区',235,'district','440307','2026-03-17 12:21:48'),(581,'盐田区',235,'district','440308','2026-03-17 12:21:48'),(582,'龙华区',235,'district','440309','2026-03-17 12:21:48'),(583,'坪山区',235,'district','440310','2026-03-17 12:21:48'),(584,'光明区',235,'district','440311','2026-03-17 12:21:48'),(585,'香洲区',236,'district','440402','2026-03-17 12:21:48'),(586,'斗门区',236,'district','440403','2026-03-17 12:21:48'),(587,'金湾区',236,'district','440404','2026-03-17 12:21:48'),(588,'禅城区',238,'district','440604','2026-03-17 12:21:48'),(589,'南海区',238,'district','440605','2026-03-17 12:21:48'),(590,'顺德区',238,'district','440606','2026-03-17 12:21:48'),(591,'三水区',238,'district','440607','2026-03-17 12:21:48'),(592,'高明区',238,'district','440608','2026-03-17 12:21:48'),(593,'惠城区',243,'district','441302','2026-03-17 12:21:48'),(594,'惠阳区',243,'district','441303','2026-03-17 12:21:48'),(595,'博罗县',243,'district','441322','2026-03-17 12:21:48'),(596,'惠东县',243,'district','441323','2026-03-17 12:21:48'),(597,'龙门县',243,'district','441324','2026-03-17 12:21:48'),(598,'莞城街道',249,'district','441900003','2026-03-17 12:21:48'),(599,'南城街道',249,'district','441900004','2026-03-17 12:21:48'),(600,'东城街道',249,'district','441900005','2026-03-17 12:21:48'),(601,'万江街道',249,'district','441900006','2026-03-17 12:21:48'),(602,'石龙镇',249,'district','441900101','2026-03-17 12:21:48'),(603,'虎门镇',249,'district','441900121','2026-03-17 12:21:48'),(604,'石岐街道',250,'district','442000001','2026-03-17 12:21:48'),(605,'东区街道',250,'district','442000002','2026-03-17 12:21:48'),(606,'西区街道',250,'district','442000003','2026-03-17 12:21:48'),(607,'南区街道',250,'district','442000004','2026-03-17 12:21:48'),(608,'五桂山街道',250,'district','442000005','2026-03-17 12:21:48'),(609,'小榄镇',250,'district','442000100','2026-03-17 12:21:48'),(610,'古镇镇',250,'district','442000105','2026-03-17 12:21:48'),(611,'锦江区',273,'district','510104','2026-03-17 12:21:48'),(612,'青羊区',273,'district','510105','2026-03-17 12:21:48'),(613,'金牛区',273,'district','510106','2026-03-17 12:21:48'),(614,'武侯区',273,'district','510107','2026-03-17 12:21:48'),(615,'成华区',273,'district','510108','2026-03-17 12:21:48'),(616,'龙泉驿区',273,'district','510112','2026-03-17 12:21:48'),(617,'青白江区',273,'district','510113','2026-03-17 12:21:48'),(618,'新都区',273,'district','510114','2026-03-17 12:21:48'),(619,'温江区',273,'district','510115','2026-03-17 12:21:48'),(620,'双流区',273,'district','510116','2026-03-17 12:21:48'),(621,'郫都区',273,'district','510117','2026-03-17 12:21:48'),(622,'金堂县',273,'district','510121','2026-03-17 12:21:48'),(623,'大邑县',273,'district','510129','2026-03-17 12:21:48'),(624,'蒲江县',273,'district','510131','2026-03-17 12:21:48'),(625,'新津区',273,'district','510118','2026-03-17 12:21:48'),(626,'都江堰市',273,'district','510181','2026-03-17 12:21:48'),(627,'彭州市',273,'district','510182','2026-03-17 12:21:48'),(628,'邛崃市',273,'district','510183','2026-03-17 12:21:48'),(629,'崇州市',273,'district','510184','2026-03-17 12:21:48'),(630,'简阳市',273,'district','510185','2026-03-17 12:21:48'),(631,'涪城区',278,'district','510703','2026-03-17 12:21:48'),(632,'游仙区',278,'district','510704','2026-03-17 12:21:48'),(633,'安州区',278,'district','510705','2026-03-17 12:21:48'),(634,'三台县',278,'district','510722','2026-03-17 12:21:48'),(635,'盐亭县',278,'district','510723','2026-03-17 12:21:48'),(636,'梓潼县',278,'district','510725','2026-03-17 12:21:48'),(637,'北川羌族自治县',278,'district','510726','2026-03-17 12:21:48'),(638,'平武县',278,'district','510727','2026-03-17 12:21:48'),(639,'江油市',278,'district','510781','2026-03-17 12:21:48'),(640,'江岸区',202,'district','420102','2026-03-17 12:21:48'),(641,'江汉区',202,'district','420103','2026-03-17 12:21:48'),(642,'硚口区',202,'district','420104','2026-03-17 12:21:48'),(643,'汉阳区',202,'district','420105','2026-03-17 12:21:48'),(644,'武昌区',202,'district','420106','2026-03-17 12:21:48'),(645,'青山区',202,'district','420107','2026-03-17 12:21:48'),(646,'洪山区',202,'district','420111','2026-03-17 12:21:48'),(647,'东西湖区',202,'district','420112','2026-03-17 12:21:48'),(648,'汉南区',202,'district','420113','2026-03-17 12:21:48'),(649,'蔡甸区',202,'district','420114','2026-03-17 12:21:48'),(650,'江夏区',202,'district','420115','2026-03-17 12:21:48'),(651,'黄陂区',202,'district','420116','2026-03-17 12:21:48'),(652,'新洲区',202,'district','420117','2026-03-17 12:21:48'),(653,'西陵区',205,'district','420502','2026-03-17 12:21:48'),(654,'伍家岗区',205,'district','420503','2026-03-17 12:21:48'),(655,'点军区',205,'district','420504','2026-03-17 12:21:48'),(656,'猇亭区',205,'district','420505','2026-03-17 12:21:48'),(657,'夷陵区',205,'district','420506','2026-03-17 12:21:48'),(658,'远安县',205,'district','420525','2026-03-17 12:21:48'),(659,'兴山县',205,'district','420526','2026-03-17 12:21:48'),(660,'秭归县',205,'district','420527','2026-03-17 12:21:48'),(661,'长阳土家族自治县',205,'district','420528','2026-03-17 12:21:48'),(662,'五峰土家族自治县',205,'district','420529','2026-03-17 12:21:48'),(663,'宜都市',205,'district','420581','2026-03-17 12:21:48'),(664,'当阳市',205,'district','420582','2026-03-17 12:21:48'),(665,'枝江市',205,'district','420583','2026-03-17 12:21:48'),(666,'芙蓉区',219,'district','430102','2026-03-17 12:21:48'),(667,'天心区',219,'district','430103','2026-03-17 12:21:48'),(668,'岳麓区',219,'district','430104','2026-03-17 12:21:48'),(669,'开福区',219,'district','430105','2026-03-17 12:21:48'),(670,'雨花区',219,'district','430111','2026-03-17 12:21:48'),(671,'望城区',219,'district','430112','2026-03-17 12:21:48'),(672,'长沙县',219,'district','430121','2026-03-17 12:21:48'),(673,'浏阳市',219,'district','430181','2026-03-17 12:21:48'),(674,'宁乡市',219,'district','430182','2026-03-17 12:21:48'),(675,'历下区',168,'district','370102','2026-03-17 12:21:48'),(676,'市中区',168,'district','370103','2026-03-17 12:21:48'),(677,'槐荫区',168,'district','370104','2026-03-17 12:21:48'),(678,'天桥区',168,'district','370105','2026-03-17 12:21:48'),(679,'历城区',168,'district','370112','2026-03-17 12:21:48'),(680,'长清区',168,'district','370113','2026-03-17 12:21:48'),(681,'章丘区',168,'district','370114','2026-03-17 12:21:48'),(682,'济阳区',168,'district','370115','2026-03-17 12:21:48'),(683,'莱芜区',168,'district','370116','2026-03-17 12:21:48'),(684,'钢城区',168,'district','370117','2026-03-17 12:21:48'),(685,'平阴县',168,'district','370124','2026-03-17 12:21:48'),(686,'商河县',168,'district','370126','2026-03-17 12:21:48'),(687,'市南区',169,'district','370202','2026-03-17 12:21:48'),(688,'市北区',169,'district','370203','2026-03-17 12:21:48'),(689,'黄岛区',169,'district','370211','2026-03-17 12:21:48'),(690,'崂山区',169,'district','370212','2026-03-17 12:21:48'),(691,'李沧区',169,'district','370213','2026-03-17 12:21:48'),(692,'城阳区',169,'district','370214','2026-03-17 12:21:48'),(693,'即墨区',169,'district','370215','2026-03-17 12:21:48'),(694,'胶州市',169,'district','370281','2026-03-17 12:21:48'),(695,'平度市',169,'district','370283','2026-03-17 12:21:48'),(696,'莱西市',169,'district','370285','2026-03-17 12:21:48'),(697,'中原区',184,'district','410102','2026-03-17 12:21:48'),(698,'二七区',184,'district','410103','2026-03-17 12:21:48'),(699,'管城回族区',184,'district','410104','2026-03-17 12:21:48'),(700,'金水区',184,'district','410105','2026-03-17 12:21:48'),(701,'上街区',184,'district','410106','2026-03-17 12:21:48'),(702,'惠济区',184,'district','410108','2026-03-17 12:21:48'),(703,'中牟县',184,'district','410122','2026-03-17 12:21:48'),(704,'巩义市',184,'district','410181','2026-03-17 12:21:48'),(705,'荥阳市',184,'district','410182','2026-03-17 12:21:48'),(706,'新密市',184,'district','410183','2026-03-17 12:21:48'),(707,'新郑市',184,'district','410184','2026-03-17 12:21:48'),(708,'登封市',184,'district','410185','2026-03-17 12:21:48'),(709,'老城区',186,'district','410302','2026-03-17 12:21:48'),(710,'西工区',186,'district','410303','2026-03-17 12:21:48'),(711,'瀍河回族区',186,'district','410304','2026-03-17 12:21:48'),(712,'涧西区',186,'district','410305','2026-03-17 12:21:48'),(713,'吉利区',186,'district','410306','2026-03-17 12:21:48'),(714,'洛龙区',186,'district','410311','2026-03-17 12:21:48'),(715,'孟津区',186,'district','410313','2026-03-17 12:21:48'),(716,'新安县',186,'district','410323','2026-03-17 12:21:48'),(717,'栾川县',186,'district','410324','2026-03-17 12:21:48'),(718,'嵩县',186,'district','410325','2026-03-17 12:21:48'),(719,'汝阳县',186,'district','410326','2026-03-17 12:21:48'),(720,'宜阳县',186,'district','410327','2026-03-17 12:21:48'),(721,'洛宁县',186,'district','410328','2026-03-17 12:21:48'),(722,'伊川县',186,'district','410329','2026-03-17 12:21:48'),(723,'偃师区',186,'district','410381','2026-03-17 12:21:48'),(724,'新城区',326,'district','610102','2026-03-17 12:21:48'),(725,'碑林区',326,'district','610103','2026-03-17 12:21:48'),(726,'莲湖区',326,'district','610104','2026-03-17 12:21:48'),(727,'灞桥区',326,'district','610111','2026-03-17 12:21:48'),(728,'未央区',326,'district','610112','2026-03-17 12:21:48'),(729,'雁塔区',326,'district','610113','2026-03-17 12:21:48'),(730,'阎良区',326,'district','610114','2026-03-17 12:21:48'),(731,'临潼区',326,'district','610115','2026-03-17 12:21:48'),(732,'长安区',326,'district','610116','2026-03-17 12:21:48'),(733,'高陵区',326,'district','610117','2026-03-17 12:21:48'),(734,'鄠邑区',326,'district','610118','2026-03-17 12:21:48'),(735,'蓝田县',326,'district','610122','2026-03-17 12:21:48'),(736,'周至县',326,'district','610124','2026-03-17 12:21:48'),(737,'鼓楼区',148,'district','350102','2026-03-17 12:21:48'),(738,'台江区',148,'district','350103','2026-03-17 12:21:48'),(739,'仓山区',148,'district','350104','2026-03-17 12:21:48'),(740,'马尾区',148,'district','350105','2026-03-17 12:21:48'),(741,'晋安区',148,'district','350111','2026-03-17 12:21:48'),(742,'长乐区',148,'district','350112','2026-03-17 12:21:48'),(743,'闽侯县',148,'district','350121','2026-03-17 12:21:48'),(744,'连江县',148,'district','350122','2026-03-17 12:21:48'),(745,'罗源县',148,'district','350123','2026-03-17 12:21:48'),(746,'闽清县',148,'district','350124','2026-03-17 12:21:48'),(747,'永泰县',148,'district','350125','2026-03-17 12:21:48'),(748,'平潭县',148,'district','350128','2026-03-17 12:21:48'),(749,'福清市',148,'district','350181','2026-03-17 12:21:48'),(750,'瑶海区',132,'district','340102','2026-03-17 12:21:48'),(751,'庐阳区',132,'district','340103','2026-03-17 12:21:48'),(752,'蜀山区',132,'district','340104','2026-03-17 12:21:48'),(753,'包河区',132,'district','340111','2026-03-17 12:21:48'),(754,'长丰县',132,'district','340121','2026-03-17 12:21:48'),(755,'肥东县',132,'district','340122','2026-03-17 12:21:48'),(756,'肥西县',132,'district','340123','2026-03-17 12:21:48'),(757,'庐江县',132,'district','340124','2026-03-17 12:21:48'),(758,'巢湖市',132,'district','340181','2026-03-17 12:21:48'),(759,'东湖区',157,'district','360102','2026-03-17 12:21:48'),(760,'西湖区',157,'district','360103','2026-03-17 12:21:48'),(761,'青云谱区',157,'district','360104','2026-03-17 12:21:48'),(762,'青山湖区',157,'district','360111','2026-03-17 12:21:48'),(763,'新建区',157,'district','360112','2026-03-17 12:21:48'),(764,'红谷滩区',157,'district','360113','2026-03-17 12:21:48'),(765,'南昌县',157,'district','360121','2026-03-17 12:21:48'),(766,'安义县',157,'district','360123','2026-03-17 12:21:48'),(767,'进贤县',157,'district','360124','2026-03-17 12:21:48'),(768,'兴宁区',254,'district','450102','2026-03-17 12:21:48'),(769,'青秀区',254,'district','450103','2026-03-17 12:21:48'),(770,'江南区',254,'district','450105','2026-03-17 12:21:48'),(771,'西乡塘区',254,'district','450107','2026-03-17 12:21:48'),(772,'良庆区',254,'district','450108','2026-03-17 12:21:48'),(773,'邕宁区',254,'district','450109','2026-03-17 12:21:48'),(774,'武鸣区',254,'district','450110','2026-03-17 12:21:48'),(775,'隆安县',254,'district','450123','2026-03-17 12:21:48'),(776,'马山县',254,'district','450124','2026-03-17 12:21:48'),(777,'上林县',254,'district','450125','2026-03-17 12:21:48'),(778,'宾阳县',254,'district','450126','2026-03-17 12:21:48'),(779,'横州市',254,'district','450181','2026-03-17 12:21:48'),(780,'五华区',303,'district','530102','2026-03-17 12:21:48'),(781,'盘龙区',303,'district','530103','2026-03-17 12:21:48'),(782,'官渡区',303,'district','530111','2026-03-17 12:21:48'),(783,'西山区',303,'district','530112','2026-03-17 12:21:48'),(784,'东川区',303,'district','530113','2026-03-17 12:21:48'),(785,'呈贡区',303,'district','530114','2026-03-17 12:21:48'),(786,'晋宁区',303,'district','530115','2026-03-17 12:21:48'),(787,'富民县',303,'district','530124','2026-03-17 12:21:48'),(788,'宜良县',303,'district','530125','2026-03-17 12:21:48'),(789,'石林彝族自治县',303,'district','530126','2026-03-17 12:21:48'),(790,'嵩明县',303,'district','530127','2026-03-17 12:21:48'),(791,'禄劝彝族苗族自治县',303,'district','530128','2026-03-17 12:21:48'),(792,'寻甸回族彝族自治县',303,'district','530129','2026-03-17 12:21:48'),(793,'安宁市',303,'district','530181','2026-03-17 12:21:48'),(794,'南明区',294,'district','520102','2026-03-17 12:21:48'),(795,'云岩区',294,'district','520103','2026-03-17 12:21:48'),(796,'花溪区',294,'district','520111','2026-03-17 12:21:48'),(797,'乌当区',294,'district','520112','2026-03-17 12:21:48'),(798,'白云区',294,'district','520113','2026-03-17 12:21:48'),(799,'观山湖区',294,'district','520115','2026-03-17 12:21:48'),(800,'开阳县',294,'district','520121','2026-03-17 12:21:48'),(801,'息烽县',294,'district','520122','2026-03-17 12:21:48'),(802,'修文县',294,'district','520123','2026-03-17 12:21:48'),(803,'清镇市',294,'district','520181','2026-03-17 12:21:48');
/*!40000 ALTER TABLE `region` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `report`
--

DROP TABLE IF EXISTS `report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '举报ID，主键',
  `reporter_id` bigint NOT NULL COMMENT '举报人ID，关联user_info表',
  `target_type` enum('blog','project','comment','user','circle') NOT NULL COMMENT '被举报的目标类型：blog-博客, project-项目, comment-评论, user-用户, circle-圈子',
  `target_id` bigint NOT NULL COMMENT '被举报目标的ID',
  `reason` varchar(500) NOT NULL COMMENT '举报原因描述',
  `status` enum('pending','processed','ignored') DEFAULT 'pending' COMMENT '处理状态：pending-待处理, processed-已处理, ignored-已忽略',
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `report`
--

LOCK TABLES `report` WRITE;
/*!40000 ALTER TABLE `report` DISABLE KEYS */;
/*!40000 ALTER TABLE `report` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role`
--

DROP TABLE IF EXISTS `role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '角色ID，主键',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称，例如：管理员、审核员、普通用户',
  `description` text COMMENT '角色的详细描述',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '角色创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '角色信息最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `role_name` (`role_name`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role`
--

LOCK TABLES `role` WRITE;
/*!40000 ALTER TABLE `role` DISABLE KEYS */;
INSERT INTO `role` VALUES (1,'超级管理员','管理后台','2026-03-10 12:17:38','2026-03-10 12:17:38'),(2,'管理员','后台(管理)','2026-03-10 12:17:38','2026-03-10 12:17:38'),(3,'审查员','后台(审查内容)','2026-03-10 12:17:38','2026-03-10 12:17:38'),(4,'用户','客户端','2026-03-10 12:17:38','2026-03-10 12:17:38');
/*!40000 ALTER TABLE `role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role_permission`
--

DROP TABLE IF EXISTS `role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_permission` (
  `role_id` int NOT NULL COMMENT '角色ID',
  `menu_id` int NOT NULL,
  PRIMARY KEY (`role_id`,`menu_id`),
  KEY `permission_id` (`menu_id`),
  CONSTRAINT `fk_role_menu_menu_id` FOREIGN KEY (`menu_id`) REFERENCES `menu` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `role_permission_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色与菜单的多对多关联表，用于控制角色可以访问的菜单项';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role_permission`
--

LOCK TABLES `role_permission` WRITE;
/*!40000 ALTER TABLE `role_permission` DISABLE KEYS */;
INSERT INTO `role_permission` VALUES (1,1),(2,1),(3,1),(1,2),(2,2),(1,3),(2,3),(3,3),(1,4),(2,4),(3,4),(4,5),(4,6),(4,7),(4,8),(4,9),(4,10),(4,11),(1,15),(2,15),(1,16),(2,16),(1,17),(2,17),(1,18),(2,18),(1,19),(2,19),(1,20),(2,20),(1,21),(2,21),(1,22),(2,22),(1,23),(2,23),(3,23),(1,24),(2,24),(1,25),(2,25),(3,25),(1,26),(2,26),(1,27),(2,27),(3,27),(1,28),(2,28),(3,28),(1,29),(2,29),(1,30),(2,30),(1,31),(2,31),(1,32),(2,32),(3,32),(1,33),(2,33),(3,33),(1,34),(2,34),(3,34),(1,35),(2,35),(1,36),(2,36),(1,37),(2,37),(1,38),(2,38),(3,38),(1,39),(2,39),(3,39),(1,40),(2,40),(1,41),(2,41),(1,42),(2,42),(3,42),(1,43),(2,43),(3,43),(1,44),(2,44),(3,44),(1,45),(2,45),(1,46),(2,46),(3,46),(1,47),(2,47),(3,47),(1,48),(2,48),(3,48),(1,49),(2,49),(3,49),(1,50),(2,50),(1,51),(2,51),(1,52),(2,52),(1,53),(2,53),(1,54),(2,54),(1,55),(2,55),(1,56),(2,56),(1,57),(2,57),(3,57),(1,58),(2,58),(3,58),(1,59),(2,59),(1,60),(2,60),(1,61),(2,61),(1,62),(2,62),(1,63),(2,63),(1,64),(2,64),(1,65),(2,65),(1,66),(2,66),(1,67),(2,67),(1,68),(2,68),(1,69),(2,69),(1,70),(2,70),(1,71),(2,71),(1,72),(2,72),(1,73),(2,73),(1,74),(2,74),(1,75),(2,75),(1,76),(2,76),(1,77),(2,77),(1,78),(2,78),(1,79),(2,79),(1,80),(2,80),(1,81),(2,81),(1,82),(2,82),(1,83),(2,83),(1,84),(2,84),(1,85),(2,85),(1,86),(1,87),(2,87),(1,88),(2,88),(1,89),(1,90),(1,91),(1,92),(1,93),(2,93),(1,94),(2,94),(1,95),(1,96),(1,97),(1,98),(1,99),(1,100),(1,101),(2,101),(1,102),(1,103),(1,104),(2,104),(1,105),(2,105),(1,106),(2,106),(1,107),(2,107),(1,108),(2,108),(1,109),(2,109),(1,110),(2,110),(1,111),(2,111),(1,112),(2,112),(1,113),(2,113),(1,114),(2,114),(1,115),(2,115),(1,116),(2,116),(1,117),(2,117),(1,118),(2,118),(3,118),(1,119),(2,119),(3,119),(1,120),(2,120),(1,121),(1,122),(1,123),(1,124),(1,125),(1,126),(1,127),(1,128),(1,129),(1,130),(1,131),(1,132),(1,133),(1,134),(1,135),(1,136),(1,137),(1,138),(1,139),(1,140),(1,141),(1,142),(1,143),(1,144),(1,145),(1,146),(1,147),(1,148),(1,149),(1,150),(1,151),(1,152),(1,153),(1,154),(4,155),(4,156),(4,157),(4,158),(4,159),(4,160),(4,161),(4,162),(4,163),(4,164),(4,165),(4,166),(4,167),(4,168),(4,169),(4,170),(4,171),(4,172),(4,173),(4,174),(4,175),(4,176),(4,177),(4,178),(4,179),(4,180),(4,181),(4,182),(4,183),(4,184),(4,185),(4,186),(4,187),(4,188),(4,189),(4,190),(4,191),(4,192),(4,193),(4,194),(4,195),(4,196),(4,197),(4,198),(4,199),(4,200),(4,201),(4,202),(4,203),(4,204),(4,205),(4,206),(4,207),(4,208),(4,209),(4,210),(4,211),(4,212),(4,213),(4,214),(4,215),(4,216),(4,217),(4,218),(4,219),(4,220),(4,221),(4,222),(4,223),(4,224),(4,225),(4,226),(4,227),(4,228),(4,229),(4,230),(4,231),(4,232),(4,233),(4,234),(4,235),(4,236),(4,237),(4,238),(4,239),(4,240),(4,241),(4,242),(4,243),(4,244),(4,245),(4,246),(4,247),(4,248),(4,249),(4,250),(4,251),(4,252),(4,253),(4,254),(4,255),(4,256),(4,257),(4,258),(4,259),(4,260),(4,261),(4,262),(4,263),(4,264),(4,265),(4,266),(4,267),(4,268),(4,269),(4,270),(4,271),(4,272),(4,273),(4,274),(4,275),(4,276),(4,277),(4,278),(4,279),(4,280),(4,281),(4,282),(4,283),(4,284),(4,285);
/*!40000 ALTER TABLE `role_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `search_index`
--

DROP TABLE IF EXISTS `search_index`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `search_index` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '索引ID，主键',
  `content` text COMMENT '用于搜索的文本内容',
  `doc_type` enum('blog','project','user') NOT NULL COMMENT '文档类型：blog-博客, project-项目, user-用户',
  `doc_id` bigint NOT NULL COMMENT '对应文档的ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '索引创建时间',
  PRIMARY KEY (`id`),
  FULLTEXT KEY `idx_content` (`content`) COMMENT '为内容字段创建全文索引，提升搜索性能'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='全文搜索引擎的索引表，用于加速内容检索';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `search_index`
--

LOCK TABLES `search_index` WRITE;
/*!40000 ALTER TABLE `search_index` DISABLE KEYS */;
/*!40000 ALTER TABLE `search_index` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tag`
--

DROP TABLE IF EXISTS `tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID，主键',
  `name` varchar(50) NOT NULL COMMENT '标签名称，例如：Java, Spring Boot, Web开发',
  `parent_id` bigint DEFAULT NULL COMMENT '父级标签ID，用于构建技术分类树，顶级分类为NULL',
  `category` enum('tech','language','framework','tool','other') DEFAULT 'tech' COMMENT '标签大类，方便顶层分类',
  `description` text COMMENT '标签的详细描述',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标签创建时间',
  PRIMARY KEY (`id`),
  KEY `parent_id` (`parent_id`),
  CONSTRAINT `tag_ibfk_1` FOREIGN KEY (`parent_id`) REFERENCES `tag` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标签信息表，支持树状层级结构，用于对博客和项目进行分类';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tag`
--

LOCK TABLES `tag` WRITE;
/*!40000 ALTER TABLE `tag` DISABLE KEYS */;
INSERT INTO `tag` VALUES (1,'编程语言',NULL,'language','各类编程语言','2026-03-10 14:41:34'),(2,'Java',1,'language','面向对象的编程语言','2026-03-10 14:41:34'),(3,'Python',1,'language','解释型高级语言','2026-03-10 14:41:34'),(4,'JavaScript',1,'language','前端/后端脚本语言','2026-03-10 14:41:34'),(5,'框架',NULL,'framework','开发框架集合','2026-03-10 14:41:34'),(6,'Spring Boot',5,'framework','Java 微服务框架','2026-03-10 14:41:34'),(7,'Django',5,'framework','Python Web 框架','2026-03-10 14:41:34'),(8,'React',5,'framework','前端 UI 库','2026-03-10 14:41:34'),(9,'工具',NULL,'tool','开发工具类','2026-03-10 14:41:34'),(10,'Git',9,'tool','分布式版本控制','2026-03-10 14:41:34'),(11,'Docker',9,'tool','容器化平台','2026-03-10 14:41:34'),(12,'Maven',9,'tool','Java 项目构建工具','2026-03-10 14:41:34');
/*!40000 ALTER TABLE `tag` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_behavior`
--

DROP TABLE IF EXISTS `user_behavior`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_behavior` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '行为记录ID，主键',
  `user_id` bigint NOT NULL COMMENT '用户ID，关联user_info表',
  `behavior_type` enum('login','logout','view','like','collect','comment','share','upload','download') NOT NULL COMMENT '行为类型',
  `target_type` enum('blog','project','comment','circle','user') DEFAULT NULL COMMENT '行为对象类型',
  `target_id` bigint DEFAULT NULL COMMENT '行为对象ID',
  `extra_data` json DEFAULT NULL COMMENT '额外的行为数据，如搜索关键词、操作参数等',
  `ip_address` varchar(45) DEFAULT NULL COMMENT '用户IP地址',
  `user_agent` text COMMENT '用户代理字符串',
  `occurred_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '行为发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_behavior` (`user_id`,`behavior_type`),
  KEY `idx_occurred_at` (`occurred_at` DESC),
  CONSTRAINT `user_behavior_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户行为详细记录表，用于分析和推荐';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_behavior`
--

LOCK TABLES `user_behavior` WRITE;
/*!40000 ALTER TABLE `user_behavior` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_behavior` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_info`
--

DROP TABLE IF EXISTS `user_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户基本信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_info`
--

LOCK TABLES `user_info` WRITE;
/*!40000 ALTER TABLE `user_info` DISABLE KEYS */;
INSERT INTO `user_info` VALUES (1,'szw','$2a$10$0.KFlSPxVrSvykbOuVz2seKOkdDDixSFZzb2st2rqmQ/KPoMQH7Kq','2159997489@qq.com',NULL,NULL,NULL,NULL,4,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(3,'admin','$2a$10$idZC095FXpQ0LAKiboVfPOD35/Fa.uWsRvRiZlILI9g2vS9xWTSYa','1036144175@qq.com','',NULL,'active','',1,NULL,NULL,NULL,0,'','','',NULL,'',NULL),(5,'test-2','$2a$10$JJCD0eHmAaTaBkNhh4juuu1WA.NypYzMfMfa9csnbvkKNahKqpJ1e','2@qq.com','',NULL,'active','1231231231',2,NULL,NULL,NULL,0,'','','',NULL,'',NULL),(6,'test-3','$2a$10$i.Xci2phOzcq6LgLI54WUuacAoXBq3ezCW8Kmkxy5f8leD84pISCS','3@qq.com','',NULL,'active','1111111',3,NULL,NULL,NULL,0,'','','',NULL,'',NULL);
/*!40000 ALTER TABLE `user_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_skill`
--

DROP TABLE IF EXISTS `user_skill`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_skill` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID，主键',
  `user_id` bigint NOT NULL COMMENT '用户ID，关联user_info表',
  `tag_id` bigint NOT NULL COMMENT '技能标签ID，关联tag表',
  `proficiency_level` enum('beginner','intermediate','advanced','expert') DEFAULT 'intermediate' COMMENT '熟练程度',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关联创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_tag` (`user_id`,`tag_id`) COMMENT '确保一个用户与一个技能标签只有一条记录',
  KEY `tag_id` (`tag_id`),
  CONSTRAINT `user_skill_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_skill_ibfk_2` FOREIGN KEY (`tag_id`) REFERENCES `tag` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户技能标签关联表，用于展示用户擅长的技术';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_skill`
--

LOCK TABLES `user_skill` WRITE;
/*!40000 ALTER TABLE `user_skill` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_skill` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `view_log`
--

DROP TABLE IF EXISTS `view_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `view_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '浏览记录ID，主键',
  `user_id` bigint DEFAULT NULL COMMENT '浏览者ID，关联user_info表，匿名浏览则为NULL',
  `target_type` enum('blog','project') NOT NULL COMMENT '被浏览的目标类型：blog-博客, project-项目',
  `target_id` bigint NOT NULL COMMENT '被浏览目标的ID',
  `ip_address` varchar(45) DEFAULT NULL COMMENT '浏览者IP地址，用于统计和安全分析',
  `user_agent` text COMMENT '用户代理字符串，记录浏览器等客户端信息',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_target` (`user_id`,`target_type`,`target_id`),
  KEY `idx_created_at` (`created_at` DESC),
  CONSTRAINT `view_log_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内容浏览日志表，用于统计和数据分析';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `view_log`
--

LOCK TABLES `view_log` WRITE;
/*!40000 ALTER TABLE `view_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `view_log` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-22 15:42:08
