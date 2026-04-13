package com.alikeyou.itmoduleai.dto.response;

import com.alikeyou.itmoduleai.entity.AiSession;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class AiSessionVO {

    private Long id;
    private Long userId;
    private AiSession.BizType bizType;
    private Long bizId;
    private Long projectId;
    private String sceneCode;
    private String sessionTitle;
    private AiSession.MemoryMode memoryMode;
    private Long activeModelId;
    private Long promptTemplateId;
    private Long defaultKnowledgeBaseId;
    private Long recentKnowledgeBaseId;
    private List<Long> boundKnowledgeBaseIds;
    private List<AiKnowledgeBaseBindingVO> boundKnowledgeBases;
    private String sessionSummary;
    private String extConfig;
    private AiSession.Status status;
    private Instant lastMessageAt;
    private Instant createdAt;
    private Instant updatedAt;
}
