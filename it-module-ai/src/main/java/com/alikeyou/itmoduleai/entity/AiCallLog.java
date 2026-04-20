package com.alikeyou.itmoduleai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.function.Supplier;

@Getter
@Setter
@Entity
@Table(name = "ai_call_log", schema = "it9_data")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "biz_type", length = 30)
    private BizType bizType;

    @Column(name = "biz_id")
    private Long bizId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private AiSession session;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private AiMessage message;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_template_id")
    private AiPromptTemplate promptTemplate;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_model_id")
    private AiModel aiModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 30)
    private RequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Lob
    @Column(name = "request_text")
    private String requestText;

    @Lob
    @Column(name = "response_text")
    private String responseText;

    @Column(name = "request_params", columnDefinition = "json")
    private String requestParams;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "repository_id")
    private Long repositoryId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "commit_id")
    private Long commitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_stage", length = 30)
    private RequestStage requestStage;

    @Column(name = "retrieval_plan_json", columnDefinition = "json")
    private String retrievalPlanJson;

    @Column(name = "retrieval_summary_json", columnDefinition = "json")
    private String retrievalSummaryJson;

    @Column(name = "grounding_report_json", columnDefinition = "json")
    private String groundingReportJson;

    @Column(name = "degrade_reason", length = 500)
    private String degradeReason;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "cost_amount", precision = 10, scale = 4)
    private BigDecimal costAmount;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "stream_started_at")
    private Instant streamStartedAt;

    @Column(name = "stream_finished_at")
    private Instant streamFinishedAt;

    @Column(name = "first_token_latency_ms")
    private Integer firstTokenLatencyMs;

    @Column(name = "metadata_json", columnDefinition = "json")
    private String metadataJson;

    @Column(name = "created_at")
    private Instant createdAt;

    public Long getSessionId() {
        return safe(() -> session.getId());
    }

    public String getSceneCode() {
        return safe(() -> session.getSceneCode());
    }

    public Long getMessageId() {
        return safe(() -> message.getId());
    }

    public Long getPromptTemplateId() {
        return safe(() -> promptTemplate.getId());
    }

    public String getPromptTemplateName() {
        return safe(() -> promptTemplate.getTemplateName());
    }

    public Long getAiModelId() {
        return safe(() -> aiModel.getId());
    }

    public String getAiModelName() {
        return safe(() -> aiModel.getModelName());
    }

    public String getAiModelProviderCode() {
        return safe(() -> aiModel.getProviderCode());
    }

    public String getAiModelDeploymentMode() {
        return safe(() -> aiModel.getDeploymentMode() == null ? null : aiModel.getDeploymentMode().name());
    }

    private <T> T safe(Supplier<T> supplier) {
        try {
            return supplier == null ? null : supplier.get();
        } catch (Exception e) {
            return null;
        }
    }

    public enum BizType {
        GENERAL,
        PROJECT,
        BLOG,
        CIRCLE,
        PAID_CONTENT
    }

    public enum RequestType {
        CHAT,
        KNOWLEDGE_QA,
        SUMMARY,
        REWRITE,
        PROJECT_ASSISTANT,
        BLOG_ASSISTANT,
        CODE_EXPLAIN,
        OTHER
    }

    public enum RequestStage {
        PLAN,
        RETRIEVE,
        RERANK,
        GROUND,
        GENERATE,
        STREAM
    }

    public enum Status {
        SUCCESS,
        FAILED,
        TIMEOUT,
        CANCELLED
    }
}
