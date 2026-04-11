package com.alikeyou.itmoduleinteractive.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationParticipantTableInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initialize() {
        String sql = """
                CREATE TABLE IF NOT EXISTS conversation_participant (
                  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '参与记录ID，主键',
                  conversation_id BIGINT NOT NULL COMMENT '会话ID，关联conversation表',
                  user_id BIGINT NOT NULL COMMENT '参与用户ID，关联user_info表',
                  joined_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入会话时间',
                  last_read_at TIMESTAMP NULL DEFAULT NULL COMMENT '该用户最后已读时间',
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_conversation_participant (conversation_id, user_id),
                  KEY idx_conversation_participant_user (user_id),
                  KEY idx_conversation_participant_conversation (conversation_id),
                  CONSTRAINT fk_conversation_participant_conversation FOREIGN KEY (conversation_id) REFERENCES conversation (id) ON DELETE CASCADE,
                  CONSTRAINT fk_conversation_participant_user FOREIGN KEY (user_id) REFERENCES user_info (id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会话参与者表，支持多客户端聊天';
                """;

        try {
            jdbcTemplate.execute(sql);
            log.info("conversation_participant 表检查完成");
        } catch (Exception ex) {
            log.warn("初始化 conversation_participant 表失败，聊天多客户端能力可能不可用", ex);
        }
    }
}
