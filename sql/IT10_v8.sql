-- IT10 v8 incremental migration for code-analysis AI.
-- Target: MySQL 8.0+, database: it9_data.
-- This script is additive: it does not drop or rebuild existing business tables.

CREATE DATABASE IF NOT EXISTS `it9_data` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `it9_data`;
SET NAMES utf8mb4;

DELIMITER $$

DROP PROCEDURE IF EXISTS `it10_v8_add_column`$$
CREATE PROCEDURE `it10_v8_add_column`(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @it10_v8_sql = CONCAT(
            'ALTER TABLE `', REPLACE(p_table_name, '`', '``'), '` ADD COLUMN ',
            p_column_definition
        );
        PREPARE it10_v8_stmt FROM @it10_v8_sql;
        EXECUTE it10_v8_stmt;
        DEALLOCATE PREPARE it10_v8_stmt;
    END IF;
END$$

DROP PROCEDURE IF EXISTS `it10_v8_add_index`$$
CREATE PROCEDURE `it10_v8_add_index`(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_index_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND INDEX_NAME = p_index_name
    ) THEN
        SET @it10_v8_sql = CONCAT(
            'ALTER TABLE `', REPLACE(p_table_name, '`', '``'), '` ADD ',
            p_index_definition
        );
        PREPARE it10_v8_stmt FROM @it10_v8_sql;
        EXECUTE it10_v8_stmt;
        DEALLOCATE PREPARE it10_v8_stmt;
    END IF;
END$$

DROP PROCEDURE IF EXISTS `it10_v8_add_fk`$$
CREATE PROCEDURE `it10_v8_add_fk`(
    IN p_table_name VARCHAR(64),
    IN p_constraint_name VARCHAR(64),
    IN p_fk_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND CONSTRAINT_NAME = p_constraint_name
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ) THEN
        SET @it10_v8_sql = CONCAT(
            'ALTER TABLE `', REPLACE(p_table_name, '`', '``'), '` ADD CONSTRAINT `',
            REPLACE(p_constraint_name, '`', '``'), '` ',
            p_fk_definition
        );
        PREPARE it10_v8_stmt FROM @it10_v8_sql;
        EXECUTE it10_v8_stmt;
        DEALLOCATE PREPARE it10_v8_stmt;
    END IF;
END$$

DELIMITER ;

CREATE TABLE IF NOT EXISTS `ai_code_symbol` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `knowledge_base_id` bigint NOT NULL COMMENT 'Owning knowledge base.',
  `document_id` bigint NOT NULL COMMENT 'Source knowledge document.',
  `chunk_id` bigint DEFAULT NULL COMMENT 'Primary declaration/body chunk.',
  `project_id` bigint DEFAULT NULL COMMENT 'Related project when available.',
  `repository_id` bigint DEFAULT NULL COMMENT 'Related project_repository.',
  `branch_id` bigint DEFAULT NULL COMMENT 'Related project_branch.',
  `commit_id` bigint DEFAULT NULL COMMENT 'Related project_commit.',
  `project_file_id` bigint DEFAULT NULL COMMENT 'Related project_file.',
  `project_file_version_id` bigint DEFAULT NULL COMMENT 'Related project_file_version.',
  `language` varchar(30) DEFAULT NULL COMMENT 'Programming language.',
  `file_path` varchar(1000) NOT NULL COMMENT 'Repository-relative source file path.',
  `file_path_hash` char(64) GENERATED ALWAYS AS ((case when (`file_path` is null) then NULL else upper(sha2(`file_path`,256)) end)) STORED,
  `symbol_name` varchar(255) NOT NULL COMMENT 'Short symbol name.',
  `qualified_name` varchar(1000) NOT NULL COMMENT 'Fully qualified symbol name.',
  `qualified_name_hash` char(64) GENERATED ALWAYS AS (upper(sha2(`qualified_name`,256))) STORED,
  `symbol_kind` varchar(40) NOT NULL COMMENT 'CLASS, METHOD, FUNCTION, FIELD, MODULE, PACKAGE, etc.',
  `symbol_scope` varchar(40) DEFAULT NULL COMMENT 'GLOBAL, MODULE, CLASS, LOCAL, etc.',
  `parent_symbol_id` bigint DEFAULT NULL COMMENT 'Containing symbol for hierarchy expansion.',
  `signature` text COMMENT 'Declaration signature.',
  `return_type` varchar(255) DEFAULT NULL COMMENT 'Return or declared type.',
  `visibility` varchar(30) DEFAULT NULL COMMENT 'PUBLIC, PROTECTED, PRIVATE, INTERNAL, etc.',
  `modifiers_json` json DEFAULT NULL COMMENT 'Language-specific modifiers.',
  `start_line` int NOT NULL DEFAULT 1 COMMENT 'Declaration start line.',
  `start_column` int DEFAULT NULL COMMENT 'Declaration start column.',
  `end_line` int DEFAULT NULL COMMENT 'Declaration end line.',
  `end_column` int DEFAULT NULL COMMENT 'Declaration end column.',
  `body_start_line` int DEFAULT NULL COMMENT 'Body start line.',
  `body_end_line` int DEFAULT NULL COMMENT 'Body end line.',
  `is_declaration` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Whether this row is a declaration target.',
  `is_exported` tinyint(1) DEFAULT NULL COMMENT 'Whether this symbol is externally visible.',
  `doc_comment` text COMMENT 'Doc comment or extracted summary.',
  `symbol_key` varchar(128) DEFAULT NULL COMMENT 'Importer-provided stable dedupe key.',
  `metadata_json` json DEFAULT NULL COMMENT 'Parser and language metadata.',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, STALE, DELETED.',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_code_symbol_key` (`symbol_key`),
  KEY `idx_ai_code_symbol_kb_lookup` (`knowledge_base_id`,`language`,`symbol_kind`,`qualified_name_hash`),
  KEY `idx_ai_code_symbol_doc_range` (`document_id`,`start_line`,`end_line`),
  KEY `idx_ai_code_symbol_chunk` (`chunk_id`),
  KEY `idx_ai_code_symbol_project` (`project_id`,`repository_id`,`branch_id`,`commit_id`),
  KEY `idx_ai_code_symbol_file_decl` (`knowledge_base_id`,`file_path_hash`,`is_declaration`,`start_line`),
  KEY `idx_ai_code_symbol_parent` (`parent_symbol_id`),
  KEY `idx_ai_code_symbol_status` (`status`,`updated_at`),
  FULLTEXT KEY `ft_ai_code_symbol_text` (`symbol_name`,`qualified_name`,`signature`,`doc_comment`),
  CONSTRAINT `fk_ai_code_symbol_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ai_code_symbol_document` FOREIGN KEY (`document_id`) REFERENCES `knowledge_document` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ai_code_symbol_chunk` FOREIGN KEY (`chunk_id`) REFERENCES `knowledge_chunk` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_code_symbol_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_code_symbol_repository` FOREIGN KEY (`repository_id`) REFERENCES `project_repository` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_code_symbol_branch` FOREIGN KEY (`branch_id`) REFERENCES `project_branch` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_code_symbol_commit` FOREIGN KEY (`commit_id`) REFERENCES `project_commit` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_code_symbol_project_file` FOREIGN KEY (`project_file_id`) REFERENCES `project_file` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_code_symbol_project_file_version` FOREIGN KEY (`project_file_version_id`) REFERENCES `project_file_version` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_code_symbol_parent` FOREIGN KEY (`parent_symbol_id`) REFERENCES `ai_code_symbol` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Code symbols for declaration-first retrieval and graph expansion.';

CREATE TABLE IF NOT EXISTS `ai_code_reference` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `knowledge_base_id` bigint NOT NULL COMMENT 'Owning knowledge base.',
  `project_id` bigint DEFAULT NULL COMMENT 'Related project when available.',
  `repository_id` bigint DEFAULT NULL COMMENT 'Related project_repository.',
  `branch_id` bigint DEFAULT NULL COMMENT 'Related project_branch.',
  `commit_id` bigint DEFAULT NULL COMMENT 'Related project_commit.',
  `from_symbol_id` bigint DEFAULT NULL COMMENT 'Source symbol.',
  `to_symbol_id` bigint DEFAULT NULL COMMENT 'Resolved target symbol.',
  `from_document_id` bigint NOT NULL COMMENT 'Source document.',
  `from_chunk_id` bigint DEFAULT NULL COMMENT 'Source chunk.',
  `to_document_id` bigint DEFAULT NULL COMMENT 'Resolved target document.',
  `to_chunk_id` bigint DEFAULT NULL COMMENT 'Resolved target chunk.',
  `ref_kind` varchar(40) NOT NULL COMMENT 'CALL, IMPORT, INHERIT, IMPLEMENT, READ, WRITE, TYPE, etc.',
  `ref_name` varchar(255) DEFAULT NULL COMMENT 'Reference text in source.',
  `target_qualified_name` varchar(1000) DEFAULT NULL COMMENT 'Unresolved or resolved target qualified name.',
  `target_qualified_name_hash` char(64) GENERATED ALWAYS AS ((case when (`target_qualified_name` is null) then NULL else upper(sha2(`target_qualified_name`,256)) end)) STORED,
  `from_file_path` varchar(1000) DEFAULT NULL COMMENT 'Repository-relative source path.',
  `from_file_path_hash` char(64) GENERATED ALWAYS AS ((case when (`from_file_path` is null) then NULL else upper(sha2(`from_file_path`,256)) end)) STORED,
  `start_line` int DEFAULT NULL COMMENT 'Reference start line.',
  `start_column` int DEFAULT NULL COMMENT 'Reference start column.',
  `end_line` int DEFAULT NULL COMMENT 'Reference end line.',
  `end_column` int DEFAULT NULL COMMENT 'Reference end column.',
  `resolution_status` varchar(30) NOT NULL DEFAULT 'UNRESOLVED' COMMENT 'RESOLVED, UNRESOLVED, AMBIGUOUS, EXTERNAL.',
  `confidence` decimal(8,6) DEFAULT NULL COMMENT 'Resolution confidence.',
  `reference_key` varchar(128) DEFAULT NULL COMMENT 'Importer-provided stable dedupe key.',
  `metadata_json` json DEFAULT NULL COMMENT 'Parser and graph metadata.',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, STALE, DELETED.',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_code_reference_key` (`reference_key`),
  KEY `idx_ai_code_reference_from` (`from_symbol_id`,`ref_kind`),
  KEY `idx_ai_code_reference_to` (`to_symbol_id`,`ref_kind`),
  KEY `idx_ai_code_reference_graph` (`knowledge_base_id`,`from_symbol_id`,`to_symbol_id`),
  KEY `idx_ai_code_reference_target` (`knowledge_base_id`,`target_qualified_name_hash`,`resolution_status`),
  KEY `idx_ai_code_reference_doc` (`from_document_id`,`from_chunk_id`),
  KEY `idx_ai_code_reference_file` (`knowledge_base_id`,`from_file_path_hash`,`start_line`),
  KEY `idx_ai_code_reference_project` (`project_id`,`repository_id`,`branch_id`,`commit_id`),
  KEY `idx_ai_code_reference_status` (`status`,`updated_at`),
  FULLTEXT KEY `ft_ai_code_reference_text` (`ref_name`,`target_qualified_name`),
  CONSTRAINT `fk_ai_code_reference_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ai_code_reference_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_code_reference_repository` FOREIGN KEY (`repository_id`) REFERENCES `project_repository` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_code_reference_branch` FOREIGN KEY (`branch_id`) REFERENCES `project_branch` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_code_reference_commit` FOREIGN KEY (`commit_id`) REFERENCES `project_commit` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_code_reference_from_symbol` FOREIGN KEY (`from_symbol_id`) REFERENCES `ai_code_symbol` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_code_reference_to_symbol` FOREIGN KEY (`to_symbol_id`) REFERENCES `ai_code_symbol` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_code_reference_from_doc` FOREIGN KEY (`from_document_id`) REFERENCES `knowledge_document` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ai_code_reference_from_chunk` FOREIGN KEY (`from_chunk_id`) REFERENCES `knowledge_chunk` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_code_reference_to_doc` FOREIGN KEY (`to_document_id`) REFERENCES `knowledge_document` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ai_code_reference_to_chunk` FOREIGN KEY (`to_chunk_id`) REFERENCES `knowledge_chunk` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Code reference edges for graph expansion and strict grounding.';

CALL `it10_v8_add_column`('ai_session', 'analysis_profile', '`analysis_profile` varchar(50) DEFAULT NULL COMMENT ''AI analysis profile, e.g. CODE_ANALYSIS.''');
CALL `it10_v8_add_column`('ai_session', 'repository_id', '`repository_id` bigint DEFAULT NULL COMMENT ''Related project_repository for code sessions.''');
CALL `it10_v8_add_column`('ai_session', 'branch_id', '`branch_id` bigint DEFAULT NULL COMMENT ''Related project_branch for code sessions.''');
CALL `it10_v8_add_column`('ai_session', 'commit_id', '`commit_id` bigint DEFAULT NULL COMMENT ''Related project_commit snapshot for code sessions.''');
CALL `it10_v8_add_column`('ai_session', 'retrieval_policy_json', '`retrieval_policy_json` json DEFAULT NULL COMMENT ''Per-session retrieval policy.''');
CALL `it10_v8_add_column`('ai_session', 'grounding_policy_json', '`grounding_policy_json` json DEFAULT NULL COMMENT ''Per-session grounding policy.''');
CALL `it10_v8_add_column`('ai_session', 'context_snapshot_json', '`context_snapshot_json` json DEFAULT NULL COMMENT ''Pinned code/context snapshot.''');
CALL `it10_v8_add_column`('ai_session', 'last_retrieval_summary_json', '`last_retrieval_summary_json` json DEFAULT NULL COMMENT ''Last retrieval summary for UI/debug.''');

CALL `it10_v8_add_column`('ai_message', 'parent_message_id', '`parent_message_id` bigint DEFAULT NULL COMMENT ''Parent assistant message for streamed partial rows.''');
CALL `it10_v8_add_column`('ai_message', 'turn_index', '`turn_index` int DEFAULT NULL COMMENT ''Conversation turn index.''');
CALL `it10_v8_add_column`('ai_message', 'content_format', '`content_format` varchar(30) NOT NULL DEFAULT ''TEXT'' COMMENT ''TEXT, MARKDOWN, JSON, TOOL_RESULT.''');
CALL `it10_v8_add_column`('ai_message', 'stream_state', '`stream_state` varchar(20) NOT NULL DEFAULT ''FINAL'' COMMENT ''FINAL, PARTIAL, DELTA, CANCELLED.''');
CALL `it10_v8_add_column`('ai_message', 'partial_seq', '`partial_seq` int NOT NULL DEFAULT 0 COMMENT ''Streaming partial sequence.''');
CALL `it10_v8_add_column`('ai_message', 'delta_content', '`delta_content` mediumtext DEFAULT NULL COMMENT ''Delta payload for streamed partial answer.''');
CALL `it10_v8_add_column`('ai_message', 'retrieval_summary_json', '`retrieval_summary_json` json DEFAULT NULL COMMENT ''Retrieval summary used for this message.''');
CALL `it10_v8_add_column`('ai_message', 'grounding_status', '`grounding_status` varchar(30) DEFAULT NULL COMMENT ''STRICT_PASS, STRICT_FAIL, PARTIAL, NOT_CHECKED.''');
CALL `it10_v8_add_column`('ai_message', 'grounding_json', '`grounding_json` json DEFAULT NULL COMMENT ''Grounding evidence and violations.''');
CALL `it10_v8_add_column`('ai_message', 'citation_json', '`citation_json` json DEFAULT NULL COMMENT ''Structured citations to chunks/symbols/references.''');
CALL `it10_v8_add_column`('ai_message', 'updated_at', '`updated_at` timestamp NULL DEFAULT NULL COMMENT ''Message update time for streaming.''');

CALL `it10_v8_add_column`('ai_call_log', 'request_id', '`request_id` varchar(64) DEFAULT NULL COMMENT ''External or generated request id.''');
CALL `it10_v8_add_column`('ai_call_log', 'trace_id', '`trace_id` varchar(64) DEFAULT NULL COMMENT ''Trace id across retrieval/model/streaming.''');
CALL `it10_v8_add_column`('ai_call_log', 'repository_id', '`repository_id` bigint DEFAULT NULL COMMENT ''Related project_repository for code calls.''');
CALL `it10_v8_add_column`('ai_call_log', 'branch_id', '`branch_id` bigint DEFAULT NULL COMMENT ''Related project_branch for code calls.''');
CALL `it10_v8_add_column`('ai_call_log', 'commit_id', '`commit_id` bigint DEFAULT NULL COMMENT ''Related project_commit snapshot for code calls.''');
CALL `it10_v8_add_column`('ai_call_log', 'request_stage', '`request_stage` varchar(30) DEFAULT NULL COMMENT ''PLAN, RETRIEVE, RERANK, GROUND, GENERATE, STREAM.''');
CALL `it10_v8_add_column`('ai_call_log', 'retrieval_plan_json', '`retrieval_plan_json` json DEFAULT NULL COMMENT ''Retrieval plan including declaration-first and graph expansion.''');
CALL `it10_v8_add_column`('ai_call_log', 'retrieval_summary_json', '`retrieval_summary_json` json DEFAULT NULL COMMENT ''Retrieval summary and selected evidence.''');
CALL `it10_v8_add_column`('ai_call_log', 'grounding_report_json', '`grounding_report_json` json DEFAULT NULL COMMENT ''Strict grounding result.''');
CALL `it10_v8_add_column`('ai_call_log', 'degrade_reason', '`degrade_reason` varchar(500) DEFAULT NULL COMMENT ''Reason for degraded retrieval/generation.''');
CALL `it10_v8_add_column`('ai_call_log', 'stream_started_at', '`stream_started_at` timestamp NULL DEFAULT NULL COMMENT ''Streaming start time.''');
CALL `it10_v8_add_column`('ai_call_log', 'stream_finished_at', '`stream_finished_at` timestamp NULL DEFAULT NULL COMMENT ''Streaming finish time.''');
CALL `it10_v8_add_column`('ai_call_log', 'first_token_latency_ms', '`first_token_latency_ms` int DEFAULT NULL COMMENT ''First-token latency in ms.''');
CALL `it10_v8_add_column`('ai_call_log', 'metadata_json', '`metadata_json` json DEFAULT NULL COMMENT ''Provider/runtime metadata.''');

CALL `it10_v8_add_column`('ai_retrieval_log', 'session_id', '`session_id` bigint DEFAULT NULL COMMENT ''Backfilled from call log for faster lookup.''');
CALL `it10_v8_add_column`('ai_retrieval_log', 'message_id', '`message_id` bigint DEFAULT NULL COMMENT ''Backfilled from call log for faster lookup.''');
CALL `it10_v8_add_column`('ai_retrieval_log', 'stage_code', '`stage_code` varchar(30) NOT NULL DEFAULT ''RECALL'' COMMENT ''PLAN, RECALL, DECLARATION_FIRST, GRAPH_EXPAND, RERANK, GROUND.''');
CALL `it10_v8_add_column`('ai_retrieval_log', 'stage_order', '`stage_order` int DEFAULT NULL COMMENT ''Order within a retrieval pipeline.''');
CALL `it10_v8_add_column`('ai_retrieval_log', 'query_variant', '`query_variant` text DEFAULT NULL COMMENT ''Expanded or rewritten query variant.''');
CALL `it10_v8_add_column`('ai_retrieval_log', 'plan_step_json', '`plan_step_json` json DEFAULT NULL COMMENT ''Retrieval plan step details.''');
CALL `it10_v8_add_column`('ai_retrieval_log', 'hit_reason_json', '`hit_reason_json` json DEFAULT NULL COMMENT ''Hit reason payload.''');
CALL `it10_v8_add_column`('ai_retrieval_log', 'score_detail_json', '`score_detail_json` json DEFAULT NULL COMMENT ''Score detail payload.''');
CALL `it10_v8_add_column`('ai_retrieval_log', 'candidate_source', '`candidate_source` varchar(30) DEFAULT NULL COMMENT ''CHUNK, DOCUMENT, SYMBOL, REFERENCE.''');
CALL `it10_v8_add_column`('ai_retrieval_log', 'symbol_id', '`symbol_id` bigint DEFAULT NULL COMMENT ''Candidate code symbol.''');
CALL `it10_v8_add_column`('ai_retrieval_log', 'reference_id', '`reference_id` bigint DEFAULT NULL COMMENT ''Candidate code reference edge.''');
CALL `it10_v8_add_column`('ai_retrieval_log', 'score_vector', '`score_vector` decimal(10,6) DEFAULT NULL COMMENT ''Vector recall score.''');
CALL `it10_v8_add_column`('ai_retrieval_log', 'score_keyword', '`score_keyword` decimal(10,6) DEFAULT NULL COMMENT ''Keyword/BM25 score.''');
CALL `it10_v8_add_column`('ai_retrieval_log', 'score_graph', '`score_graph` decimal(10,6) DEFAULT NULL COMMENT ''Graph expansion score.''');
CALL `it10_v8_add_column`('ai_retrieval_log', 'score_rerank', '`score_rerank` decimal(10,6) DEFAULT NULL COMMENT ''Rerank score.''');
CALL `it10_v8_add_column`('ai_retrieval_log', 'rerank_model', '`rerank_model` varchar(100) DEFAULT NULL COMMENT ''Rerank model name.''');
CALL `it10_v8_add_column`('ai_retrieval_log', 'grounding_status', '`grounding_status` varchar(30) DEFAULT NULL COMMENT ''Evidence grounding status.''');
CALL `it10_v8_add_column`('ai_retrieval_log', 'grounding_evidence_json', '`grounding_evidence_json` json DEFAULT NULL COMMENT ''Grounding evidence payload.''');
CALL `it10_v8_add_column`('ai_retrieval_log', 'degrade_reason', '`degrade_reason` varchar(500) DEFAULT NULL COMMENT ''Retrieval degradation reason.''');
CALL `it10_v8_add_column`('ai_retrieval_log', 'metadata_json', '`metadata_json` json DEFAULT NULL COMMENT ''Retriever metadata.''');

CALL `it10_v8_add_column`('knowledge_document', 'project_id', '`project_id` bigint DEFAULT NULL COMMENT ''Related project for code documents.''');
CALL `it10_v8_add_column`('knowledge_document', 'repository_id', '`repository_id` bigint DEFAULT NULL COMMENT ''Related project_repository.''');
CALL `it10_v8_add_column`('knowledge_document', 'branch_id', '`branch_id` bigint DEFAULT NULL COMMENT ''Related project_branch.''');
CALL `it10_v8_add_column`('knowledge_document', 'commit_id', '`commit_id` bigint DEFAULT NULL COMMENT ''Related project_commit.''');
CALL `it10_v8_add_column`('knowledge_document', 'project_file_id', '`project_file_id` bigint DEFAULT NULL COMMENT ''Related project_file.''');
CALL `it10_v8_add_column`('knowledge_document', 'project_file_version_id', '`project_file_version_id` bigint DEFAULT NULL COMMENT ''Related project_file_version.''');
CALL `it10_v8_add_column`('knowledge_document', 'doc_kind', '`doc_kind` varchar(30) NOT NULL DEFAULT ''DOCUMENT'' COMMENT ''DOCUMENT, CODE_FILE, README, API_DOC.''');
CALL `it10_v8_add_column`('knowledge_document', 'file_path', '`file_path` varchar(1000) DEFAULT NULL COMMENT ''Canonical source path for code/document retrieval.''');
CALL `it10_v8_add_column`('knowledge_document', 'file_path_hash', '`file_path_hash` char(64) GENERATED ALWAYS AS ((case when (`file_path` is null) then NULL else upper(sha2(`file_path`,256)) end)) STORED COMMENT ''SHA-256 hash of file_path.''');
CALL `it10_v8_add_column`('knowledge_document', 'parser_name', '`parser_name` varchar(100) DEFAULT NULL COMMENT ''Parser used for code/document extraction.''');
CALL `it10_v8_add_column`('knowledge_document', 'parser_version', '`parser_version` varchar(50) DEFAULT NULL COMMENT ''Parser version.''');
CALL `it10_v8_add_column`('knowledge_document', 'parse_status', '`parse_status` varchar(30) NOT NULL DEFAULT ''PENDING'' COMMENT ''PENDING, PARSED, FAILED, NOT_APPLICABLE.''');
CALL `it10_v8_add_column`('knowledge_document', 'symbol_index_status', '`symbol_index_status` varchar(30) NOT NULL DEFAULT ''PENDING'' COMMENT ''PENDING, INDEXED, FAILED, NOT_APPLICABLE.''');
CALL `it10_v8_add_column`('knowledge_document', 'symbol_count', '`symbol_count` int NOT NULL DEFAULT 0 COMMENT ''Indexed symbol count.''');
CALL `it10_v8_add_column`('knowledge_document', 'reference_count', '`reference_count` int NOT NULL DEFAULT 0 COMMENT ''Indexed reference count.''');
CALL `it10_v8_add_column`('knowledge_document', 'metadata_json', '`metadata_json` json DEFAULT NULL COMMENT ''Document parser metadata.''');
CALL `it10_v8_add_column`('knowledge_document', 'source_updated_at', '`source_updated_at` timestamp NULL DEFAULT NULL COMMENT ''Source file update time.''');

CALL `it10_v8_add_column`('knowledge_chunk', 'chunk_type', '`chunk_type` varchar(30) NOT NULL DEFAULT ''TEXT'' COMMENT ''TEXT, CODE_BLOCK, SYMBOL_DECL, SYMBOL_BODY.''');
CALL `it10_v8_add_column`('knowledge_chunk', 'file_path', '`file_path` varchar(1000) DEFAULT NULL COMMENT ''Source file path inherited from document.''');
CALL `it10_v8_add_column`('knowledge_chunk', 'file_path_hash', '`file_path_hash` char(64) GENERATED ALWAYS AS ((case when (`file_path` is null) then NULL else upper(sha2(`file_path`,256)) end)) STORED COMMENT ''SHA-256 hash of file_path.''');
CALL `it10_v8_add_column`('knowledge_chunk', 'start_line', '`start_line` int DEFAULT NULL COMMENT ''Chunk start line for code/document grounding.''');
CALL `it10_v8_add_column`('knowledge_chunk', 'start_column', '`start_column` int DEFAULT NULL COMMENT ''Chunk start column.''');
CALL `it10_v8_add_column`('knowledge_chunk', 'end_line', '`end_line` int DEFAULT NULL COMMENT ''Chunk end line for code/document grounding.''');
CALL `it10_v8_add_column`('knowledge_chunk', 'end_column', '`end_column` int DEFAULT NULL COMMENT ''Chunk end column.''');
CALL `it10_v8_add_column`('knowledge_chunk', 'section_path', '`section_path` varchar(500) DEFAULT NULL COMMENT ''Heading or code scope path.''');
CALL `it10_v8_add_column`('knowledge_chunk', 'primary_symbol_id', '`primary_symbol_id` bigint DEFAULT NULL COMMENT ''Primary code symbol represented by this chunk.''');
CALL `it10_v8_add_column`('knowledge_chunk', 'content_hash', '`content_hash` char(64) DEFAULT NULL COMMENT ''SHA-256 hash of chunk content.''');
CALL `it10_v8_add_column`('knowledge_chunk', 'rank_boost', '`rank_boost` decimal(8,4) DEFAULT NULL COMMENT ''Manual or parser-provided rank boost.''');
CALL `it10_v8_add_column`('knowledge_chunk', 'retrieval_tags', '`retrieval_tags` json DEFAULT NULL COMMENT ''Tags used by retrieval filters.''');
CALL `it10_v8_add_column`('knowledge_chunk', 'updated_at', '`updated_at` timestamp NULL DEFAULT NULL COMMENT ''Chunk update time.''');

CALL `it10_v8_add_index`('ai_session', 'idx_ai_session_project_scene', 'INDEX `idx_ai_session_project_scene` (`project_id`,`scene_code`,`status`,`updated_at`)');
CALL `it10_v8_add_index`('ai_session', 'idx_ai_session_repo_branch', 'INDEX `idx_ai_session_repo_branch` (`repository_id`,`branch_id`,`commit_id`)');
CALL `it10_v8_add_index`('ai_message', 'idx_ai_message_parent_stream', 'INDEX `idx_ai_message_parent_stream` (`parent_message_id`,`stream_state`,`partial_seq`)');
CALL `it10_v8_add_index`('ai_message', 'idx_ai_message_session_turn', 'INDEX `idx_ai_message_session_turn` (`session_id`,`turn_index`,`created_at`)');
CALL `it10_v8_add_index`('ai_message', 'idx_ai_message_grounding', 'INDEX `idx_ai_message_grounding` (`grounding_status`,`created_at`)');
CALL `it10_v8_add_index`('ai_call_log', 'idx_ai_call_log_trace', 'INDEX `idx_ai_call_log_trace` (`trace_id`,`created_at`)');
CALL `it10_v8_add_index`('ai_call_log', 'idx_ai_call_log_request_id', 'INDEX `idx_ai_call_log_request_id` (`request_id`)');
CALL `it10_v8_add_index`('ai_call_log', 'idx_ai_call_log_stage_status', 'INDEX `idx_ai_call_log_stage_status` (`request_stage`,`status`,`created_at`)');
CALL `it10_v8_add_index`('ai_call_log', 'idx_ai_call_log_repo', 'INDEX `idx_ai_call_log_repo` (`repository_id`,`branch_id`,`commit_id`)');
CALL `it10_v8_add_index`('ai_retrieval_log', 'idx_ai_retrieval_stage_rank', 'INDEX `idx_ai_retrieval_stage_rank` (`call_log_id`,`stage_code`,`stage_order`,`rank_no`)');
CALL `it10_v8_add_index`('ai_retrieval_log', 'idx_ai_retrieval_session_message', 'INDEX `idx_ai_retrieval_session_message` (`session_id`,`message_id`,`created_at`)');
CALL `it10_v8_add_index`('ai_retrieval_log', 'idx_ai_retrieval_symbol', 'INDEX `idx_ai_retrieval_symbol` (`symbol_id`,`stage_code`,`rank_no`)');
CALL `it10_v8_add_index`('ai_retrieval_log', 'idx_ai_retrieval_reference', 'INDEX `idx_ai_retrieval_reference` (`reference_id`,`stage_code`,`rank_no`)');
CALL `it10_v8_add_index`('ai_retrieval_log', 'idx_ai_retrieval_candidate', 'INDEX `idx_ai_retrieval_candidate` (`candidate_source`,`retrieval_method`,`score_rerank`)');
CALL `it10_v8_add_index`('knowledge_document', 'idx_kd_code_scope', 'INDEX `idx_kd_code_scope` (`knowledge_base_id`,`doc_kind`,`status`,`indexed_at`)');
CALL `it10_v8_add_index`('knowledge_document', 'idx_kd_repo_file_hash', 'INDEX `idx_kd_repo_file_hash` (`repository_id`,`file_path_hash`,`commit_id`)');
CALL `it10_v8_add_index`('knowledge_document', 'idx_kd_project_file_version', 'INDEX `idx_kd_project_file_version` (`project_id`,`project_file_id`,`project_file_version_id`)');
CALL `it10_v8_add_index`('knowledge_document', 'idx_kd_symbol_status', 'INDEX `idx_kd_symbol_status` (`symbol_index_status`,`parse_status`,`updated_at`)');
CALL `it10_v8_add_index`('knowledge_chunk', 'idx_kc_doc_line', 'INDEX `idx_kc_doc_line` (`document_id`,`start_line`,`end_line`)');
CALL `it10_v8_add_index`('knowledge_chunk', 'idx_kc_file_line', 'INDEX `idx_kc_file_line` (`knowledge_base_id`,`file_path_hash`,`start_line`)');
CALL `it10_v8_add_index`('knowledge_chunk', 'idx_kc_chunk_type', 'INDEX `idx_kc_chunk_type` (`knowledge_base_id`,`chunk_type`,`chunk_index`)');
CALL `it10_v8_add_index`('knowledge_chunk', 'idx_kc_primary_symbol', 'INDEX `idx_kc_primary_symbol` (`primary_symbol_id`)');
CALL `it10_v8_add_index`('knowledge_chunk', 'idx_kc_content_hash', 'INDEX `idx_kc_content_hash` (`content_hash`)');

CALL `it10_v8_add_fk`('ai_session', 'fk_ai_session_repository', 'FOREIGN KEY (`repository_id`) REFERENCES `project_repository` (`id`) ON DELETE SET NULL');
CALL `it10_v8_add_fk`('ai_session', 'fk_ai_session_branch', 'FOREIGN KEY (`branch_id`) REFERENCES `project_branch` (`id`) ON DELETE SET NULL');
CALL `it10_v8_add_fk`('ai_session', 'fk_ai_session_commit', 'FOREIGN KEY (`commit_id`) REFERENCES `project_commit` (`id`) ON DELETE SET NULL');
CALL `it10_v8_add_fk`('ai_message', 'fk_ai_message_parent', 'FOREIGN KEY (`parent_message_id`) REFERENCES `ai_message` (`id`) ON DELETE SET NULL');
CALL `it10_v8_add_fk`('ai_call_log', 'fk_ai_call_log_repository', 'FOREIGN KEY (`repository_id`) REFERENCES `project_repository` (`id`) ON DELETE SET NULL');
CALL `it10_v8_add_fk`('ai_call_log', 'fk_ai_call_log_branch', 'FOREIGN KEY (`branch_id`) REFERENCES `project_branch` (`id`) ON DELETE SET NULL');
CALL `it10_v8_add_fk`('ai_call_log', 'fk_ai_call_log_commit', 'FOREIGN KEY (`commit_id`) REFERENCES `project_commit` (`id`) ON DELETE SET NULL');
CALL `it10_v8_add_fk`('ai_retrieval_log', 'fk_ai_retrieval_log_session', 'FOREIGN KEY (`session_id`) REFERENCES `ai_session` (`id`) ON DELETE SET NULL');
CALL `it10_v8_add_fk`('ai_retrieval_log', 'fk_ai_retrieval_log_message', 'FOREIGN KEY (`message_id`) REFERENCES `ai_message` (`id`) ON DELETE SET NULL');
CALL `it10_v8_add_fk`('ai_retrieval_log', 'fk_ai_retrieval_log_symbol', 'FOREIGN KEY (`symbol_id`) REFERENCES `ai_code_symbol` (`id`) ON DELETE SET NULL');
CALL `it10_v8_add_fk`('ai_retrieval_log', 'fk_ai_retrieval_log_reference', 'FOREIGN KEY (`reference_id`) REFERENCES `ai_code_reference` (`id`) ON DELETE SET NULL');
CALL `it10_v8_add_fk`('knowledge_document', 'fk_kd_project', 'FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE SET NULL');
CALL `it10_v8_add_fk`('knowledge_document', 'fk_kd_repository', 'FOREIGN KEY (`repository_id`) REFERENCES `project_repository` (`id`) ON DELETE SET NULL');
CALL `it10_v8_add_fk`('knowledge_document', 'fk_kd_branch', 'FOREIGN KEY (`branch_id`) REFERENCES `project_branch` (`id`) ON DELETE SET NULL');
CALL `it10_v8_add_fk`('knowledge_document', 'fk_kd_commit', 'FOREIGN KEY (`commit_id`) REFERENCES `project_commit` (`id`) ON DELETE SET NULL');
CALL `it10_v8_add_fk`('knowledge_document', 'fk_kd_project_file', 'FOREIGN KEY (`project_file_id`) REFERENCES `project_file` (`id`) ON DELETE SET NULL');
CALL `it10_v8_add_fk`('knowledge_document', 'fk_kd_project_file_version', 'FOREIGN KEY (`project_file_version_id`) REFERENCES `project_file_version` (`id`) ON DELETE SET NULL');
CALL `it10_v8_add_fk`('knowledge_chunk', 'fk_kc_primary_symbol', 'FOREIGN KEY (`primary_symbol_id`) REFERENCES `ai_code_symbol` (`id`) ON DELETE SET NULL');

UPDATE `ai_message`
SET `content_format` = COALESCE(`content_format`, 'TEXT'),
    `stream_state` = COALESCE(`stream_state`, 'FINAL'),
    `partial_seq` = COALESCE(`partial_seq`, 0)
WHERE `content_format` IS NULL
   OR `stream_state` IS NULL
   OR `partial_seq` IS NULL;

UPDATE `ai_session` s
SET s.`last_message_at` = (
    SELECT MAX(m.`created_at`)
    FROM `ai_message` m
    WHERE m.`session_id` = s.`id`
)
WHERE s.`last_message_at` IS NULL
  AND EXISTS (
      SELECT 1
      FROM `ai_message` m2
      WHERE m2.`session_id` = s.`id`
  );

UPDATE `ai_call_log` l
JOIN `ai_session` s ON s.`id` = l.`session_id`
SET l.`project_id` = COALESCE(l.`project_id`, s.`project_id`),
    l.`repository_id` = COALESCE(l.`repository_id`, s.`repository_id`),
    l.`branch_id` = COALESCE(l.`branch_id`, s.`branch_id`),
    l.`commit_id` = COALESCE(l.`commit_id`, s.`commit_id`)
WHERE l.`session_id` IS NOT NULL
  AND (
      l.`project_id` IS NULL
      OR l.`repository_id` IS NULL
      OR l.`branch_id` IS NULL
      OR l.`commit_id` IS NULL
  );

UPDATE `ai_retrieval_log` r
JOIN `ai_call_log` l ON l.`id` = r.`call_log_id`
SET r.`session_id` = COALESCE(r.`session_id`, l.`session_id`),
    r.`message_id` = COALESCE(r.`message_id`, l.`message_id`),
    r.`stage_code` = COALESCE(r.`stage_code`, 'RECALL'),
    r.`candidate_source` = COALESCE(r.`candidate_source`, CASE WHEN r.`chunk_id` IS NULL THEN NULL ELSE 'CHUNK' END)
WHERE r.`session_id` IS NULL
   OR r.`message_id` IS NULL
   OR r.`stage_code` IS NULL
   OR r.`candidate_source` IS NULL;

UPDATE `knowledge_document`
SET `file_path` = COALESCE(`file_path`, `archive_entry_path`, `file_name`, `source_url`, `storage_path`),
    `doc_kind` = CASE
        WHEN `doc_kind` IS NOT NULL AND `doc_kind` <> 'DOCUMENT' THEN `doc_kind`
        WHEN LOWER(COALESCE(`archive_entry_path`, `file_name`, `source_url`, `storage_path`, '')) REGEXP '\\.(java|kt|kts|scala|groovy|js|jsx|ts|tsx|vue|py|go|rs|c|cc|cpp|cxx|h|hpp|cs|php|rb|swift|sql|xml|yaml|yml|json|html|css|scss|less)$' THEN 'CODE_FILE'
        WHEN LOWER(COALESCE(`archive_entry_path`, `file_name`, `source_url`, `storage_path`, '')) REGEXP '(^|/)(readme|license|changelog)(\\.|$)' THEN 'README'
        ELSE COALESCE(`doc_kind`, 'DOCUMENT')
    END,
    `parse_status` = COALESCE(`parse_status`, 'PENDING'),
    `symbol_index_status` = COALESCE(`symbol_index_status`, 'PENDING')
WHERE `file_path` IS NULL
   OR `doc_kind` IS NULL
   OR `parse_status` IS NULL
   OR `symbol_index_status` IS NULL;

UPDATE `knowledge_chunk` kc
JOIN `knowledge_document` kd ON kd.`id` = kc.`document_id`
SET kc.`file_path` = COALESCE(kc.`file_path`, kd.`file_path`),
    kc.`chunk_type` = CASE
        WHEN kc.`chunk_type` IS NOT NULL AND kc.`chunk_type` <> 'TEXT' THEN kc.`chunk_type`
        WHEN kd.`doc_kind` = 'CODE_FILE' THEN 'CODE_BLOCK'
        ELSE COALESCE(kc.`chunk_type`, 'TEXT')
    END,
    kc.`content_hash` = COALESCE(kc.`content_hash`, upper(sha2(kc.`content`, 256)))
WHERE kc.`file_path` IS NULL
   OR kc.`chunk_type` IS NULL
   OR kc.`content_hash` IS NULL;

UPDATE `knowledge_document` kd
LEFT JOIN (
    SELECT `document_id`, COUNT(*) AS `symbol_count`
    FROM `ai_code_symbol`
    WHERE `status` = 'ACTIVE'
    GROUP BY `document_id`
) s ON s.`document_id` = kd.`id`
LEFT JOIN (
    SELECT `from_document_id`, COUNT(*) AS `reference_count`
    FROM `ai_code_reference`
    WHERE `status` = 'ACTIVE'
    GROUP BY `from_document_id`
) r ON r.`from_document_id` = kd.`id`
SET kd.`symbol_count` = COALESCE(s.`symbol_count`, kd.`symbol_count`, 0),
    kd.`reference_count` = COALESCE(r.`reference_count`, kd.`reference_count`, 0)
WHERE kd.`symbol_count` IS NULL
   OR kd.`reference_count` IS NULL
   OR s.`symbol_count` IS NOT NULL
   OR r.`reference_count` IS NOT NULL;

DROP PROCEDURE IF EXISTS `it10_v8_add_fk`;
DROP PROCEDURE IF EXISTS `it10_v8_add_index`;
DROP PROCEDURE IF EXISTS `it10_v8_add_column`;

-- Validation SQL: expected v8_new_tables.found_count = 2.
SELECT 'v8_new_tables' AS `check_name`,
       COUNT(*) AS `found_count`
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('ai_code_symbol', 'ai_code_reference');

-- Validation SQL: expected missing_count = 0.
SELECT 'v8_required_columns_missing' AS `check_name`,
       COUNT(*) AS `missing_count`
FROM (
    SELECT 'ai_session' AS table_name, 'retrieval_policy_json' AS column_name
    UNION ALL SELECT 'ai_session', 'grounding_policy_json'
    UNION ALL SELECT 'ai_message', 'stream_state'
    UNION ALL SELECT 'ai_message', 'grounding_json'
    UNION ALL SELECT 'ai_call_log', 'retrieval_plan_json'
    UNION ALL SELECT 'ai_call_log', 'degrade_reason'
    UNION ALL SELECT 'ai_retrieval_log', 'stage_code'
    UNION ALL SELECT 'ai_retrieval_log', 'symbol_id'
    UNION ALL SELECT 'knowledge_document', 'doc_kind'
    UNION ALL SELECT 'knowledge_document', 'file_path_hash'
    UNION ALL SELECT 'knowledge_chunk', 'chunk_type'
    UNION ALL SELECT 'knowledge_chunk', 'primary_symbol_id'
) required_columns
LEFT JOIN information_schema.COLUMNS c
  ON c.TABLE_SCHEMA = DATABASE()
 AND c.TABLE_NAME = required_columns.table_name
 AND c.COLUMN_NAME = required_columns.column_name
WHERE c.COLUMN_NAME IS NULL;

-- Validation SQL: expected missing_count = 0.
SELECT 'v8_required_indexes_missing' AS `check_name`,
       COUNT(*) AS `missing_count`
FROM (
    SELECT 'ai_code_symbol' AS table_name, 'idx_ai_code_symbol_kb_lookup' AS index_name
    UNION ALL SELECT 'ai_code_reference', 'idx_ai_code_reference_graph'
    UNION ALL SELECT 'ai_session', 'idx_ai_session_repo_branch'
    UNION ALL SELECT 'ai_message', 'idx_ai_message_parent_stream'
    UNION ALL SELECT 'ai_call_log', 'idx_ai_call_log_trace'
    UNION ALL SELECT 'ai_retrieval_log', 'idx_ai_retrieval_stage_rank'
    UNION ALL SELECT 'knowledge_document', 'idx_kd_repo_file_hash'
    UNION ALL SELECT 'knowledge_chunk', 'idx_kc_file_line'
) required_indexes
LEFT JOIN information_schema.STATISTICS s
  ON s.TABLE_SCHEMA = DATABASE()
 AND s.TABLE_NAME = required_indexes.table_name
 AND s.INDEX_NAME = required_indexes.index_name
WHERE s.INDEX_NAME IS NULL;

-- Validation SQL: expected missing_count = 0.
SELECT 'v8_required_foreign_keys_missing' AS `check_name`,
       COUNT(*) AS `missing_count`
FROM (
    SELECT 'fk_ai_code_symbol_document' AS constraint_name
    UNION ALL SELECT 'fk_ai_code_reference_from_doc'
    UNION ALL SELECT 'fk_ai_retrieval_log_symbol'
    UNION ALL SELECT 'fk_ai_retrieval_log_reference'
    UNION ALL SELECT 'fk_kc_primary_symbol'
) required_fks
LEFT JOIN information_schema.TABLE_CONSTRAINTS tc
  ON tc.CONSTRAINT_SCHEMA = DATABASE()
 AND tc.CONSTRAINT_NAME = required_fks.constraint_name
 AND tc.CONSTRAINT_TYPE = 'FOREIGN KEY'
WHERE tc.CONSTRAINT_NAME IS NULL;

-- Validation SQL: expected invalid_count = 0.
SELECT 'v8_retrieval_log_invalid_symbol_refs' AS `check_name`,
       COUNT(*) AS `invalid_count`
FROM `ai_retrieval_log` r
LEFT JOIN `ai_code_symbol` s ON s.`id` = r.`symbol_id`
WHERE r.`symbol_id` IS NOT NULL
  AND s.`id` IS NULL;

-- Validation SQL: expected null_count = 0.
SELECT 'v8_stream_columns_null_count' AS `check_name`,
       COUNT(*) AS `null_count`
FROM `ai_message`
WHERE `stream_state` IS NULL
   OR `content_format` IS NULL
   OR `partial_seq` IS NULL;

-- Validation SQL: expected invalid_count = 0.
SELECT 'v8_message_stream_state_values' AS `check_name`,
       COUNT(*) AS `invalid_count`
FROM `ai_message`
WHERE `stream_state` NOT IN ('FINAL', 'PARTIAL', 'DELTA', 'CANCELLED')
   AND `stream_state` IS NOT NULL;
