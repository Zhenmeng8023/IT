package com.alikeyou.itmoduleai.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeChunkPreviewRequest {
    private String text;
    private String chunkStrategy;
    private Integer maxChars;
    private Integer overlapChars;
}
