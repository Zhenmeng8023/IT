package com.alikeyou.itmoduleai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class KnowledgeSearchDebugResponse {
    private Long knowledgeBaseId;
    private String query;
    private Integer topK;
    private Integer hitCount;
    private List<HitItem> hits;

    @Getter
    @Builder
    public static class HitItem {
        private Long knowledgeBaseId;
        private String knowledgeBaseName;
        private Long documentId;
        private String documentTitle;
        private String fileName;
        private String archiveEntryPath;
        private Long chunkId;
        private Integer chunkIndex;
        private String snippet;
        private BigDecimal score;
        private BigDecimal keywordScore;
        private BigDecimal vectorScore;
        private String retrievalMethod;
        private Integer rankNo;
    }
}
