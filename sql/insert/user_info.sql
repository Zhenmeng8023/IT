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
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '资料更新时间（新增）',
  `occupation` varchar(100) DEFAULT NULL COMMENT '职业（新增）',
  `points` int DEFAULT '0' COMMENT '积分（新增）',
  `experience` int DEFAULT '0' COMMENT '经验值（新增）',
  `is_premium_member` tinyint(1) DEFAULT '0' COMMENT '是否为高级会员（新增）',
  `premium_expiry_date` timestamp NULL DEFAULT NULL COMMENT '高级会员到期时间（新增）',
  `balance` decimal(10,2) DEFAULT '0.00' COMMENT '账户余额，精确到分（新增）',
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
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户基本信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_info`
--

LOCK TABLES `user_info` WRITE;
/*!40000 ALTER TABLE `user_info` DISABLE KEYS */;
INSERT INTO `user_info` VALUES (1,'szw','$2a$10$0.KFlSPxVrSvykbOuVz2seKOkdDDixSFZzb2st2rqmQ/KPoMQH7Kq','2159997489@qq.com',NULL,NULL,'active',NULL,4,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-03-31 02:02:07',NULL,0,0,1,'2026-05-29 17:59:23',0.00),(3,'admin','$2a$10$idZC095FXpQ0LAKiboVfPOD35/Fa.uWsRvRiZlILI9g2vS9xWTSYa','1036144175@qq.com',NULL,NULL,'active',NULL,1,NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,'2026-03-26 10:22:54',NULL,0,0,0,NULL,0.00),(5,'test-2','$2a$10$JJCD0eHmAaTaBkNhh4juuu1WA.NypYzMfMfa9csnbvkKNahKqpJ1e','2@qq.com',NULL,NULL,'active','1231231231',2,NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,'2026-03-26 10:22:54',NULL,0,0,0,NULL,0.00),(6,'test-3','$2a$10$i.Xci2phOzcq6LgLI54WUuacAoXBq3ezCW8Kmkxy5f8leD84pISCS','3@qq.com',NULL,NULL,'active','1111111',3,NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,'2026-03-26 10:22:54',NULL,0,0,0,NULL,0.00),(7,'zfs','$2a$10$F7YOao8VBU1kjH6V/RnoMeC3dGcg1FfCmKJclKS2bMsH9A1CsEzDq','1918023903@qq.com','',NULL,NULL,NULL,4,NULL,NULL,NULL,NULL,'/pic/choubi.jpg','','god',NULL,'other',NULL,'2026-03-29 03:32:06',NULL,0,0,0,NULL,0.00);
/*!40000 ALTER TABLE `user_info` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-03 23:04:15
