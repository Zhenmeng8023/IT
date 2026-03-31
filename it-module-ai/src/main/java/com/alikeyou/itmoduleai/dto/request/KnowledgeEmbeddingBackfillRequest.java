package com.alikeyou.itmoduleai.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeEmbeddingBackfillRequest {
    private String provider;
    private String modelName;
    private Integer dimension;
}
