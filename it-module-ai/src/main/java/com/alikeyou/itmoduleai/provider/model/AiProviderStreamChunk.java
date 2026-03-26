package com.alikeyou.itmoduleai.provider.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiProviderStreamChunk {

    private String delta;
    private Boolean finished;
    private String finishReason;
}
