package com.alikeyou.itmoduleai.dto.front.request;

import com.alikeyou.itmoduleai.dto.request.KnowledgeBaseCreateRequest;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FrontKnowledgeBaseUpsertRequest {

    private KnowledgeBase.ScopeType scopeType;
    private Long projectId;
    private String name;
    private String description;
    private KnowledgeBase.SourceType sourceType;
    private String embeddingProvider;
    private String embeddingModel;
    private KnowledgeBase.ChunkStrategy chunkStrategy;
    private Integer defaultTopK;
    private KnowledgeBase.Visibility visibility;

    public KnowledgeBaseCreateRequest toServiceRequest(Long ownerId) {
        KnowledgeBaseCreateRequest target = new KnowledgeBaseCreateRequest();
        target.setScopeType(scopeType);
        target.setProjectId(projectId);
        target.setOwnerId(ownerId);
        target.setName(name);
        target.setDescription(description);
        target.setSourceType(sourceType);
        target.setEmbeddingProvider(embeddingProvider);
        target.setEmbeddingModel(embeddingModel);
        target.setChunkStrategy(chunkStrategy);
        target.setDefaultTopK(defaultTopK);
        target.setVisibility(visibility);
        return target;
    }
}
