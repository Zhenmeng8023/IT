package com.alikeyou.itmoduleai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AiChatStreamChunkResponse {

    private Long sessionId;
    private String delta;
    private Boolean finished;
    private String finishReason;
    private List<Long> knowledgeBaseIds;
    private Long defaultKnowledgeBaseId;
    private Long recentKnowledgeBaseId;
    private List<AiCitationResponse> citations;
}
