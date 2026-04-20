package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeChunk;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.alikeyou.itmoduleai.entity.KnowledgeIndexTask;
import com.alikeyou.itmoduleai.service.KnowledgeAccessGuard;
import com.alikeyou.itmoduleai.service.KnowledgeBaseService;
import com.alikeyou.itmoduleai.service.KnowledgeImportTaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseControllerHttpTest {

    @Mock
    private KnowledgeBaseService knowledgeBaseService;
    @Mock
    private KnowledgeImportTaskService knowledgeImportTaskService;
    @Mock
    private KnowledgeAccessGuard knowledgeAccessGuard;
    @Mock
    private AiCurrentUserProvider currentUserProvider;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        KnowledgeBaseController controller = new KnowledgeBaseController(
                knowledgeBaseService,
                knowledgeImportTaskService,
                knowledgeAccessGuard,
                currentUserProvider
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getKnowledgeBaseReturnsDataAfterReadGuard() throws Exception {
        KnowledgeBase kb = knowledgeBase(11L);
        when(knowledgeAccessGuard.requireKnowledgeBaseRead(11L)).thenReturn(kb);
        when(knowledgeBaseService.getById(11L)).thenReturn(kb);

        mockMvc.perform(get("/api/ai/knowledge-bases/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.name").value("kb-11"));

        verify(knowledgeAccessGuard).requireKnowledgeBaseRead(11L);
    }

    @Test
    void addDocumentRequiresEditAccessAndReturnsIndexedDocument() throws Exception {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(21L);
        document.setKnowledgeBase(knowledgeBase(11L));
        document.setSourceType(KnowledgeDocument.SourceType.MANUAL);
        document.setTitle("Guide");
        document.setContentText("content");
        document.setStatus(KnowledgeDocument.Status.INDEXED);

        when(knowledgeAccessGuard.requireKnowledgeBaseEdit(11L)).thenReturn(knowledgeBase(11L));
        when(knowledgeBaseService.addDocument(any(), any())).thenReturn(document);

        mockMvc.perform(post("/api/ai/knowledge-bases/11/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Guide", "contentText": "content", "autoIndex": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(21))
                .andExpect(jsonPath("$.data.status").value("INDEXED"));

        verify(knowledgeAccessGuard).requireKnowledgeBaseEdit(11L);
    }

    @Test
    void createIndexTaskReturnsPendingTask() throws Exception {
        KnowledgeIndexTask task = new KnowledgeIndexTask();
        task.setId(31L);
        task.setKnowledgeBase(knowledgeBase(11L));
        task.setTaskType(KnowledgeIndexTask.TaskType.REINDEX);
        task.setStatus(KnowledgeIndexTask.Status.PENDING);

        when(knowledgeAccessGuard.requireKnowledgeBaseEdit(11L)).thenReturn(knowledgeBase(11L));
        when(knowledgeBaseService.createIndexTask(any(), any())).thenReturn(task);

        mockMvc.perform(post("/api/ai/knowledge-bases/11/index-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("taskType", "REINDEX"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(31))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void listChunksReturnsNotFoundWhenDocumentDoesNotExist() throws Exception {
        when(knowledgeAccessGuard.requireDocumentRead(404L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Knowledge document not found"));

        mockMvc.perform(get("/api/ai/knowledge-bases/documents/404/chunks"))
                .andExpect(status().isNotFound());

        verify(knowledgeBaseService, never()).listChunks(any());
    }

    @Test
    void listChunksRejectsUnauthorizedDocumentAccess() throws Exception {
        when(knowledgeAccessGuard.requireDocumentRead(22L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden"));

        mockMvc.perform(get("/api/ai/knowledge-bases/documents/22/chunks"))
                .andExpect(status().isForbidden());

        verify(knowledgeBaseService, never()).listChunks(any());
    }

    @Test
    void listChunksReturnsChunksAfterDocumentGuard() throws Exception {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(33L);
        chunk.setChunkIndex(0);
        chunk.setContent("first chunk");

        when(knowledgeAccessGuard.requireDocumentRead(21L)).thenReturn(document(21L, knowledgeBase(11L)));
        when(knowledgeBaseService.listChunks(21L)).thenReturn(List.of(chunk));

        mockMvc.perform(get("/api/ai/knowledge-bases/documents/21/chunks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(33))
                .andExpect(jsonPath("$.data[0].content").value("first chunk"));
    }

    private KnowledgeBase knowledgeBase(Long id) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(id);
        kb.setOwnerId(1L);
        kb.setName("kb-" + id);
        kb.setScopeType(KnowledgeBase.ScopeType.PERSONAL);
        kb.setVisibility(KnowledgeBase.Visibility.PRIVATE);
        kb.setSourceType(KnowledgeBase.SourceType.MANUAL);
        kb.setChunkStrategy(KnowledgeBase.ChunkStrategy.PARAGRAPH);
        kb.setStatus(KnowledgeBase.Status.ACTIVE);
        return kb;
    }

    private KnowledgeDocument document(Long id, KnowledgeBase kb) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(id);
        document.setKnowledgeBase(kb);
        document.setSourceType(KnowledgeDocument.SourceType.MANUAL);
        document.setTitle("doc-" + id);
        document.setStatus(KnowledgeDocument.Status.UPLOADED);
        return document;
    }
}
