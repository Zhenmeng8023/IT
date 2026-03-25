package com.alikeyou.itmoduleai.entity;

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
@Table(name = "ai_model", schema = "it_data")
public class AiModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "model_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ModelType modelType;
    public enum ModelType {
        OPENAI, BAIDU, QWEN, DEEPSEEK, CUSTOM
    }

    @Column(name = "api_key", nullable = false, length = 255)
    private String apiKey;

    @Column(name = "base_url", length = 255)
    private String baseUrl;

    @Lob
    @Column(name = "default_params")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> defaultParams;

    @ColumnDefault("1")
    @Column(name = "is_enabled")
    private Boolean isEnabled;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;
}
