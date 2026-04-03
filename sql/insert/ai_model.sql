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
-- Table structure for table `ai_model`
--

DROP TABLE IF EXISTS `ai_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_model` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'AI模型ID，主键',
  `model_name` varchar(100) NOT NULL COMMENT '模型名称，如 deepseek-chat、qwen2.5、llama3.1',
  `model_type` varchar(50) NOT NULL COMMENT '模型类型，对应 AiModel.ModelType，例如 OPENAI、DEEPSEEK、OLLAMA',
  `provider_code` varchar(50) NOT NULL COMMENT '模型提供方代码，用于 provider 路由，例如 deepseek、ollama',
  `deployment_mode` varchar(20) NOT NULL COMMENT '部署模式，对应 AiModel.DeploymentMode，例如 CLOUD、LOCAL',
  `api_key` varchar(255) DEFAULT NULL COMMENT 'API密钥，本地模型可为空',
  `base_url` varchar(255) DEFAULT NULL COMMENT '模型服务基础地址',
  `default_params` json DEFAULT NULL COMMENT '默认调用参数JSON，如 temperature、max_tokens',
  `priority` int DEFAULT NULL COMMENT '模型优先级，越小越优先',
  `timeout_ms` int DEFAULT NULL COMMENT '超时时间（毫秒）',
  `supports_stream` tinyint(1) DEFAULT NULL COMMENT '是否支持流式输出',
  `supports_tools` tinyint(1) DEFAULT NULL COMMENT '是否支持工具调用',
  `supports_embedding` tinyint(1) DEFAULT NULL COMMENT '是否支持Embedding',
  `cost_input_per_1m` decimal(10,4) DEFAULT NULL COMMENT '每100万输入Token成本',
  `cost_output_per_1m` decimal(10,4) DEFAULT NULL COMMENT '每100万输出Token成本',
  `is_enabled` tinyint(1) DEFAULT NULL COMMENT '是否启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注说明',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_model_model_name` (`model_name`),
  KEY `idx_ai_model_provider_priority` (`provider_code`,`is_enabled`,`priority`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI模型配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_model`
--

LOCK TABLES `ai_model` WRITE;
/*!40000 ALTER TABLE `ai_model` DISABLE KEYS */;
INSERT INTO `ai_model` VALUES (1,'qwen3.5:latest','OLLAMA','ollama','LOCAL_OLLAMA',NULL,'http://localhost:11434','{\"max_tokens\": 2048, \"temperature\": 0.7}',1,120000,1,0,0,0.0000,0.0000,1,'本地学习环境默认 Ollama 模型，用于 AI 聊天测试','2026-03-26 10:22:54','2026-03-26 11:09:39'),(2,'deepseek-chat','DEEPSEEK','deepseek','REMOTE_API','sk-1297f2ca3006453c92e015af54c148cc','https://api.deepseek.com','{\"max_tokens\": 4096, \"temperature\": 0.7}',10,120000,1,1,0,0.5500,2.1900,1,'DeepSeek 普通对话模型','2026-03-27 02:47:44','2026-03-27 02:48:32'),(3,'deepseek-reasoner','DEEPSEEK','deepseek','REMOTE_API','sk-1297f2ca3006453c92e015af54c148cc','https://api.deepseek.com','{\"max_tokens\": 8192, \"temperature\": 0.6}',20,180000,1,1,0,0.5500,2.1900,1,'DeepSeek 推理模型','2026-03-27 02:48:23','2026-03-27 02:48:46'),(4,'embeddinggemma:300m','OLLAMA','ollama','LOCAL_OLLAMA',NULL,'http://127.0.0.1:11434','{\"keep_alive\": \"30m\"}',1,NULL,NULL,NULL,1,NULL,NULL,1,'Ollama本地嵌入模型，用于文本向量生成','2026-04-03 15:05:04','2026-04-03 15:05:42');
/*!40000 ALTER TABLE `ai_model` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-03 23:06:18
