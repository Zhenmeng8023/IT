package com.alikeyou.itmoduleai.entity;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "ai_call_log", schema = "it_data")
public class AiCallLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "user_id")
    private UserInfo user;

    // Deleted:@ManyToOne(fetch = FetchType.LAZY)
    // Deleted:@OnDelete(action = OnDeleteAction.SET_NULL)
    // Deleted:@JoinColumn(name = "project_id")
    // Deleted:private com.alikeyou.itmoduleblog.entity.Project project;

    // Deleted:@ManyToOne(fetch = FetchType.LAZY)
    // Deleted:@OnDelete(action = OnDeleteAction.SET_NULL)
    // Deleted:@JoinColumn(name = "assistant_id")
    // Deleted:private ProjectAiAssistant assistant;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "conversation_id")
    private com.alikeyou.itmoduleinteractive.entity.Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "message_id")
    private com.alikeyou.itmoduleinteractive.entity.Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "prompt_template_id")
    private AiPromptTemplate promptTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "ai_model_id")
    private AiModel aiModel;

    @Column(name = "request_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private RequestType requestType;

    public enum RequestType {
        CHAT, KNOWLEDGE_QA, SUMMARY, REWRITE, PROJECT_ASSISTANT, OTHER
    }

    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        SUCCESS, FAILED, TIMEOUT, CANCELLED
    }

    @Lob
    @Column(name = "request_text")
    private String requestText;

    @Lob
    @Column(name = "response_text")
    private String responseText;

    @Lob
    @Column(name = "request_params")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> requestParams;

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

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;
}
