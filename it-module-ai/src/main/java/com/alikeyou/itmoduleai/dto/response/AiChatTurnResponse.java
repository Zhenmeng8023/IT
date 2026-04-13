package com.alikeyou.itmoduleai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class AiChatTurnResponse {

    private Long sessionId;
    private Long userMessageId;
    private Long assistantMessageId;
    private Long callLogId;
    private String content;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Integer latencyMs;
    private String finishReason;
    private Long modelId;
    private String modelName;
    private Long promptTemplateId;
    private String promptTemplateName;
    private String sceneCode;
    private List<Long> knowledgeBaseIds;
    private Long defaultKnowledgeBaseId;
    private Long recentKnowledgeBaseId;
    private String displayText;
    private Map<String, Object> structured;
    private List<AiCitationResponse> citations;
}
