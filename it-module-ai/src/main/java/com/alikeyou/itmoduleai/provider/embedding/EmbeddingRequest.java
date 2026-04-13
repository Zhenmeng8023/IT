package com.alikeyou.itmoduleai.provider.embedding;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmbeddingRequest {
    private String text;
    private String providerCode;
    private String modelName;
    private Integer dimension;
}
