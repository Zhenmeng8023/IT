package com.alikeyou.itmoduleai.provider.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiProviderStreamChunk {

    private String delta;

    private Boolean finished;

    private String finishReason;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;
}