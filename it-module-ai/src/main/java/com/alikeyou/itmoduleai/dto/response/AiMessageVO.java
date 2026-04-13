package com.alikeyou.itmoduleai.dto.response;

import com.alikeyou.itmoduleai.entity.AiMessage;
import com.alikeyou.itmoduleai.enums.GroundingStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class AiMessageVO {

    private Long id;
    private Long sessionId;
    private Long callLogId;
    private AiMessage.Role role;
    private Long senderUserId;
    private String content;
    private Integer contentTokens;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Long promptTemplateId;
    private Long modelId;
    private Long knowledgeBaseId;
    private List<Long> knowledgeBaseIds;
    private Long defaultKnowledgeBaseId;
    private Long recentKnowledgeBaseId;
    private List<AiCitationResponse> citations;
    private String quotedChunkIds;
    private String toolCallJson;
    private Map<String, Object> retrievalSummary;
    private GroundingStatus groundingStatus;
    private Map<String, Object> grounding;
    private AiMessage.StreamState streamState;
    private Integer latencyMs;
    private String finishReason;
    private AiMessage.Status status;
    private Instant createdAt;
}
