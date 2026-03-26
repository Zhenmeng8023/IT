package com.alikeyou.itmoduleai.provider.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiProviderChatResponse {

    private String content;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Integer latencyMs;
    private String finishReason;
    private String rawResponse;
}
