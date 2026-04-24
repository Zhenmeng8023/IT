package com.alikeyou.itmoduleai.controller.front;

import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import com.alikeyou.itmoduleai.dto.request.KnowledgeBaseCreateRequest;
import com.alikeyou.itmoduleai.dto.request.KnowledgeDocumentCreateRequest;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.alikeyou.itmoduleai.entity.KnowledgeImportTask;
import com.alikeyou.itmoduleai.entity.KnowledgeIndexTask;
import com.alikeyou.itmoduleai.security.AiPermissionGuard;
import com.alikeyou.itmoduleai.service.KnowledgeAccessGuard;
import com.alikeyou.itmoduleai.service.KnowledgeBaseService;
import com.alikeyou.itmoduleai.service.KnowledgeEmbeddingService;
import com.alikeyou.itmoduleai.service.KnowledgeImportTaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FrontKnowledgeBaseControllerHttpTest {

    @Mock
    private KnowledgeBaseService knowledgeBaseService;
    @Mock
    private KnowledgeImportTaskService knowledgeImportTaskService;
    @Mock
    private KnowledgeAccessGuard knowledgeAccessGuard;
    @Mock
    private AiCurrentUserProvider currentUserProvider;
    @Mock
    private AiPermissionGuard aiPermissionGuard;
    @Mock
    private KnowledgeEmbeddingService knowledgeEmbeddingService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        FrontKnowledgeBaseController controller = new FrontKnowledgeBaseController(
                knowledgeBaseService,
                knowledgeImportTaskService,
                knowledgeAccessGuard,
                currentUserProvider,
                aiPermissionGuard,
                knowledgeEmbeddingService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void createUsesCurrentUserAsOwnerForFrontKnowledgeBase() throws Exception {
        when(currentUserProvider.requireCurrentUserId()).thenReturn(7L);
        doNothing().when(aiPermissionGuard).requireFrontKnowledgeBaseCreate(KnowledgeBase.ScopeType.PERSONAL, null);
        when(knowledgeBaseService.createKnowledgeBase(any(KnowledgeBaseCreateRequest.class))).thenReturn(knowledgeBase(15L, KnowledgeBase.ScopeType.PERSONAL));

        mockMvc.perform(post("/api/ai/front/knowledge-bases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scopeType":"PERSONAL","name":"My KB"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(15))
                .andExpect(jsonPath("$.data.ownerId").value(7));

        ArgumentCaptor<KnowledgeBaseCreateRequest> captor = ArgumentCaptor.forClass(KnowledgeBaseCreateRequest.class);
        verify(knowledgeBaseService).createKnowledgeBase(captor.capture());
        verify(aiPermissionGuard).requireFrontKnowledgeBaseCreate(KnowledgeBase.ScopeType.PERSONAL, null);
        KnowledgeBaseCreateRequest request = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(7L, request.getOwnerId());
        org.junit.jupiter.api.Assertions.assertEquals(KnowledgeBase.ScopeType.PERSONAL, request.getScopeType());
    }

    @Test
    void createProjectKnowledgeBaseUsesProjectAwareCreateGuard() throws Exception {
        when(currentUserProvider.requireCurrentUserId()).thenReturn(7L);
        doNothing().when(aiPermissionGuard).requireFrontKnowledgeBaseCreate(KnowledgeBase.ScopeType.PROJECT, 99L);
        when(knowledgeBaseService.createKnowledgeBase(any(KnowledgeBaseCreateRequest.class)))
                .thenReturn(knowledgeBase(16L, KnowledgeBase.ScopeType.PROJECT));

        mockMvc.perform(post("/api/ai/front/knowledge-bases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scopeType":"PROJECT","projectId":99,"name":"Project KB"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(16))
                .andExpect(jsonPath("$.data.scopeType").value("PROJECT"));

        verify(aiPermissionGuard).requireFrontKnowledgeBaseCreate(KnowledgeBase.ScopeType.PROJECT, 99L);
    }

    @Test
    void updateRequiresTargetScopePermissionWhenSwitchingToProjectScope() throws Exception {
        KnowledgeBase existing = knowledgeBase(21L, KnowledgeBase.ScopeType.PERSONAL);
        when(knowledgeAccessGuard.requireKnowledgeBaseEdit(21L)).thenReturn(existing);
        doNothing().when(aiPermissionGuard).requireFrontKnowledgeBaseEdit(KnowledgeBase.ScopeType.PERSONAL);
        doNothing().when(aiPermissionGuard).requireFrontKnowledgeBaseEdit(KnowledgeBase.ScopeType.PROJECT);
        when(knowledgeBaseService.updateKnowledgeBase(eq(21L), any(KnowledgeBaseCreateRequest.class)))
                .thenReturn(knowledgeBase(21L, KnowledgeBase.ScopeType.PROJECT));

        mockMvc.perform(put("/api/ai/front/knowledge-bases/21")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scopeType":"PROJECT","projectId":99,"name":"Project KB"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scopeType").value("PROJECT"));

        verify(aiPermissionGuard).requireFrontKnowledgeBaseEdit(KnowledgeBase.ScopeType.PERSONAL);
        verify(aiPermissionGuard).requireFrontKnowledgeBaseEdit(KnowledgeBase.ScopeType.PROJECT);
    }

    @Test
    void pageMineReturnsOwnedKnowledgeBases() throws Exception {
        when(currentUserProvider.requireCurrentUserId()).thenReturn(7L);
        when(knowledgeAccessGuard.pageKnowledgeBasesByOwner(eq(7L), any()))
                .thenReturn(new PageImpl<>(
                        List.of(knowledgeBase(15L, KnowledgeBase.ScopeType.PROJECT)),
                        PageRequest.of(0, 10),
                        1
                ));

        mockMvc.perform(get("/api/ai/front/knowledge-bases/my")
                        .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(15))
                .andExpect(jsonPath("$.data.content[0].scopeType").value("PROJECT"))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(knowledgeAccessGuard).pageKnowledgeBasesByOwner(eq(7L), any());
    }

    @Test
    void uploadDocumentsUsesCurrentUserAsUploader() throws Exception {
        when(currentUserProvider.requireCurrentUserId()).thenReturn(9L);
        when(knowledgeAccessGuard.requireKnowledgeBaseEdit(31L)).thenReturn(knowledgeBase(31L, KnowledgeBase.ScopeType.PROJECT));
        doNothing().when(aiPermissionGuard).requireFrontKnowledgeBaseEdit(KnowledgeBase.ScopeType.PROJECT);

        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(41L);
        document.setKnowledgeBase(knowledgeBase(31L, KnowledgeBase.ScopeType.PROJECT));
        document.setTitle("guide.txt");
        document.setFileName("guide.txt");
        document.setStatus(KnowledgeDocument.Status.UPLOADED);
        when(knowledgeBaseService.uploadDocuments(eq(31L), any(), any(KnowledgeDocumentCreateRequest.class)))
                .thenReturn(List.of(document));

        MockMultipartFile file = new MockMultipartFile("files", "guide.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/api/ai/front/knowledge-bases/31/documents/upload")
                        .file(file)
                        .param("title", "Guide")
                        .param("autoIndex", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(41))
                .andExpect(jsonPath("$.data[0].knowledgeBaseId").value(31));

        ArgumentCaptor<KnowledgeDocumentCreateRequest> captor = ArgumentCaptor.forClass(KnowledgeDocumentCreateRequest.class);
        verify(knowledgeBaseService).uploadDocuments(eq(31L), any(), captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(9L, captor.getValue().getUploadedBy());
        org.junit.jupiter.api.Assertions.assertEquals("Guide", captor.getValue().getTitle());
    }

    @Test
    void getRejectsPlatformKnowledgeBaseFromFrontApi() throws Exception {
        when(knowledgeAccessGuard.requireKnowledgeBaseRead(51L)).thenReturn(knowledgeBase(51L, KnowledgeBase.ScopeType.PLATFORM));
        doThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "forbidden"))
                .when(aiPermissionGuard).requireFrontKnowledgeBaseRead(KnowledgeBase.ScopeType.PLATFORM);

        mockMvc.perform(get("/api/ai/front/knowledge-bases/51"))
                .andExpect(status().isForbidden());

        verify(knowledgeBaseService, never()).getById(any());
    }

    @Test
    void addMemberRejectsOwnerRoleAssignment() throws Exception {
        when(knowledgeAccessGuard.requireKnowledgeBaseOwner(61L)).thenReturn(knowledgeBase(61L, KnowledgeBase.ScopeType.PROJECT));
        doNothing().when(aiPermissionGuard).requireFrontKnowledgeBaseMemberManage(KnowledgeBase.ScopeType.PROJECT);

        mockMvc.perform(post("/api/ai/front/knowledge-bases/61/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("userId", 1001L, "roleCode", "OWNER"))))
                .andExpect(status().isBadRequest());

        verify(knowledgeBaseService, never()).addMember(any(), any());
    }

    @Test
    void listMembersOnlyRequiresReadPermission() throws Exception {
        when(knowledgeAccessGuard.requireKnowledgeBaseRead(71L)).thenReturn(knowledgeBase(71L, KnowledgeBase.ScopeType.PERSONAL));
        doNothing().when(aiPermissionGuard).requireFrontKnowledgeBaseRead(KnowledgeBase.ScopeType.PERSONAL);
        when(knowledgeBaseService.listMembers(71L)).thenReturn(List.of());

        mockMvc.perform(get("/api/ai/front/knowledge-bases/71/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(knowledgeAccessGuard, never()).requireKnowledgeBaseOwner(71L);
        verify(knowledgeBaseService).listMembers(71L);
    }

    @Test
    void getImportTaskUsesFrontReadableGuard() throws Exception {
        KnowledgeBase kb = knowledgeBase(81L, KnowledgeBase.ScopeType.PROJECT);
        KnowledgeImportTask task = new KnowledgeImportTask();
        task.setId(91L);
        task.setKnowledgeBase(kb);
        task.setStatus(KnowledgeImportTask.Status.RUNNING);
        when(knowledgeAccessGuard.requireImportTaskRead(91L)).thenReturn(task);
        doNothing().when(aiPermissionGuard).requireFrontKnowledgeBaseRead(KnowledgeBase.ScopeType.PROJECT);

        mockMvc.perform(get("/api/ai/front/knowledge-bases/import-tasks/91"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(91))
                .andExpect(jsonPath("$.data.knowledgeBaseId").value(81));

        verify(knowledgeImportTaskService, never()).getTask(91L);
    }

    @Test
    void deleteDocumentUsesFrontEditableGuard() throws Exception {
        when(knowledgeAccessGuard.requireKnowledgeBaseEdit(31L)).thenReturn(knowledgeBase(31L, KnowledgeBase.ScopeType.PROJECT));
        doNothing().when(aiPermissionGuard).requireFrontKnowledgeBaseEdit(KnowledgeBase.ScopeType.PROJECT);
        doNothing().when(knowledgeBaseService).deleteDocument(31L, 41L);

        mockMvc.perform(delete("/api/ai/front/knowledge-bases/31/documents/41"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(knowledgeBaseService).deleteDocument(31L, 41L);
    }

    @Test
    void createIndexTaskUsesFrontEditableGuard() throws Exception {
        when(knowledgeAccessGuard.requireKnowledgeBaseEdit(31L)).thenReturn(knowledgeBase(31L, KnowledgeBase.ScopeType.PROJECT));
        doNothing().when(aiPermissionGuard).requireFrontKnowledgeBaseEdit(KnowledgeBase.ScopeType.PROJECT);
        KnowledgeIndexTask task = new KnowledgeIndexTask();
        task.setId(51L);
        task.setKnowledgeBase(knowledgeBase(31L, KnowledgeBase.ScopeType.PROJECT));
        task.setStatus(KnowledgeIndexTask.Status.PENDING);
        task.setTaskType(KnowledgeIndexTask.TaskType.REINDEX);
        when(knowledgeBaseService.createIndexTask(eq(31L), any())).thenReturn(task);

        mockMvc.perform(post("/api/ai/front/knowledge-bases/31/index-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documentId":41}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(51))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(knowledgeBaseService).createIndexTask(eq(31L), any());
    }

    @Test
    void deleteKnowledgeBaseUsesFrontOwnerGuard() throws Exception {
        when(knowledgeAccessGuard.requireKnowledgeBaseOwner(31L)).thenReturn(knowledgeBase(31L, KnowledgeBase.ScopeType.PROJECT));
        doNothing().when(aiPermissionGuard).requireFrontKnowledgeBaseMemberManage(KnowledgeBase.ScopeType.PROJECT);

        mockMvc.perform(delete("/api/ai/front/knowledge-bases/31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(knowledgeBaseService).deleteKnowledgeBase(31L);
    }

    private KnowledgeBase knowledgeBase(Long id, KnowledgeBase.ScopeType scopeType) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(id);
        knowledgeBase.setOwnerId(7L);
        knowledgeBase.setProjectId(99L);
        knowledgeBase.setName("kb-" + id);
        knowledgeBase.setScopeType(scopeType);
        knowledgeBase.setVisibility(KnowledgeBase.Visibility.PRIVATE);
        knowledgeBase.setSourceType(KnowledgeBase.SourceType.MANUAL);
        knowledgeBase.setChunkStrategy(KnowledgeBase.ChunkStrategy.PARAGRAPH);
        knowledgeBase.setStatus(KnowledgeBase.Status.ACTIVE);
        return knowledgeBase;
    }
}
