package com.alikeyou.itmoduleai.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiChatStreamChunkResponse {

    private Long sessionId;
    private String delta;
    private Boolean finished;
    private String finishReason;
}
