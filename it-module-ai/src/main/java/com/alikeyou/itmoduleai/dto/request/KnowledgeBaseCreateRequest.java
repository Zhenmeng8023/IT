package com.alikeyou.itmoduleai.dto.request;

import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeBaseCreateRequest {

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
}
