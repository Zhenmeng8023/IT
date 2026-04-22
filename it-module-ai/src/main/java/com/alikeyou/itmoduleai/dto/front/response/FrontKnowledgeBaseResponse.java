package com.alikeyou.itmoduleai.dto.front.response;

import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class FrontKnowledgeBaseResponse {

    private Long id;
    private KnowledgeBase.ScopeType scopeType;
    private Long projectId;
    private Long ownerId;
    private String name;
    private String description;
    private KnowledgeBase.SourceType sourceType;
    private String embeddingProvider;
    private String embeddingModel;
    private KnowledgeBase.ChunkStrategy chunkStrategy;
    private Integer defaultTopK;
    private KnowledgeBase.Visibility visibility;
    private KnowledgeBase.Status status;
    private Integer docCount;
    private Integer chunkCount;
    private Instant lastIndexedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static FrontKnowledgeBaseResponse from(KnowledgeBase entity) {
        if (entity == null) {
            return null;
        }
        return FrontKnowledgeBaseResponse.builder()
                .id(entity.getId())
                .scopeType(entity.getScopeType())
                .projectId(entity.getProjectId())
                .ownerId(entity.getOwnerId())
                .name(entity.getName())
                .description(entity.getDescription())
                .sourceType(entity.getSourceType())
                .embeddingProvider(entity.getEmbeddingProvider())
                .embeddingModel(entity.getEmbeddingModel())
                .chunkStrategy(entity.getChunkStrategy())
                .defaultTopK(entity.getDefaultTopK())
                .visibility(entity.getVisibility())
                .status(entity.getStatus())
                .docCount(entity.getDocCount())
                .chunkCount(entity.getChunkCount())
                .lastIndexedAt(entity.getLastIndexedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
