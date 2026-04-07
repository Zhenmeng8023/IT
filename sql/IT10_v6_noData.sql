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
-- Table structure for table `ai_call_log`
--

DROP TABLE IF EXISTS `ai_call_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_call_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'AI调用日志ID，主键',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `project_id` bigint DEFAULT NULL COMMENT '项目ID，兼容项目场景统计',
  `biz_type` varchar(30) NOT NULL COMMENT '业务类型，对应 AiSession.BizType',
  `biz_id` bigint DEFAULT NULL COMMENT '业务主记录ID',
  `session_id` bigint DEFAULT NULL COMMENT 'AI会话ID，关联 ai_session 表',
  `message_id` bigint DEFAULT NULL COMMENT 'AI消息ID，关联 ai_message 表',
  `prompt_template_id` bigint DEFAULT NULL COMMENT '提示词模板ID，关联 ai_prompt_template 表',
  `ai_model_id` bigint DEFAULT NULL COMMENT '模型ID，关联 ai_model 表',
  `knowledge_base_id` bigint DEFAULT NULL COMMENT '主知识库ID，关联 knowledge_base 表',
  `request_type` varchar(50) NOT NULL COMMENT '请求类型，对应 AiCallLog.RequestType',
  `request_text` longtext COMMENT '请求文本',
  `response_text` longtext COMMENT '响应文本',
  `request_params` json DEFAULT NULL COMMENT '请求参数JSON',
  `prompt_tokens` int DEFAULT NULL COMMENT '输入Token数',
  `completion_tokens` int DEFAULT NULL COMMENT '输出Token数',
  `total_tokens` int DEFAULT NULL COMMENT '总Token数',
  `cost_amount` decimal(10,4) DEFAULT NULL COMMENT '成本金额',
  `latency_ms` int DEFAULT NULL COMMENT '耗时（毫秒）',
  `status` varchar(20) NOT NULL COMMENT '调用状态，对应 AiCallLog.Status',
  `error_code` varchar(100) DEFAULT NULL COMMENT '错误码',
  `error_message` varchar(500) DEFAULT NULL COMMENT '错误信息',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_call_log_user` (`user_id`,`created_at`),
  KEY `idx_ai_call_log_session` (`session_id`),
  KEY `idx_ai_call_log_model` (`ai_model_id`),
  KEY `fk_ai_call_log_message` (`message_id`),
  KEY `fk_ai_call_log_prompt` (`prompt_template_id`),
  KEY `fk_ai_call_log_kb` (`knowledge_base_id`),
  CONSTRAINT `fk_ai_call_log_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_call_log_message` FOREIGN KEY (`message_id`) REFERENCES `ai_message` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_call_log_model` FOREIGN KEY (`ai_model_id`) REFERENCES `ai_model` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_call_log_prompt` FOREIGN KEY (`prompt_template_id`) REFERENCES `ai_prompt_template` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_call_log_session` FOREIGN KEY (`session_id`) REFERENCES `ai_session` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_call_log_user` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI调用日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_feedback_log`
--

DROP TABLE IF EXISTS `ai_feedback_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_feedback_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'AI反馈日志ID，主键',
  `call_log_id` bigint NOT NULL COMMENT '所属AI调用日志ID，关联 ai_call_log 表',
  `message_id` bigint DEFAULT NULL COMMENT '对应AI消息ID，关联 ai_message 表',
  `user_id` bigint DEFAULT NULL COMMENT '反馈用户ID，关联 user_info 表',
  `feedback_type` varchar(20) NOT NULL COMMENT '反馈类型，对应 AiFeedbackLog.FeedbackType，如 LIKE、DISLIKE、ACCEPTED、RETRY、REPORT',
  `comment_text` varchar(500) DEFAULT NULL COMMENT '反馈说明',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_feedback_log_call` (`call_log_id`),
  KEY `idx_ai_feedback_log_user` (`user_id`),
  KEY `fk_ai_feedback_log_message` (`message_id`),
  CONSTRAINT `fk_ai_feedback_log_call` FOREIGN KEY (`call_log_id`) REFERENCES `ai_call_log` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ai_feedback_log_message` FOREIGN KEY (`message_id`) REFERENCES `ai_message` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_feedback_log_user` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI反馈日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_message`
--

