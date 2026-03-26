package com.alikeyou.itmoduleai.dto.request;

import com.alikeyou.itmoduleai.entity.AiMessage;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiMessageCreateRequest {

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
    private String quotedChunkIds;
    private String toolCallJson;
    private Integer latencyMs;
    private String finishReason;
    private AiMessage.Status status;
}
