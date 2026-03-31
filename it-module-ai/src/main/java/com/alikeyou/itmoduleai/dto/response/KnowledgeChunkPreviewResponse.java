package com.alikeyou.itmoduleai.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KnowledgeChunkPreviewResponse {
    private Integer chunkIndex;
    private String title;
    private String content;
    private Integer tokenCount;
    private Integer charCount;
    private Integer startOffset;
    private Integer endOffset;
    private String metadataJson;
}
