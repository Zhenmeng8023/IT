package com.alikeyou.itmoduleai.dto.response;

import com.alikeyou.itmoduleai.entity.AiMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AiMessageVO {

    private Long id;
    private Long sessionId;
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
    private Instant createdAt;
}
