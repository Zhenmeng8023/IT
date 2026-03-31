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
}
