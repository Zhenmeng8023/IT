package com.alikeyou.itmoduleai.dto.response;

import com.alikeyou.itmoduleai.provider.embedding.EmbeddingProfileInfo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmbeddingProfileView {
    private String requestedProvider;
    private String requestedModelName;
    private Integer requestedDimension;
    private String configuredProvider;
    private String configuredModelName;
    private String provider;
    private String modelName;
    private Integer dimension;
    private Integer batchSize;
    private String source;
    private Boolean providerSupported;
    private String warning;
    private String activeProvider;
    private String activeModelName;
    private Integer activeDimension;
    private Long activeEmbeddingCount;
    private Boolean needsRebuild;

    public static EmbeddingProfileView from(EmbeddingProfileInfo info) {
        if (info == null) {
            return null;
        }
        return EmbeddingProfileView.builder()
                .requestedProvider(info.getRequestedProvider())
                .requestedModelName(info.getRequestedModelName())
                .requestedDimension(info.getRequestedDimension())
                .configuredProvider(info.getConfiguredProvider())
                .configuredModelName(info.getConfiguredModelName())
                .provider(info.getProvider())
                .modelName(info.getModelName())
                .dimension(info.getDimension())
                .batchSize(info.getBatchSize())
                .source(info.getSource())
                .providerSupported(info.getProviderSupported())
                .warning(info.getWarning())
                .build();
    }
}
