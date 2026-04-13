package com.alikeyou.itmoduleai.dto.response;

import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AiKnowledgeBaseBindingVO {

    private Long id;
    private Long sessionId;
    private Long knowledgeBaseId;
    private String knowledgeBaseName;
    private KnowledgeBase.ScopeType scopeType;
    private KnowledgeBase.Visibility visibility;
    private KnowledgeBase.Status status;
    private Integer priority;
    private Boolean defaultKnowledgeBase;
    private Boolean recentKnowledgeBase;
    private Instant createdAt;
}