DROP TABLE IF EXISTS `ai_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'AI消息ID，主键',
  `session_id` bigint NOT NULL COMMENT '所属AI会话ID，关联 ai_session 表',
  `role` varchar(20) NOT NULL COMMENT '消息角色，对应 AiMessage.Role，例如 SYSTEM、USER、ASSISTANT、TOOL',
  `sender_user_id` bigint DEFAULT NULL COMMENT '发送用户ID，role=USER 时通常有值',
  `content` longtext NOT NULL COMMENT '消息内容',
  `content_tokens` int DEFAULT NULL COMMENT '内容Token数',
  `prompt_tokens` int DEFAULT NULL COMMENT '输入Token数',
  `completion_tokens` int DEFAULT NULL COMMENT '输出Token数',
  `total_tokens` int DEFAULT NULL COMMENT '总Token数',
  `prompt_template_id` bigint DEFAULT NULL COMMENT '使用的提示词模板ID',
  `model_id` bigint DEFAULT NULL COMMENT '使用的模型ID',
  `knowledge_base_id` bigint DEFAULT NULL COMMENT '关联知识库ID',
  `quoted_chunk_ids` json DEFAULT NULL COMMENT '引用切片ID列表JSON',
  `tool_call_json` json DEFAULT NULL COMMENT '工具调用记录JSON',
  `latency_ms` int DEFAULT NULL COMMENT '耗时（毫秒）',
  `finish_reason` varchar(50) DEFAULT NULL COMMENT '结束原因',
  `status` varchar(20) NOT NULL COMMENT '消息状态，对应 AiMessage.Status，例如 SUCCESS、FAILED、CANCELLED',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_message_session` (`session_id`,`created_at`),
  KEY `idx_ai_message_model` (`model_id`),
  KEY `idx_ai_message_prompt` (`prompt_template_id`),
  KEY `fk_ai_message_sender` (`sender_user_id`),
  KEY `fk_ai_message_kb` (`knowledge_base_id`),
  CONSTRAINT `fk_ai_message_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_message_model` FOREIGN KEY (`model_id`) REFERENCES `ai_model` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_message_prompt` FOREIGN KEY (`prompt_template_id`) REFERENCES `ai_prompt_template` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_message_sender` FOREIGN KEY (`sender_user_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_message_session` FOREIGN KEY (`session_id`) REFERENCES `ai_session` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI模型配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_prompt_template`
--

DROP TABLE IF EXISTS `ai_prompt_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_prompt_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '提示词模板ID，主键',
  `scene_code` varchar(100) NOT NULL COMMENT '场景编码，如 chat_general、knowledge_qa、project_summary',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `template_type` varchar(50) NOT NULL COMMENT '模板类型，对应 AiPromptTemplate.TemplateType，例如 GENERAL_CHAT、BLOG_ASSISTANT',
  `scope_type` varchar(50) NOT NULL COMMENT '作用域，对应 AiPromptTemplate.ScopeType，例如 PLATFORM、PROJECT、PERSONAL',
  `project_id` bigint DEFAULT NULL COMMENT '项目ID，项目级模板使用，关联 project 表',
  `owner_id` bigint DEFAULT NULL COMMENT '模板拥有者ID，关联 user_info 表',
  `default_model_id` bigint DEFAULT NULL COMMENT '默认模型ID，关联 ai_model 表',
  `system_prompt` longtext NOT NULL COMMENT '系统提示词',
  `user_prompt_template` longtext COMMENT '用户提示词模板',
  `variables_schema` json DEFAULT NULL COMMENT '模板变量定义JSON',
  `output_schema` json DEFAULT NULL COMMENT '输出结构约束JSON',
  `version_no` int DEFAULT NULL COMMENT '版本号',
  `publish_status` varchar(20) NOT NULL COMMENT '发布状态，对应 DRAFT、PUBLISHED、DISABLED',
  `is_enabled` tinyint(1) DEFAULT NULL COMMENT '是否启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注说明',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_prompt_scene_code` (`scene_code`),
  KEY `idx_ai_prompt_scope_type` (`scope_type`),
  KEY `idx_ai_prompt_default_model` (`default_model_id`),
  KEY `fk_ai_prompt_template_owner` (`owner_id`),
  CONSTRAINT `fk_ai_prompt_template_default_model` FOREIGN KEY (`default_model_id`) REFERENCES `ai_model` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_prompt_template_owner` FOREIGN KEY (`owner_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI提示词模板表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_retrieval_log`
--

DROP TABLE IF EXISTS `ai_retrieval_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_retrieval_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'AI检索日志ID，主键',
  `call_log_id` bigint NOT NULL COMMENT '所属AI调用日志ID，关联 ai_call_log 表',
  `knowledge_base_id` bigint DEFAULT NULL COMMENT '知识库ID，关联 knowledge_base 表',
  `document_id` bigint DEFAULT NULL COMMENT '文档ID，关联 knowledge_document 表',
  `chunk_id` bigint DEFAULT NULL COMMENT '切片ID，关联 knowledge_chunk 表',
  `query_text` text COMMENT '检索查询文本',
  `score` decimal(10,6) DEFAULT NULL COMMENT '分值',
  `rank_no` int DEFAULT NULL COMMENT '排名',
  `retrieval_method` varchar(20) NOT NULL COMMENT '检索方式，对应 AiRetrievalLog.RetrievalMethod',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_retrieval_log_call` (`call_log_id`),
  KEY `idx_ai_retrieval_log_chunk` (`chunk_id`),
  KEY `fk_ai_retrieval_log_kb` (`knowledge_base_id`),
  KEY `fk_ai_retrieval_log_doc` (`document_id`),
  CONSTRAINT `fk_ai_retrieval_log_call` FOREIGN KEY (`call_log_id`) REFERENCES `ai_call_log` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ai_retrieval_log_chunk` FOREIGN KEY (`chunk_id`) REFERENCES `knowledge_chunk` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_retrieval_log_doc` FOREIGN KEY (`document_id`) REFERENCES `knowledge_document` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_retrieval_log_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI检索日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_session`
--

DROP TABLE IF EXISTS `ai_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'AI会话ID，主键',
  `user_id` bigint NOT NULL COMMENT '用户ID，关联 user_info 表',
  `biz_type` varchar(30) NOT NULL COMMENT '业务类型，对应 AiSession.BizType，例如 GENERAL、PROJECT、BLOG、CIRCLE、PAID_CONTENT',
  `biz_id` bigint DEFAULT NULL COMMENT '业务主记录ID，结合 biz_type 使用',
  `project_id` bigint DEFAULT NULL COMMENT '兼容保留的项目ID，关联 project 表',
  `scene_code` varchar(100) NOT NULL COMMENT '场景编码',
  `session_title` varchar(200) DEFAULT NULL COMMENT '会话标题',
  `memory_mode` varchar(20) NOT NULL COMMENT '记忆模式，对应 AiSession.MemoryMode，例如 NONE、SHORT、SUMMARY',
  `active_model_id` bigint DEFAULT NULL COMMENT '当前会话默认模型ID，关联 ai_model 表',
  `prompt_template_id` bigint DEFAULT NULL COMMENT '默认提示词模板ID，关联 ai_prompt_template 表',
  `default_knowledge_base_id` bigint DEFAULT NULL COMMENT '默认知识库ID，关联 knowledge_base 表',
  `session_summary` longtext COMMENT '会话摘要',
  `summary_updated_at` timestamp NULL DEFAULT NULL COMMENT '会话摘要更新时间',
  `ext_config` json DEFAULT NULL COMMENT '扩展配置JSON',
  `status` varchar(20) NOT NULL COMMENT '会话状态，对应 AiSession.Status，例如 ACTIVE、ARCHIVED、DELETED',
  `last_message_at` timestamp NULL DEFAULT NULL COMMENT '最后消息时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_session_user` (`user_id`,`scene_code`),
  KEY `idx_ai_session_model` (`active_model_id`),
  KEY `idx_ai_session_prompt` (`prompt_template_id`),
  KEY `idx_ai_session_default_kb` (`default_knowledge_base_id`),
  CONSTRAINT `fk_ai_session_default_kb` FOREIGN KEY (`default_knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_session_model` FOREIGN KEY (`active_model_id`) REFERENCES `ai_model` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_session_prompt` FOREIGN KEY (`prompt_template_id`) REFERENCES `ai_prompt_template` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_session_user` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI会话表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_session_knowledge_base`
--

DROP TABLE IF EXISTS `ai_session_knowledge_base`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_session_knowledge_base` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话知识库绑定ID，主键',
  `session_id` bigint NOT NULL COMMENT 'AI会话ID，关联 ai_session 表',
  `knowledge_base_id` bigint NOT NULL COMMENT '知识库ID，关联 knowledge_base 表',
  `priority` int DEFAULT NULL COMMENT '优先级，越小越优先',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_session_kb` (`session_id`,`knowledge_base_id`),
  KEY `idx_ai_session_kb_priority` (`session_id`,`priority`),
  KEY `fk_ai_session_kb_kb` (`knowledge_base_id`),
  CONSTRAINT `fk_ai_session_kb_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ai_session_kb_session` FOREIGN KEY (`session_id`) REFERENCES `ai_session` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI会话知识库绑定表';
/*!40101 SET character_set_client = @saved_cs_client */;

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
-- Table structure for table `blog`
--

DROP TABLE IF EXISTS `blog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blog` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '博客ID，主键',
  `title` varchar(200) NOT NULL COMMENT '博客标题',
  `summary` varchar(255) DEFAULT NULL COMMENT '博客摘要',
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
  `price` int DEFAULT '0' COMMENT '博客价格（积分或金币）',
  PRIMARY KEY (`id`),
  KEY `idx_author_status` (`author_id`,`status`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_is_marked` (`is_marked`),
  CONSTRAINT `blog_ibfk_1` FOREIGN KEY (`author_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='博客内容表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blog_audit_log`
--

DROP TABLE IF EXISTS `blog_audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blog_audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '审核记录 ID，主键',
  `blog_id` bigint NOT NULL COMMENT '被审核博客的 ID，对应 blog 表主键',
  `audit_type` enum('AUTO','MANUAL') NOT NULL DEFAULT 'AUTO' COMMENT '审核类型：AUTO 自动审核，MANUAL 人工审核',
  `audit_status` enum('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING' COMMENT '审核状态',
  `audit_score` decimal(5,2) DEFAULT NULL COMMENT '自动审核评分，范围 0-100',
  `audit_reason` text COMMENT '审核原因，自动审核的结果说明或人工审核的理由',
  `auto_review_suggestion` varchar(200) DEFAULT NULL COMMENT '自动审核建议，如内容质量改进建议',
  `requires_manual_review` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否需要人工复审：0 否，1 是',
  `auditor_id` bigint DEFAULT NULL COMMENT '审核人 ID，关联 user_info 表，自动审核时为 NULL',
  `audit_comment` varchar(500) DEFAULT NULL COMMENT '审核备注，人工审核时的详细意见',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交审核时间',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审核完成时间',
  PRIMARY KEY (`id`),
  KEY `idx_blog_status` (`blog_id`,`audit_status`),
  KEY `idx_audit_type` (`audit_type`),
  KEY `idx_auditor` (`auditor_id`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_blog_audit_auditor` FOREIGN KEY (`auditor_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_blog_audit_blog` FOREIGN KEY (`blog_id`) REFERENCES `blog` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='博客审核记录表：用于记录博客的自动审核和人工审核全过程，包括评分、结果和审核意见';
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `post_type` enum('blog','project') DEFAULT 'blog' COMMENT '评论目标类型（新增，解决 post_id 语义冲突）',
  PRIMARY KEY (`id`),
  KEY `idx_post_type_post_id` (`post_id`) COMMENT '按ID快速查找评论',
  KEY `idx_author` (`author_id`),
  KEY `idx_parent` (`parent_comment_id`),
  KEY `idx_comment_post_type_post_id` (`post_type`,`post_id`),
  CONSTRAINT `comment_ibfk_1` FOREIGN KEY (`author_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `comment_ibfk_2` FOREIGN KEY (`parent_comment_id`) REFERENCES `comment` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评论表，支持对博客和项目进行评论和回复';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `content_access`
--

DROP TABLE IF EXISTS `content_access`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `content_review_record`
--

DROP TABLE IF EXISTS `content_review_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通用私信/群聊会话表，仅用于站内消息与群聊，不承载AI会话';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `coupon`
--

DROP TABLE IF EXISTS `coupon`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `coupon_redemption`
--

DROP TABLE IF EXISTS `coupon_redemption`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  KEY `coupon_redemption_ibfk_3` (`user_id`),
  CONSTRAINT `coupon_redemption_ibfk_1` FOREIGN KEY (`user_coupon_id`) REFERENCES `user_coupon` (`id`) ON DELETE CASCADE,
  CONSTRAINT `coupon_redemption_ibfk_2` FOREIGN KEY (`coupon_id`) REFERENCES `coupon` (`id`) ON DELETE CASCADE,
  CONSTRAINT `coupon_redemption_ibfk_3` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `coupon_redemption_ibfk_4` FOREIGN KEY (`order_id`) REFERENCES `payment_order` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券核销记录表：记录优惠券在订单中的使用、抵扣金额和回滚信息，用于审计与统计';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `creator_settlement_account`
--

DROP TABLE IF EXISTS `creator_settlement_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `creator_settlement_account` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '结算账户ID，主键',
  `user_id` bigint NOT NULL COMMENT '创作者用户ID，关联 user_info 表',
  `account_type` varchar(20) NOT NULL COMMENT '账户类型：alipay支付宝，wechat微信，bank_card银行卡，manual线下人工',
  `account_name` varchar(100) NOT NULL COMMENT '收款账户姓名或账户名',
  `account_no` varchar(100) NOT NULL COMMENT '收款账号或卡号',
  `bank_name` varchar(100) DEFAULT NULL COMMENT '银行名称，当 account_type=bank_card 时可填写',
  `qr_code_url` varchar(500) DEFAULT NULL COMMENT '收款码图片地址，当 account_type=alipay 或 wechat 时可填写',
  `is_default` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否默认收款账户：1是，0否',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '账户状态：ACTIVE启用，DISABLED停用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注信息',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_creator_account_user` (`user_id`,`status`),
  CONSTRAINT `fk_creator_settlement_account_user` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='创作者结算账户表：管理作者提现时可使用的收款账户';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `creator_withdraw_item`
--

DROP TABLE IF EXISTS `creator_withdraw_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `creator_withdraw_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '提现明细ID，主键',
  `withdraw_request_id` bigint NOT NULL COMMENT '所属提现申请ID，关联 creator_withdraw_request 表',
  `revenue_record_id` bigint NOT NULL COMMENT '对应收益记录ID，关联 revenue_record 表',
  `amount` decimal(10,2) NOT NULL COMMENT '本条收益用于提现的金额，通常等于该收益记录中的 author_revenue',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_withdraw_revenue_record` (`revenue_record_id`),
  KEY `idx_withdraw_item_request` (`withdraw_request_id`),
  CONSTRAINT `fk_creator_withdraw_item_request` FOREIGN KEY (`withdraw_request_id`) REFERENCES `creator_withdraw_request` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_creator_withdraw_item_revenue` FOREIGN KEY (`revenue_record_id`) REFERENCES `revenue_record` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='创作者提现明细表：将每次提现与具体收益记录逐条绑定，防止重复提现';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `creator_withdraw_request`
--

DROP TABLE IF EXISTS `creator_withdraw_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `creator_withdraw_request` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '提现申请ID，主键',
  `request_no` varchar(64) NOT NULL COMMENT '提现申请单号，全局唯一',
  `user_id` bigint NOT NULL COMMENT '申请提现的创作者ID，关联 user_info 表',
  `settlement_account_id` bigint NOT NULL COMMENT '收款账户ID，关联 creator_settlement_account 表',
  `withdraw_amount` decimal(10,2) NOT NULL COMMENT '申请提现金额',
  `service_fee` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '手续费，个人学习场景通常为0',
  `actual_amount` decimal(10,2) NOT NULL COMMENT '实际打款金额=提现金额-手续费',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态：PENDING待审核，APPROVED已通过待打款，REJECTED已拒绝，PAID已打款，CANCELLED已取消',
  `reviewed_by` bigint DEFAULT NULL COMMENT '审核人ID，关联 user_info 表',
  `review_note` varchar(500) DEFAULT NULL COMMENT '审核备注',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
  `paid_at` datetime DEFAULT NULL COMMENT '打款完成时间',
  `pay_channel_ref` varchar(100) DEFAULT NULL COMMENT '打款渠道流水号或人工登记号',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_withdraw_request_no` (`request_no`),
  KEY `idx_withdraw_user_status` (`user_id`,`status`),
  KEY `idx_withdraw_account_id` (`settlement_account_id`),
  KEY `idx_withdraw_reviewer` (`reviewed_by`),
  CONSTRAINT `fk_creator_withdraw_request_account` FOREIGN KEY (`settlement_account_id`) REFERENCES `creator_settlement_account` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_creator_withdraw_request_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `user_info` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_creator_withdraw_request_user` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='创作者提现申请表：记录提现金额、审核状态和打款信息';
/*!40101 SET character_set_client = @saved_cs_client */;

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
  UNIQUE KEY `uk_follower_followee` (`follower_id`,`followee_id`),
  KEY `idx_followee` (`followee_id`),
  CONSTRAINT `follow_ibfk_1` FOREIGN KEY (`follower_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `follow_ibfk_2` FOREIGN KEY (`followee_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户关注关系表（单向）';
/*!40101 SET character_set_client = @saved_cs_client */;

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
-- Table structure for table `knowledge_base`
--

DROP TABLE IF EXISTS `knowledge_base`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `knowledge_base` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '知识库ID，主键',
  `scope_type` varchar(30) NOT NULL COMMENT '作用域，对应 KnowledgeBase.ScopeType，例如 PERSONAL、PROJECT、PLATFORM',
  `project_id` bigint DEFAULT NULL COMMENT '项目ID，项目知识库使用，关联 project 表',
  `owner_id` bigint DEFAULT NULL COMMENT '拥有者用户ID，关联 user_info 表',
  `name` varchar(100) NOT NULL COMMENT '知识库名称',
  `description` longtext COMMENT '知识库描述',
  `source_type` varchar(30) NOT NULL COMMENT '来源类型，对应 KnowledgeBase.SourceType',
  `embedding_provider` varchar(50) DEFAULT NULL COMMENT 'Embedding提供方',
  `embedding_model` varchar(100) DEFAULT NULL COMMENT 'Embedding模型名',
  `chunk_strategy` varchar(30) NOT NULL COMMENT '切片策略，对应 KnowledgeBase.ChunkStrategy',
  `default_top_k` int DEFAULT NULL COMMENT '默认召回TopK',
  `visibility` varchar(20) NOT NULL COMMENT '可见性，对应 KnowledgeBase.Visibility',
  `status` varchar(20) NOT NULL COMMENT '状态，对应 KnowledgeBase.Status',
  `doc_count` int DEFAULT NULL COMMENT '文档数量',
  `chunk_count` int DEFAULT NULL COMMENT '切片数量',
  `last_indexed_at` timestamp NULL DEFAULT NULL COMMENT '最后索引时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_knowledge_base_scope` (`scope_type`,`project_id`),
  KEY `idx_knowledge_base_owner` (`owner_id`),
  CONSTRAINT `fk_knowledge_base_owner` FOREIGN KEY (`owner_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `knowledge_base_member`
--

DROP TABLE IF EXISTS `knowledge_base_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `knowledge_base_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '知识库成员ID，主键',
  `knowledge_base_id` bigint NOT NULL COMMENT '知识库ID，关联 knowledge_base 表',
  `user_id` bigint NOT NULL COMMENT '成员用户ID，关联 user_info 表',
  `role_code` varchar(20) NOT NULL COMMENT '成员角色，对应 KnowledgeBaseMember.RoleCode，例如 OWNER、EDITOR、VIEWER',
  `granted_by` bigint DEFAULT NULL COMMENT '授权人用户ID，关联 user_info 表',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_knowledge_base_member` (`knowledge_base_id`,`user_id`),
  KEY `idx_knowledge_base_member_user` (`user_id`),
  KEY `fk_knowledge_base_member_granted_by` (`granted_by`),
  CONSTRAINT `fk_knowledge_base_member_granted_by` FOREIGN KEY (`granted_by`) REFERENCES `user_info` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_knowledge_base_member_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_knowledge_base_member_user` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库成员权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `knowledge_chunk`
--

DROP TABLE IF EXISTS `knowledge_chunk`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `knowledge_chunk` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '知识切片ID，主键',
  `knowledge_base_id` bigint NOT NULL COMMENT '所属知识库ID，关联 knowledge_base 表',
  `document_id` bigint NOT NULL COMMENT '所属文档ID，关联 knowledge_document 表',
  `chunk_index` int NOT NULL COMMENT '文档内切片序号',
  `title` varchar(255) DEFAULT NULL COMMENT '切片标题',
  `content` longtext NOT NULL COMMENT '切片正文',
  `token_count` int DEFAULT NULL COMMENT 'Token数量',
  `char_count` int DEFAULT NULL COMMENT '字符数',
  `embedding_provider` varchar(50) DEFAULT NULL COMMENT 'Embedding提供方',
  `embedding_model` varchar(100) DEFAULT NULL COMMENT 'Embedding模型名称',
  `vector_id` varchar(255) DEFAULT NULL COMMENT '向量引用ID',
  `metadata_json` json DEFAULT NULL COMMENT '切片元数据JSON',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态，对应 KnowledgeChunk.Status',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_knowledge_chunk_doc_idx` (`document_id`,`chunk_index`),
  KEY `idx_knowledge_chunk_kb` (`knowledge_base_id`),
  KEY `idx_knowledge_chunk_vector_id` (`vector_id`),
  CONSTRAINT `fk_knowledge_chunk_doc` FOREIGN KEY (`document_id`) REFERENCES `knowledge_document` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_knowledge_chunk_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识切片表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `knowledge_chunk_embedding`
--

DROP TABLE IF EXISTS `knowledge_chunk_embedding`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `knowledge_chunk_embedding` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '切片向量记录ID，主键',
  `chunk_id` bigint NOT NULL COMMENT '切片ID，关联 knowledge_chunk 表',
  `provider_code` varchar(50) NOT NULL COMMENT '向量提供方代码',
  `model_name` varchar(100) NOT NULL COMMENT '向量模型名称',
  `dimension` int DEFAULT NULL COMMENT '向量维度',
  `vector_ref` varchar(255) DEFAULT NULL COMMENT '外部向量引用ID',
  `vector_payload` longtext COMMENT '向量快照，可存JSON字符串',
  `status` varchar(20) NOT NULL COMMENT '状态，对应 KnowledgeChunkEmbedding.Status',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_knowledge_chunk_embedding` (`chunk_id`,`provider_code`,`model_name`),
  KEY `idx_knowledge_chunk_embedding_status` (`status`),
  CONSTRAINT `fk_knowledge_chunk_embedding_chunk` FOREIGN KEY (`chunk_id`) REFERENCES `knowledge_chunk` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识切片向量表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `knowledge_document`
--

DROP TABLE IF EXISTS `knowledge_document`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `knowledge_document` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '知识文档ID，主键',
  `knowledge_base_id` bigint NOT NULL COMMENT '所属知识库ID，关联 knowledge_base 表',
  `title` varchar(255) NOT NULL COMMENT '文档标题',
  `source_type` varchar(50) NOT NULL COMMENT '来源类型，对应 KnowledgeDocument.SourceType',
  `source_ref_id` bigint DEFAULT NULL COMMENT '来源业务ID',
  `source_url` varchar(500) DEFAULT NULL COMMENT '来源URL',
  `file_name` varchar(255) DEFAULT NULL COMMENT '原始文件名',
  `archive_name` varchar(255) DEFAULT NULL COMMENT '导入的zip文件名',
  `archive_entry_path` varchar(1000) DEFAULT NULL COMMENT '文件在zip内的相对路径',
  `import_batch_id` varchar(64) DEFAULT NULL COMMENT '一次zip导入批次号',
  `mime_type` varchar(100) DEFAULT NULL COMMENT '文件MIME类型',
  `storage_path` varchar(500) DEFAULT NULL COMMENT '存储路径',
  `content_text` longtext COMMENT '抽取后的正文文本',
  `content_hash` varchar(64) DEFAULT NULL COMMENT '内容哈希',
  `language` varchar(20) DEFAULT NULL COMMENT '语言代码',
  `status` varchar(50) NOT NULL COMMENT '状态，对应 KnowledgeDocument.Status',
  `error_message` varchar(500) DEFAULT NULL COMMENT '错误信息',
  `uploaded_by` bigint DEFAULT NULL COMMENT '上传者用户ID，关联 user_info 表',
  `indexed_at` timestamp NULL DEFAULT NULL COMMENT '索引完成时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_knowledge_document_kb` (`knowledge_base_id`),
  KEY `idx_knowledge_document_status` (`status`),
  KEY `idx_knowledge_document_uploaded_by` (`uploaded_by`),
  KEY `idx_kd_import_batch_id` (`import_batch_id`),
  KEY `idx_kd_archive_entry_path` (`archive_entry_path`(255)),
  CONSTRAINT `fk_knowledge_document_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_knowledge_document_uploaded_by` FOREIGN KEY (`uploaded_by`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库文档表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `knowledge_import_task`
--

DROP TABLE IF EXISTS `knowledge_import_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `knowledge_import_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `knowledge_base_id` bigint NOT NULL,
  `zip_name` varchar(255) NOT NULL,
  `temp_zip_path` varchar(1000) NOT NULL,
  `status` varchar(20) NOT NULL,
  `current_stage` varchar(30) NOT NULL,
  `total_files` int DEFAULT '0',
  `scanned_files` int DEFAULT '0',
  `imported_files` int DEFAULT '0',
  `skipped_files` int DEFAULT '0',
  `failed_files` int DEFAULT '0',
  `progress_percent` int DEFAULT '0',
  `current_file` varchar(1000) DEFAULT NULL,
  `error_message` varchar(500) DEFAULT NULL,
  `cancel_requested` tinyint(1) DEFAULT '0',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `finished_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_kit_kb_id` (`knowledge_base_id`),
  KEY `idx_kit_status` (`status`),
  KEY `idx_kit_created_at` (`created_at` DESC),
  CONSTRAINT `fk_knowledge_import_task_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `knowledge_index_task`
--

DROP TABLE IF EXISTS `knowledge_index_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `knowledge_index_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '知识索引任务ID，主键',
  `knowledge_base_id` bigint NOT NULL COMMENT '知识库ID，关联 knowledge_base 表',
  `document_id` bigint DEFAULT NULL COMMENT '文档ID，关联 knowledge_document 表',
  `task_type` varchar(20) NOT NULL COMMENT '任务类型，对应 KnowledgeIndexTask.TaskType',
  `status` varchar(20) NOT NULL COMMENT '任务状态，对应 KnowledgeIndexTask.Status',
  `retry_count` int DEFAULT NULL COMMENT '重试次数',
  `error_message` varchar(500) DEFAULT NULL COMMENT '错误信息',
  `started_at` timestamp NULL DEFAULT NULL COMMENT '开始时间',
  `finished_at` timestamp NULL DEFAULT NULL COMMENT '结束时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_knowledge_index_task_kb` (`knowledge_base_id`,`status`),
  KEY `idx_knowledge_index_task_doc` (`document_id`,`task_type`),
  CONSTRAINT `fk_knowledge_index_task_doc` FOREIGN KEY (`document_id`) REFERENCES `knowledge_document` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_knowledge_index_task_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识索引任务表';
/*!40101 SET character_set_client = @saved_cs_client */;

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
-- Table structure for table `membership`
--

DROP TABLE IF EXISTS `membership`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `membership_level`
--

DROP TABLE IF EXISTS `membership_level`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='前端菜单及权限配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通用私信/群聊消息表，仅用于站内消息，不承载AI消息';
/*!40101 SET character_set_client = @saved_cs_client */;

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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `paid_content`
--

DROP TABLE IF EXISTS `paid_content`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  CONSTRAINT `chk_paid_content_ref` CHECK ((((`content_type` = _utf8mb4'blog') and (`blog_id` is not null) and (`project_id` is null) and (`content_id` = `blog_id`)) or ((`content_type` = _utf8mb4'project') and (`project_id` is not null) and (`blog_id` is null) and (`content_id` = `project_id`))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='付费内容表，修复了 datata 中一个字段同时外键到 blog/project 的冲突问题';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `payment_order`
--

DROP TABLE IF EXISTS `payment_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_order` (
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
  CONSTRAINT `payment_order_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `payment_order_ibfk_2` FOREIGN KEY (`paid_content_id`) REFERENCES `paid_content` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `payment_order_ibfk_3` FOREIGN KEY (`membership_level_id`) REFERENCES `membership_level` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_order_ref` CHECK ((((`type` = _utf8mb4'content') and (`paid_content_id` is not null) and (`membership_level_id` is null) and ((`target_id` is null) or (`target_id` = `paid_content_id`))) or ((`type` = _utf8mb4'membership') and (`membership_level_id` is not null) and (`paid_content_id` is null) and ((`target_id` is null) or (`target_id` = `membership_level_id`)))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单表，修复了 datata 中一个字段同时外键到 paid_content/membership_level 的冲突问题';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `payment_record`
--

DROP TABLE IF EXISTS `payment_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  CONSTRAINT `payment_record_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `payment_order` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统权限点表';
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `template_id` bigint DEFAULT NULL COMMENT '使用的项目模板ID',
  `visibility` enum('public','friends_only','private') DEFAULT 'public' COMMENT '项目可见性',
  PRIMARY KEY (`id`),
  KEY `idx_author_status` (`author_id`,`status`),
  KEY `idx_template_id` (`template_id`),
  CONSTRAINT `project_ibfk_1` FOREIGN KEY (`author_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_ibfk_2` FOREIGN KEY (`template_id`) REFERENCES `project_template` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目资源表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_activity_log`
--

DROP TABLE IF EXISTS `project_activity_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_doc`
--

DROP TABLE IF EXISTS `project_doc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_doc` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目文档ID，主键',
  `project_id` bigint NOT NULL COMMENT '所属项目ID，关联project表',
  `title` varchar(255) NOT NULL COMMENT '文档标题',
  `doc_type` enum('wiki','spec','meeting_note','design','manual','other') NOT NULL DEFAULT 'wiki' COMMENT '文档类型：wiki知识库，spec需求/规格，meeting_note会议纪要，design设计文档，manual使用手册，other其他',
  `status` enum('draft','published','archived') NOT NULL DEFAULT 'draft' COMMENT '文档状态：draft草稿，published已发布，archived已归档',
  `visibility` enum('project','team','private') NOT NULL DEFAULT 'project' COMMENT '文档可见范围：project项目内可见，team团队可见，private仅自己可见',
  `is_primary` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否为主 README 文档',
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
  KEY `idx_project_doc_project_primary` (`project_id`,`is_primary`,`updated_at`),
  CONSTRAINT `project_doc_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_doc_ibfk_2` FOREIGN KEY (`creator_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL,
  CONSTRAINT `project_doc_ibfk_3` FOREIGN KEY (`editor_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目文档表：用于存储项目内的说明文档、设计文档、会议纪要、使用手册等';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_doc_version`
--

DROP TABLE IF EXISTS `project_doc_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_download_record`
--

DROP TABLE IF EXISTS `project_download_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `version` varchar(20) DEFAULT '1.0' COMMENT '当前文件版本号',
  `is_latest` tinyint(1) DEFAULT '1' COMMENT '是否为最新版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_file_name` (`project_id`,`file_name`),
  KEY `idx_project_upload_time` (`project_id`,`upload_time`),
  CONSTRAINT `project_file_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目附件文件表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_file_version`
--

DROP TABLE IF EXISTS `project_file_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_integration`
--

DROP TABLE IF EXISTS `project_integration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_integration_event`
--

DROP TABLE IF EXISTS `project_integration_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_invitation`
--

DROP TABLE IF EXISTS `project_invitation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_join_request`
--

DROP TABLE IF EXISTS `project_join_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_media`
--

DROP TABLE IF EXISTS `project_media`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_member`
--

DROP TABLE IF EXISTS `project_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '成员关系ID，主键',
  `project_id` bigint NOT NULL COMMENT '项目ID，关联project表',
  `user_id` bigint NOT NULL COMMENT '用户ID，关联user_info表',
  `role` enum('owner','admin','member','viewer') NOT NULL DEFAULT 'member' COMMENT '成员角色：owner-所有者, admin-管理员, member-成员, viewer-查看者',
  `joined_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `status` enum('active','inactive') DEFAULT 'active' COMMENT '成员状态：active-活跃, inactive-非活跃',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_user` (`project_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `project_member_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_member_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目成员表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_milestone`
--

DROP TABLE IF EXISTS `project_milestone`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_profile`
--

DROP TABLE IF EXISTS `project_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_release`
--

DROP TABLE IF EXISTS `project_release`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_release_file`
--

DROP TABLE IF EXISTS `project_release_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_sprint`
--

DROP TABLE IF EXISTS `project_sprint`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_star`
--

DROP TABLE IF EXISTS `project_star`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_star` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_star_project_user` (`project_id`,`user_id`),
  KEY `idx_project_star_user_created` (`user_id`,`created_at`),
  KEY `idx_project_star_project` (`project_id`),
  CONSTRAINT `fk_project_star_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目收藏关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_star_record`
--

DROP TABLE IF EXISTS `project_star_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_stat_daily`
--

DROP TABLE IF EXISTS `project_stat_daily`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_task`
--

DROP TABLE IF EXISTS `project_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  `completed_by` bigint DEFAULT NULL COMMENT '任务完成人ID',
  `completed_member_joined_at` datetime DEFAULT NULL COMMENT '任务完成时负责人当前这次入组的 joined_at 快照',
  `last_reopened_by` bigint DEFAULT NULL COMMENT '最近一次重开审批人ID',
  `last_reopened_at` datetime DEFAULT NULL COMMENT '最近一次重开审批时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `project_id` (`project_id`),
  KEY `assignee_id` (`assignee_id`),
  KEY `created_by` (`created_by`),
  KEY `idx_project_task_completed_by` (`completed_by`),
  KEY `idx_project_task_project_assignee_status` (`project_id`,`assignee_id`,`status`),
  KEY `idx_project_task_project_completed_cycle` (`project_id`,`completed_member_joined_at`),
  CONSTRAINT `project_task_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `project_task_ibfk_2` FOREIGN KEY (`assignee_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL,
  CONSTRAINT `project_task_ibfk_3` FOREIGN KEY (`created_by`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目任务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_task_attachment`
--

DROP TABLE IF EXISTS `project_task_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_task_checklist_item`
--

DROP TABLE IF EXISTS `project_task_checklist_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_task_comment`
--

DROP TABLE IF EXISTS `project_task_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_task_dependency`
--

DROP TABLE IF EXISTS `project_task_dependency`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_task_log`
--

DROP TABLE IF EXISTS `project_task_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_task_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务操作日志ID，主键',
  `task_id` bigint NOT NULL COMMENT '所属任务ID，关联project_task表',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID，关联user_info表',
  `action` enum('create','update','assign','change_status','change_priority','comment','attach','complete','reopen','delete','reopen_request','reopen_approve','reopen_reject','reopen_cancel') NOT NULL COMMENT '操作类型：create创建，update修改，assign指派，change_status改状态，change_priority改优先级，comment评论，attach上传附件，complete完成，reopen重新开启，delete删除，reopen_request申请重开，reopen_approve通过重开，reopen_reject驳回重开，reopen_cancel撤销重开',
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_task_reopen_request`
--

DROP TABLE IF EXISTS `project_task_reopen_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_task_reopen_request` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `applicant_id` bigint NOT NULL COMMENT '申请人ID',
  `applicant_member_joined_at` datetime NOT NULL COMMENT '申请人当前这次入组 joined_at 快照',
  `from_status` varchar(20) NOT NULL COMMENT '申请前状态，通常为 done',
  `target_status` varchar(20) NOT NULL COMMENT '目标状态，todo 或 in_progress',
  `reason` varchar(500) NOT NULL COMMENT '申请原因',
  `status` varchar(20) NOT NULL COMMENT '审批状态：pending/approved/rejected/cancelled',
  `reviewer_id` bigint DEFAULT NULL COMMENT '审批人ID',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审批时间',
  `review_remark` varchar(500) DEFAULT NULL COMMENT '审批备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ptrr_task_id` (`task_id`),
  KEY `idx_ptrr_project_id` (`project_id`),
  KEY `idx_ptrr_applicant_id` (`applicant_id`),
  KEY `idx_ptrr_status` (`status`),
  KEY `idx_ptrr_task_status` (`task_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务重开申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_template`
--

DROP TABLE IF EXISTS `project_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_template_file`
--

DROP TABLE IF EXISTS `project_template_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_template_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板文件ID，主键',
  `template_id` bigint NOT NULL COMMENT '所属模板ID，关联project_template表',
  `item_type` varchar(30) NOT NULL DEFAULT 'file' COMMENT '模板项类型：readme/doc/task/file',
  `item_key` varchar(100) DEFAULT NULL COMMENT '模板项业务键',
  `group_name` varchar(30) DEFAULT NULL COMMENT '模板项分组：docs/tasks/files/activities/meta',
  `source_id` bigint DEFAULT NULL COMMENT '来源记录ID',
  `file_name` varchar(255) NOT NULL COMMENT '文件原始名称',
  `file_path` varchar(500) NOT NULL COMMENT '文件在服务器上的存储路径',
  `file_ext` varchar(50) DEFAULT NULL COMMENT '文件后缀',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小，单位字节',
  `mime_type` varchar(100) DEFAULT NULL COMMENT '文件MIME类型',
  `include_content` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否保存了文件内容快照',
  `version` varchar(20) DEFAULT '1.0' COMMENT '文件版本号',
  `sort_order` int DEFAULT '0' COMMENT '模板项排序',
  `payload_json` longtext COMMENT '模板项快照JSON',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `template_id` (`template_id`),
  KEY `idx_project_template_file_template_sort` (`template_id`,`sort_order`,`id`),
  KEY `idx_project_template_file_group_type` (`template_id`,`group_name`,`item_type`),
  CONSTRAINT `project_template_file_ibfk_1` FOREIGN KEY (`template_id`) REFERENCES `project_template` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目模板文件表';
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `recommended_items` json NOT NULL COMMENT '推荐项目列表，JSON格式',
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
-- Table structure for table `recommendation_rule`
--

DROP TABLE IF EXISTS `recommendation_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

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
  UNIQUE KEY `uk_region_code` (`code`),
  KEY `parent_id` (`parent_id`),
  CONSTRAINT `region_ibfk_1` FOREIGN KEY (`parent_id`) REFERENCES `region` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='地区信息表，支持多级行政区域划分';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `report`
--

DROP TABLE IF EXISTS `report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `revenue_record`
--

DROP TABLE IF EXISTS `revenue_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  CONSTRAINT `revenue_record_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `payment_order` (`id`) ON DELETE CASCADE,
  CONSTRAINT `revenue_record_ibfk_2` FOREIGN KEY (`source_user_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收益记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

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
-- Table structure for table `search_index`
--

DROP TABLE IF EXISTS `search_index`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `search_index` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '索引ID，主键',
  `content` text COMMENT '用于搜索的文本内容',
  `doc_type` enum('blog','project','user','circle') NOT NULL COMMENT '文档类型',
  `doc_id` bigint NOT NULL COMMENT '对应文档的ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '索引创建时间',
  PRIMARY KEY (`id`),
  FULLTEXT KEY `idx_content` (`content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='全文搜索引擎索引表';
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '标签更新时间（新增，不影响IT8兼容）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_name_parent` (`name`,`parent_id`),
  KEY `parent_id` (`parent_id`),
  CONSTRAINT `tag_ibfk_1` FOREIGN KEY (`parent_id`) REFERENCES `tag` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标签信息表，支持树状层级结构，用于对博客和项目进行分类';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_behavior`
--

DROP TABLE IF EXISTS `user_behavior`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_coupon`
--

DROP TABLE IF EXISTS `user_coupon`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  CONSTRAINT `user_coupon_ibfk_3` FOREIGN KEY (`order_id`) REFERENCES `payment_order` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户优惠券领取表：记录用户实际领到的优惠券实例，是优惠券领取、状态流转和过期控制的核心表';
/*!40101 SET character_set_client = @saved_cs_client */;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户基本信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_purchase`
--

DROP TABLE IF EXISTS `user_purchase`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  CONSTRAINT `user_purchase_ibfk_3` FOREIGN KEY (`order_id`) REFERENCES `payment_order` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户购买记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '关联更新时间（新增）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_tag` (`user_id`,`tag_id`),
  KEY `tag_id` (`tag_id`),
  CONSTRAINT `user_skill_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_skill_ibfk_2` FOREIGN KEY (`tag_id`) REFERENCES `tag` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户技能标签关联表，用于展示用户擅长的技术';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `view_log`
--

DROP TABLE IF EXISTS `view_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-07 13:47:43
