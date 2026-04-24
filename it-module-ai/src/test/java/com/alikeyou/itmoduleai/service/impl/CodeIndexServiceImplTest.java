package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.entity.AiCodeReference;
import com.alikeyou.itmoduleai.entity.AiCodeSymbol;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeChunk;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.alikeyou.itmoduleai.repository.AiCodeReferenceRepository;
import com.alikeyou.itmoduleai.repository.AiCodeSymbolRepository;
import com.alikeyou.itmoduleai.service.CodeIndexService;
import com.alikeyou.itmoduleai.service.KnowledgeChunkingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodeIndexServiceImplTest {

    @Mock
    private AiCodeSymbolRepository symbolRepository;

    @Mock
    private AiCodeReferenceRepository referenceRepository;

    private CodeIndexServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CodeIndexServiceImpl(symbolRepository, referenceRepository);
    }

    @Test
    void rebuildsSymbolsAndReferencesForSupportedLanguage() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(100L);

        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(200L);
        document.setLanguage("java");
        document.setFilePath("src/main/java/demo/TicketService.java");

        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(300L);
        chunk.setChunkIndex(0);
        chunk.setDocument(document);

        KnowledgeChunkingService.SymbolDraft symbolDraft = new KnowledgeChunkingService.SymbolDraft(
                "sym-ticket",
                "TicketService",
                "demo.TicketService",
                "CLASS",
                "MODULE",
                "public class TicketService",
                null,
                "PUBLIC",
                true,
                true,
                1,
                30,
                2,
                29,
                0,
                "java",
                "src/main/java/demo/TicketService.java",
                "{}"
        );
        KnowledgeChunkingService.ReferenceDraft importRef = new KnowledgeChunkingService.ReferenceDraft(
                "ref-import",
                "sym-ticket",
                "IMPORT",
                "java.util.List",
                "java.util.List",
                1,
                1,
                0,
                "EXTERNAL",
                "src/main/java/demo/TicketService.java",
                "java",
                "{}"
        );
        KnowledgeChunkingService.ReferenceDraft callRef = new KnowledgeChunkingService.ReferenceDraft(
                "ref-call",
                "sym-ticket",
                "CALL",
                "load",
                "load",
                10,
                10,
                0,
                "UNRESOLVED",
                "src/main/java/demo/TicketService.java",
                "java",
                "{}"
        );

        KnowledgeChunkingService.IndexBuildResult draft = new KnowledgeChunkingService.IndexBuildResult(
                List.of(new KnowledgeChunkingService.ChunkDraft(
                        0, "chunk", "content", 10, 20, 0, 20, 1, 20, "{}",
                        "java", "src/main/java/demo/TicketService.java", "TicketService", "class", "module"
                )),
                List.of(symbolDraft),
                List.of(importRef, callRef)
        );

        when(symbolRepository.saveAll(anyList())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<AiCodeSymbol> symbols = invocation.getArgument(0);
            long id = 1L;
            for (AiCodeSymbol symbol : symbols) {
                symbol.setId(id++);
            }
            return symbols;
        });
        when(referenceRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        CodeIndexService.CodeIndexResult result = service.rebuildDocumentCodeIndex(kb, document, List.of(chunk), draft);

        assertThat(result.symbolCount()).isEqualTo(1);
        assertThat(result.referenceCount()).isEqualTo(2);
        assertThat(result.symbolIndexStatus()).isEqualTo(KnowledgeDocument.SymbolIndexStatus.INDEXED);

        verify(referenceRepository).deleteByDocumentGraphId(eq(200L));
        verify(symbolRepository).deleteByDocumentId(eq(200L));
        verify(symbolRepository).saveAll(anyList());
        verify(referenceRepository).saveAll(anyList());

        ArgumentCaptor<List<AiCodeReference>> refCaptor = ArgumentCaptor.forClass(List.class);
        verify(referenceRepository).saveAll(refCaptor.capture());
        assertThat(refCaptor.getValue()).extracting(AiCodeReference::getRefKind)
                .contains("IMPORT", "CALL");
    }

    @Test
    void returnsNotApplicableForUnsupportedLanguage() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(1L);

        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(2L);
        document.setLanguage("python");
        document.setFilePath("src/main/python/app.py");

        CodeIndexService.CodeIndexResult result = service.rebuildDocumentCodeIndex(
                kb,
                document,
                List.of(),
                KnowledgeChunkingService.IndexBuildResult.empty()
        );

        assertThat(result.symbolCount()).isZero();
        assertThat(result.referenceCount()).isZero();
        assertThat(result.symbolIndexStatus()).isEqualTo(KnowledgeDocument.SymbolIndexStatus.NOT_APPLICABLE);

        verify(referenceRepository).deleteByDocumentGraphId(eq(2L));
        verify(symbolRepository).deleteByDocumentId(eq(2L));
        verify(symbolRepository, never()).saveAll(anyList());
        verify(referenceRepository, never()).saveAll(anyList());
    }

    @Test
    void deduplicatesGeneratedSymbolKeysWithinSameDocument() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(100L);

        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(200L);
        document.setLanguage("vue");
        document.setFilePath("ProjectDocEditor.vue");

        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(300L);
        chunk.setChunkIndex(0);
        chunk.setDocument(document);

        KnowledgeChunkingService.SymbolDraft first = new KnowledgeChunkingService.SymbolDraft(
                null, "default", null, "FUNCTION", "default", null, null, null,
                true, false, 80, 120, 81, 119, 0, "vue", "ProjectDocEditor.vue", "{}"
        );
        KnowledgeChunkingService.SymbolDraft duplicate = new KnowledgeChunkingService.SymbolDraft(
                null, "default", null, "FUNCTION", "default", null, null, null,
                true, false, 80, 120, 81, 119, 0, "vue", "ProjectDocEditor.vue", "{}"
        );

        KnowledgeChunkingService.IndexBuildResult draft = new KnowledgeChunkingService.IndexBuildResult(
                List.of(new KnowledgeChunkingService.ChunkDraft(
                        0, "chunk", "content", 10, 20, 0, 20, 1, 20, "{}",
                        "vue", "ProjectDocEditor.vue", "default", "FUNCTION", "default"
                )),
                List.of(first, duplicate),
                List.of()
        );

        when(symbolRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        CodeIndexService.CodeIndexResult result = service.rebuildDocumentCodeIndex(kb, document, List.of(chunk), draft);

        assertThat(result.symbolCount()).isEqualTo(1);

        ArgumentCaptor<List<AiCodeSymbol>> captor = ArgumentCaptor.forClass(List.class);
        verify(symbolRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getSymbolKey()).isEqualTo("200:ProjectDocEditor.vue:FUNCTION:default:80");
    }

    @Test
    void prefixesExplicitSymbolKeysAndResolvesReferenceLookupWithinDocument() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(100L);

        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(200L);
        document.setLanguage("vue");
        document.setFilePath("ConflictDetailDrawer.vue");

        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(300L);
        chunk.setChunkIndex(0);
        chunk.setDocument(document);

        KnowledgeChunkingService.SymbolDraft symbolDraft = new KnowledgeChunkingService.SymbolDraft(
                "ConflictDetailDrawer.vue:FUNCTION:TYPE_LABELS:206",
                "TYPE_LABELS",
                null,
                "FUNCTION",
                "module",
                null,
                null,
                null,
                true,
                false,
                206,
                240,
                206,
                239,
                0,
                "vue",
                "ConflictDetailDrawer.vue",
                "{}"
        );
        KnowledgeChunkingService.ReferenceDraft referenceDraft = new KnowledgeChunkingService.ReferenceDraft(
                "ref-1",
                "ConflictDetailDrawer.vue:FUNCTION:TYPE_LABELS:206",
                "CALL",
                "TYPE_LABELS",
                null,
                210,
                210,
                0,
                "RESOLVED",
                "ConflictDetailDrawer.vue",
                "vue",
                "{}"
        );

        KnowledgeChunkingService.IndexBuildResult draft = new KnowledgeChunkingService.IndexBuildResult(
                List.of(new KnowledgeChunkingService.ChunkDraft(
                        0, "chunk", "content", 10, 20, 0, 20, 1, 20, "{}",
                        "vue", "ConflictDetailDrawer.vue", "TYPE_LABELS", "FUNCTION", "module"
                )),
                List.of(symbolDraft),
                List.of(referenceDraft)
        );

        when(symbolRepository.saveAll(anyList())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<AiCodeSymbol> symbols = invocation.getArgument(0);
            symbols.get(0).setId(1L);
            return symbols;
        });
        when(referenceRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        CodeIndexService.CodeIndexResult result = service.rebuildDocumentCodeIndex(kb, document, List.of(chunk), draft);

        assertThat(result.symbolCount()).isEqualTo(1);
        assertThat(result.referenceCount()).isEqualTo(1);

        ArgumentCaptor<List<AiCodeSymbol>> symbolCaptor = ArgumentCaptor.forClass(List.class);
        verify(symbolRepository).saveAll(symbolCaptor.capture());
        assertThat(symbolCaptor.getValue()).hasSize(1);
        assertThat(symbolCaptor.getValue().get(0).getSymbolKey())
                .isEqualTo("200:ConflictDetailDrawer.vue:FUNCTION:TYPE_LABELS:206");

        ArgumentCaptor<List<AiCodeReference>> referenceCaptor = ArgumentCaptor.forClass(List.class);
        verify(referenceRepository).saveAll(referenceCaptor.capture());
        assertThat(referenceCaptor.getValue()).hasSize(1);
        assertThat(referenceCaptor.getValue().get(0).getFromSymbol()).isNotNull();
        assertThat(referenceCaptor.getValue().get(0).getFromSymbol().getSymbolKey())
                .isEqualTo("200:ConflictDetailDrawer.vue:FUNCTION:TYPE_LABELS:206");
    }

    @Test
    void prefixesAndDeduplicatesReferenceKeysWithinSameDocument() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(100L);

        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(200L);
        document.setLanguage("vue");
        document.setFilePath("ProjectActivityManagePanel.vue");

        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(300L);
        chunk.setChunkIndex(0);
        chunk.setDocument(document);

        KnowledgeChunkingService.ReferenceDraft first = new KnowledgeChunkingService.ReferenceDraft(
                "ProjectActivityManagePanel.vue:CALL:loadPage:10",
                null,
                "CALL",
                "loadPage",
                null,
                10,
                10,
                0,
                "UNRESOLVED",
                "ProjectActivityManagePanel.vue",
                "vue",
                "{}"
        );
        KnowledgeChunkingService.ReferenceDraft duplicate = new KnowledgeChunkingService.ReferenceDraft(
                "ProjectActivityManagePanel.vue:CALL:loadPage:10",
                null,
                "CALL",
                "loadPage",
                null,
                10,
                10,
                0,
                "UNRESOLVED",
                "ProjectActivityManagePanel.vue",
                "vue",
                "{}"
        );

        KnowledgeChunkingService.IndexBuildResult draft = new KnowledgeChunkingService.IndexBuildResult(
                List.of(new KnowledgeChunkingService.ChunkDraft(
                        0, "chunk", "content", 10, 20, 0, 20, 1, 20, "{}",
                        "vue", "ProjectActivityManagePanel.vue", "loadPage", "FUNCTION", "module"
                )),
                List.of(),
                List.of(first, duplicate)
        );

        when(referenceRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        CodeIndexService.CodeIndexResult result = service.rebuildDocumentCodeIndex(kb, document, List.of(chunk), draft);

        assertThat(result.symbolCount()).isZero();
        assertThat(result.referenceCount()).isEqualTo(1);

        ArgumentCaptor<List<AiCodeReference>> captor = ArgumentCaptor.forClass(List.class);
        verify(referenceRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getReferenceKey())
                .isEqualTo("200:ProjectActivityManagePanel.vue:CALL:loadPage:10");
    }
}
