package com.alikeyou.itmoduleai.entity;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "ai_prompt_template", schema = "it_data")
public class AiPromptTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "template_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private TemplateType templateType;

    public enum TemplateType {
        GENERAL_CHAT, KNOWLEDGE_QA, PROJECT_ASSISTANT, SUMMARY, CODE_EXPLAIN, CUSTOM
    }

    @Column(name = "scope_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ScopeType scopeType;

    public enum ScopeType {
        PLATFORM, PROJECT, PERSONAL
    }

    // Deleted:@ManyToOne(fetch = FetchType.LAZY)
    // Deleted:@OnDelete(action = OnDeleteAction.CASCADE)
    // Deleted:@JoinColumn(name = "project_id")
    // Deleted:private com.alikeyou.itmoduleblog.entity.Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "owner_id")
    private UserInfo owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "default_model_id")
    private AiModel defaultModel;

    @Lob
    @Column(name = "system_prompt", nullable = false)
    private String systemPrompt;

    @Lob
    @Column(name = "user_prompt_template")
    private String userPromptTemplate;

    @Column(name = "variables_schema", columnDefinition = "json")
    private String variablesSchema;

    @ColumnDefault("1")
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;
}
