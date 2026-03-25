package com.alikeyou.itmoduleinteractive.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "conversation", schema = "it_data")
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "type", nullable = false, columnDefinition = "enum('private','group')")
    private String type;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "creator_id")
    private Long creatorId;

    @Column(name = "ai_type", columnDefinition = "enum('openai','baidu','qwen','deepseek','custom')")
    private String aiType;

    @Column(name = "ai_model_name", length = 100)
    private String aiModelName;

    @Lob
    @Column(name = "ai_config_params")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> aiConfigParams;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;
}
