package com.alikeyou.itmoduleai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

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
    private List<AiCitationResponse> citations;
}
