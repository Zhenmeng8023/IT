package com.alikeyou.itmoduleai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "ai_prompt_template", schema = "it9_data")
public class AiPromptTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scene_code", nullable = false, length = 100)
    private String sceneCode;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false, length = 50)
    private TemplateType templateType;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 50)
    private ScopeType scopeType;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "owner_id")
    private Long ownerId;

    @ManyToOne(fetch = FetchType.LAZY)
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

    @Column(name = "output_schema", columnDefinition = "json")
    private String outputSchema;

    @Column(name = "version_no")
    private Integer versionNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "publish_status", nullable = false, length = 20)
    private PublishStatus publishStatus;

    @Column(name = "is_enabled")
    private Boolean isEnabled;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum TemplateType {
        GENERAL_CHAT, KNOWLEDGE_QA, PROJECT_ASSISTANT, BLOG_ASSISTANT, SUMMARY, CODE_EXPLAIN, CUSTOM
    }

    public enum ScopeType {
        PLATFORM, PROJECT, PERSONAL
    }

    public enum PublishStatus {
        DRAFT, PUBLISHED, DISABLED
    }
}
