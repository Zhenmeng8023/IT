package com.alikeyou.itmoduleai.provider.embedding;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmbeddingProfileInfo {
    private final String requestedProvider;
    private final String requestedModelName;
    private final Integer requestedDimension;
    private final String configuredProvider;
    private final String configuredModelName;
    private final String provider;
    private final String modelName;
    private final Integer dimension;
    private final Integer batchSize;
    private final String source;
    private final Boolean providerSupported;
    private final String warning;
}
