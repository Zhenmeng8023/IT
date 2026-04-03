-- MySQL dump 10.13  Distrib 8.0.39, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: it9_data
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
) ENGINE=InnoDB AUTO_INCREMENT=125 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='前端菜单及权限配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `menu`
--

LOCK TABLES `menu` WRITE;
/*!40000 ALTER TABLE `menu` DISABLE KEYS */;
INSERT INTO `menu` VALUES (1,'首页',NULL,'/homepage','/','el-icon-s-home',0,0,18,'2026-03-16 13:44:12'),(2,'系统管理',NULL,'/','/','el-icon-s-tools',0,0,19,'2026-03-16 14:05:51'),(3,'博客管理',NULL,'/','/','el-icon-notebook-2',0,0,24,'2026-03-16 14:25:36'),(4,'圈子管理',NULL,'/','/','el-icon-s-promotion',0,0,25,'2026-03-16 14:29:36'),(5,'网页主页',NULL,'/webhome','/','',0,0,26,'2026-03-16 14:39:54'),(6,'博客页面',NULL,'/blog','/','',0,0,14,'2026-03-16 14:42:14'),(7,'博客编辑',NULL,'/blogwrite','/','',0,0,27,'2026-03-16 14:43:27'),(8,'写博客',NULL,'/blogwrite','/','',0,0,27,'2026-03-16 14:45:37'),(9,'圈子首页',NULL,'/circle','/','',0,0,15,'2026-03-16 14:50:27'),(10,'圈子详情',NULL,'/circle/:id','/','',0,0,28,'2026-03-16 14:52:08'),(11,'个人首页',NULL,'/user','/','',0,0,17,'2026-03-16 14:52:54'),(12,'收藏页面',11,'/collection','/','',0,0,16,'2026-03-16 14:54:08'),(13,'登录页',NULL,'/login','/','',0,0,29,'2026-03-16 14:56:43'),(14,'注册页',NULL,'/registe','/','',0,0,30,'2026-03-16 14:57:10'),(15,'用户管理',2,'/','/','el-icon-user',1,0,20,'2026-03-16 14:12:04'),(16,'角色管理',2,'/role','/','el-icon-user',1,0,12,'2026-03-16 14:16:53'),(17,'菜单管理',2,'/menu','/','el-icon-menu',1,0,22,'2026-03-16 14:18:43'),(18,'权限管理',2,'/permission','/','el-icon-s-fold',1,0,23,'2026-03-16 14:20:03'),(19,'日志管理',2,'/log','/','el-icon-s-management',1,0,10,'2026-03-16 14:22:01'),(20,'标签管理',2,'/label','/','el-icon-collection-tag',1,0,13,'2026-03-16 14:23:42'),(21,'账户管理',15,'/count','/','el-icon-s-custom',2,0,21,'2026-03-16 14:14:22'),(22,'用户信息管理',15,'/info','/','el-icon-document',2,0,11,'2026-03-16 14:16:09'),(23,'审核',3,'/audit','/','el-icon-check',1,0,2,'2026-03-16 14:27:22'),(24,'仪表盘',3,'/dashboard','/','el-icon-data-analysis',1,0,3,'2026-03-16 14:28:36'),(25,'圈子审核',4,'/circleaudit','/','el-icon-check',1,0,4,'2026-03-16 14:31:24'),(26,'圈子管理',4,'/circlemanage','/','el-icon-s-management',1,0,5,'2026-03-16 14:33:13'),(27,'批量通过博客审核',23,NULL,NULL,NULL,0,0,31,'2026-03-21 11:52:20'),(28,'批量拒绝博客审核',23,NULL,NULL,NULL,0,0,32,'2026-03-21 11:52:20'),(29,'批量置顶博客',23,NULL,NULL,NULL,0,0,33,'2026-03-21 11:52:20'),(30,'批量取消置顶博客',23,NULL,NULL,NULL,0,0,34,'2026-03-21 11:52:20'),(31,'导出博客数据',23,NULL,NULL,NULL,0,0,35,'2026-03-21 11:52:20'),(32,'查看博客详情',23,NULL,NULL,NULL,0,0,36,'2026-03-21 11:52:20'),(33,'单个审核通过',23,NULL,NULL,NULL,0,0,37,'2026-03-21 11:52:20'),(34,'单个审核拒绝',23,NULL,NULL,NULL,0,0,38,'2026-03-21 11:52:20'),(35,'置顶博客',23,NULL,NULL,NULL,0,0,39,'2026-03-21 11:52:20'),(36,'取消置顶博客',23,NULL,NULL,NULL,0,0,40,'2026-03-21 11:52:20'),(37,'删除博客',23,NULL,NULL,NULL,0,0,41,'2026-03-21 11:52:20'),(38,'查看已下架博客',23,NULL,NULL,NULL,0,0,42,'2026-03-21 11:52:20'),(39,'恢复已下架博客',23,NULL,NULL,NULL,0,0,43,'2026-03-21 11:52:20'),(40,'刷新热门博客数据',24,NULL,NULL,NULL,0,0,44,'2026-03-21 11:52:20'),(41,'刷新系统状态',24,NULL,NULL,NULL,0,0,45,'2026-03-21 11:52:20'),(42,'批量通过圈子审核',25,NULL,NULL,NULL,0,0,46,'2026-03-21 11:52:20'),(43,'批量关闭圈子',25,NULL,NULL,NULL,0,0,47,'2026-03-21 11:52:20'),(44,'批量删除圈子',25,NULL,NULL,NULL,0,0,48,'2026-03-21 11:52:20'),(45,'导出圈子数据',25,NULL,NULL,NULL,0,0,49,'2026-03-21 11:52:20'),(46,'查看圈子详情',25,NULL,NULL,NULL,0,0,50,'2026-03-21 11:52:20'),(47,'通过圈子审核',25,NULL,NULL,NULL,0,0,51,'2026-03-21 11:52:20'),(48,'推荐圈子',25,NULL,NULL,NULL,0,0,52,'2026-03-21 11:52:20'),(49,'取消推荐圈子',25,NULL,NULL,NULL,0,0,53,'2026-03-21 11:52:20'),(50,'关闭圈子',25,NULL,NULL,NULL,0,0,54,'2026-03-21 11:52:20'),(51,'编辑圈子',25,NULL,NULL,NULL,0,0,55,'2026-03-21 11:52:20'),(52,'删除圈子',25,NULL,NULL,NULL,0,0,56,'2026-03-21 11:52:20'),(53,'圈子成员管理',25,NULL,NULL,NULL,0,0,57,'2026-03-21 11:52:20'),(54,'设为管理员',25,NULL,NULL,NULL,0,0,58,'2026-03-21 11:52:20'),(55,'移除成员',25,NULL,NULL,NULL,0,0,59,'2026-03-21 11:52:20'),(56,'圈子帖子管理',25,NULL,NULL,NULL,0,0,60,'2026-03-21 11:52:20'),(57,'通过帖子审核',25,NULL,NULL,NULL,0,0,61,'2026-03-21 11:52:20'),(58,'删除帖子',25,NULL,NULL,NULL,0,0,62,'2026-03-21 11:52:20'),(59,'创建圈子',26,NULL,NULL,NULL,0,0,63,'2026-03-21 11:52:20'),(60,'刷新圈子列表',26,NULL,NULL,NULL,0,0,64,'2026-03-21 11:52:20'),(61,'导出圈子数据',26,NULL,NULL,NULL,0,0,65,'2026-03-21 11:52:20'),(62,'编辑圈子信息',26,NULL,NULL,NULL,0,0,66,'2026-03-21 11:52:20'),(63,'删除圈子',26,NULL,NULL,NULL,0,0,67,'2026-03-21 11:52:20'),(64,'批量删除帖子',26,NULL,NULL,NULL,0,0,68,'2026-03-21 11:52:20'),(65,'添加分类',26,NULL,NULL,NULL,0,0,69,'2026-03-21 11:52:20'),(66,'排序分类',26,NULL,NULL,NULL,0,0,70,'2026-03-21 11:52:20'),(67,'编辑分类',26,NULL,NULL,NULL,0,0,71,'2026-03-21 11:52:20'),(68,'设为热门分类',26,NULL,NULL,NULL,0,0,72,'2026-03-21 11:52:20'),(69,'取消热门分类',26,NULL,NULL,NULL,0,0,73,'2026-03-21 11:52:20'),(70,'禁用分类',26,NULL,NULL,NULL,0,0,74,'2026-03-21 11:52:20'),(71,'启用分类',26,NULL,NULL,NULL,0,0,75,'2026-03-21 11:52:20'),(72,'删除分类',26,NULL,NULL,NULL,0,0,76,'2026-03-21 11:52:20'),(73,'新增用户',21,NULL,NULL,NULL,0,0,91,'2026-03-21 11:52:20'),(74,'查询用户',21,NULL,NULL,NULL,0,0,92,'2026-03-21 11:52:20'),(75,'重置搜索',21,NULL,NULL,NULL,0,0,93,'2026-03-21 11:52:20'),(76,'编辑用户',21,NULL,NULL,NULL,0,0,94,'2026-03-21 11:52:20'),(77,'重置密码',21,NULL,NULL,NULL,0,0,95,'2026-03-21 11:52:20'),(78,'禁用账户',21,NULL,NULL,NULL,0,0,96,'2026-03-21 11:52:20'),(79,'启用账户',21,NULL,NULL,NULL,0,0,97,'2026-03-21 11:52:20'),(80,'删除账户',21,NULL,NULL,NULL,0,0,98,'2026-03-21 11:52:20'),(81,'查询用户信息',22,NULL,NULL,NULL,0,0,99,'2026-03-21 11:52:20'),(82,'重置搜索',22,NULL,NULL,NULL,0,0,100,'2026-03-21 11:52:20'),(83,'查看用户详情',22,NULL,NULL,NULL,0,0,101,'2026-03-21 11:52:20'),(84,'编辑用户信息',22,NULL,NULL,NULL,0,0,102,'2026-03-21 11:52:20'),(85,'保存用户信息',22,NULL,NULL,NULL,0,0,103,'2026-03-21 11:52:20'),(86,'新增角色',16,NULL,NULL,NULL,0,0,104,'2026-03-21 11:52:20'),(87,'刷新角色列表',16,NULL,NULL,NULL,0,0,105,'2026-03-21 11:52:20'),(88,'搜索角色',16,NULL,NULL,NULL,0,0,106,'2026-03-21 11:52:20'),(89,'编辑角色',16,NULL,NULL,NULL,0,0,107,'2026-03-21 11:52:20'),(90,'权限配置',16,NULL,NULL,NULL,0,0,108,'2026-03-21 11:52:20'),(91,'删除角色',16,NULL,NULL,NULL,0,0,109,'2026-03-21 11:52:20'),(92,'新增菜单',17,NULL,NULL,NULL,0,0,110,'2026-03-21 11:52:20'),(93,'刷新菜单列表',17,NULL,NULL,NULL,0,0,111,'2026-03-21 11:52:20'),(94,'搜索菜单',17,NULL,NULL,NULL,0,0,112,'2026-03-21 11:52:20'),(95,'编辑菜单',17,NULL,NULL,NULL,0,0,113,'2026-03-21 11:52:20'),(96,'添加子菜单',17,NULL,NULL,NULL,0,0,114,'2026-03-21 11:52:20'),(97,'删除菜单',17,NULL,NULL,NULL,0,0,115,'2026-03-21 11:52:20'),(98,'调整排序',17,NULL,NULL,NULL,0,0,116,'2026-03-21 11:52:20'),(99,'切换菜单状态',17,NULL,NULL,NULL,0,0,117,'2026-03-21 11:52:20'),(100,'新增权限',18,NULL,NULL,NULL,0,0,118,'2026-03-21 11:52:20'),(101,'搜索权限',18,NULL,NULL,NULL,0,0,119,'2026-03-21 11:52:20'),(102,'编辑权限',18,NULL,NULL,NULL,0,0,120,'2026-03-21 11:52:20'),(103,'删除权限',18,NULL,NULL,NULL,0,0,121,'2026-03-21 11:52:20'),(104,'查询日志',19,NULL,NULL,NULL,0,0,122,'2026-03-21 11:52:20'),(105,'重置搜索',19,NULL,NULL,NULL,0,0,123,'2026-03-21 11:52:20'),(106,'导出日志',19,NULL,NULL,NULL,0,0,124,'2026-03-21 11:52:20'),(107,'查看日志详情',19,NULL,NULL,NULL,0,0,125,'2026-03-21 11:52:20'),(108,'新增标签',20,NULL,NULL,NULL,0,0,126,'2026-03-21 11:52:20'),(109,'新增分类',20,NULL,NULL,NULL,0,0,127,'2026-03-21 11:52:20'),(110,'搜索标签',20,NULL,NULL,NULL,0,0,128,'2026-03-21 11:52:20'),(111,'查看标签下圈子',20,NULL,NULL,NULL,0,0,129,'2026-03-21 11:52:20'),(112,'编辑标签',20,NULL,NULL,NULL,0,0,130,'2026-03-21 11:52:20'),(113,'设为热门标签',20,NULL,NULL,NULL,0,0,131,'2026-03-21 11:52:20'),(114,'取消热门标签',20,NULL,NULL,NULL,0,0,132,'2026-03-21 11:52:20'),(115,'禁用标签',20,NULL,NULL,NULL,0,0,133,'2026-03-21 11:52:20'),(116,'启用标签',20,NULL,NULL,NULL,0,0,134,'2026-03-21 11:52:20'),(117,'删除标签',20,NULL,NULL,NULL,0,0,135,'2026-03-21 11:52:20'),(118,'查看推荐结果',1,NULL,NULL,NULL,0,0,152,'2026-03-21 11:52:20'),(119,'刷新推荐',1,NULL,NULL,NULL,0,0,153,'2026-03-21 11:52:20'),(120,'导出推荐数据',1,NULL,NULL,NULL,0,0,154,'2026-03-21 11:52:20'),(121,'知识库',NULL,'/knowledge-base','/','',0,0,155,'2026-03-29 02:03:10'),(122,'AI日志',NULL,'/ai/logs','/','',0,0,158,'2026-03-29 02:03:38'),(123,'AI模型管理',NULL,'/ai/models','/','',0,0,156,'2026-03-29 02:04:07'),(124,'AI模板管理',NULL,'/ai/prompts','/','',0,0,157,'2026-03-29 02:05:13');
/*!40000 ALTER TABLE `menu` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-03 23:02:18
