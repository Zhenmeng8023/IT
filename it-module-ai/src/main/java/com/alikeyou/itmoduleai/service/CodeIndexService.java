package com.alikeyou.itmoduleai.service;

import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeChunk;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;

import java.util.List;

public interface CodeIndexService {

    CodeIndexResult rebuildDocumentCodeIndex(
            KnowledgeBase knowledgeBase,
            KnowledgeDocument document,
            List<KnowledgeChunk> chunks,
            KnowledgeChunkingService.IndexBuildResult draft
    );

    record CodeIndexResult(
            int symbolCount,
            int referenceCount,
            KnowledgeDocument.SymbolIndexStatus symbolIndexStatus
    ) {
    }
}
