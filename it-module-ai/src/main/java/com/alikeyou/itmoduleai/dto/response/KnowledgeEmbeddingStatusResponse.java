package com.alikeyou.itmoduleai.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KnowledgeEmbeddingStatusResponse {
    private String targetType;
    private Long targetId;
    private Long totalChunkCount;
    private Long embeddedChunkCount;
    private Long createdEmbeddingCount;
    private String provider;
    private String modelName;
    private Integer dimension;
    private String profileSource;
    private String profileWarning;
    private Boolean needsRebuild;
    private Integer batchSize;
    private String configuredProvider;
    private String configuredModelName;
    private String activeProvider;
    private String activeModelName;
    private Integer activeDimension;
    private Long activeEmbeddingCount;
    private EmbeddingProfileView embeddingProfile;
}
