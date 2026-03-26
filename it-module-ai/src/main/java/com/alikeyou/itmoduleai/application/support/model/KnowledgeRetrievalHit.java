package com.alikeyou.itmoduleai.application.support.model;

import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeChunk;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class KnowledgeRetrievalHit {

    private Long knowledgeBaseId;
    private String knowledgeBaseName;
    private Long documentId;
    private String documentTitle;
    private Long chunkId;
    private Integer chunkIndex;
    private String chunkContent;
    private String snippet;
    private BigDecimal score;
    private Integer rankNo;

    private KnowledgeBase knowledgeBase;
    private KnowledgeDocument document;
    private KnowledgeChunk chunk;
}
