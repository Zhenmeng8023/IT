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
    private Integer vectorCandidateCount;
    private Integer keywordCandidateCount;
    private Integer availableEmbeddingCount;
    private Integer providerFilteredEmbeddingCount;
    private Integer modelFilteredEmbeddingCount;
    private Integer statusFilteredEmbeddingCount;
    private String degradeReason;
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
        private String path;
        private Long chunkId;
        private Integer chunkIndex;
        private String snippet;
        private BigDecimal score;
        private BigDecimal keywordScore;
        private BigDecimal vectorScore;
        private String retrievalMethod;
        private Integer rankNo;
        private String language;
        private String symbolName;
        private String symbolType;
        private Integer startLine;
        private Integer endLine;
        private String sectionName;
    }
}
