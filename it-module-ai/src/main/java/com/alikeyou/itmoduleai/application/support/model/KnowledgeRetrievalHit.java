package com.alikeyou.itmoduleai.application.support.model;

import com.alikeyou.itmoduleai.entity.AiRetrievalLog;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeChunk;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeRetrievalHit {
    private Long knowledgeBaseId;
    private String knowledgeBaseName;
    private Long documentId;
    private String documentTitle;
    private String fileName;
    private String archiveEntryPath;
    private String path;
    private Long chunkId;
    private Integer chunkIndex;
    private String chunkContent;
    private String snippet;
    private BigDecimal score;
    private BigDecimal keywordScore;
    private BigDecimal vectorScore;
    private Integer rankNo;
    private AiRetrievalLog.RetrievalMethod retrievalMethod;
    private String language;
    private String symbolName;
    private String symbolType;
    private Integer startLine;
    private Integer endLine;
    private String sectionName;

    private KnowledgeBase knowledgeBase;
    private KnowledgeDocument document;
    private KnowledgeChunk chunk;
}
