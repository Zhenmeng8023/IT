package com.alikeyou.itmoduleai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "ai_model", schema = "it9_data")
public class AiModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_type", nullable = false, length = 50)
    private ModelType modelType;

    @Column(name = "provider_code", nullable = false, length = 50)
    private String providerCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "deployment_mode", nullable = false, length = 20)
    private DeploymentMode deploymentMode;

    @Column(name = "api_key", length = 255)
    private String apiKey;

    @Column(name = "base_url", length = 255)
    private String baseUrl;

    @Column(name = "default_params", columnDefinition = "json")
    private String defaultParams;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "timeout_ms")
    private Integer timeoutMs;

    @Column(name = "supports_stream")
    private Boolean supportsStream;

    @Column(name = "supports_tools")
    private Boolean supportsTools;

    @Column(name = "supports_embedding")
    private Boolean supportsEmbedding;

    @Column(name = "cost_input_per_1m", precision = 10, scale = 4)
    private BigDecimal costInputPer1m;

    @Column(name = "cost_output_per_1m", precision = 10, scale = 4)
    private BigDecimal costOutputPer1m;

    @Column(name = "is_enabled")
    private Boolean isEnabled;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum ModelType {
        OPENAI, BAIDU, QWEN, DEEPSEEK, OLLAMA, CUSTOM
    }

    public enum DeploymentMode {
        REMOTE_API, LOCAL_OLLAMA, MOCK
    }
}
