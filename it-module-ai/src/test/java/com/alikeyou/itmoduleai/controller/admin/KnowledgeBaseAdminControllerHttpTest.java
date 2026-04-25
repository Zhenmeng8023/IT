package com.alikeyou.itmoduleai.controller.admin;

import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseRepository;
import com.alikeyou.itmoduleai.service.KnowledgeBaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseAdminControllerHttpTest {

    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;
    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        KnowledgeBaseAdminController controller = new KnowledgeBaseAdminController(
                knowledgeBaseRepository,
                knowledgeBaseService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void pageReturnsAllKnowledgeBasesWhenNoFilterSpecified() throws Exception {
        when(knowledgeBaseRepository.findAllByOrderByUpdatedAtDesc(any()))
                .thenReturn(new PageImpl<>(List.of(knowledgeBase(3L, KnowledgeBase.Status.ACTIVE)), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/admin/ai/knowledge-bases")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(3));

        verify(knowledgeBaseRepository).findAllByOrderByUpdatedAtDesc(any());
    }

    @Test
    void pageFiltersByScopeType() throws Exception {
        when(knowledgeBaseRepository.findByScopeTypeOrderByUpdatedAtDesc(eq(KnowledgeBase.ScopeType.PLATFORM), any()))
                .thenReturn(new PageImpl<>(List.of(knowledgeBase(4L, KnowledgeBase.Status.ACTIVE)), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/admin/ai/knowledge-bases")
                        .param("page", "0")
                        .param("size", "10")
                        .param("scopeType", "PLATFORM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(4));

        verify(knowledgeBaseRepository).findByScopeTypeOrderByUpdatedAtDesc(eq(KnowledgeBase.ScopeType.PLATFORM), any());
    }

    @Test
    void pageFiltersByScopeTypeAndProjectId() throws Exception {
        when(knowledgeBaseRepository.findByScopeTypeAndProjectIdOrderByUpdatedAtDesc(eq(KnowledgeBase.ScopeType.PROJECT), eq(18L), any()))
                .thenReturn(new PageImpl<>(List.of(knowledgeBase(5L, KnowledgeBase.Status.ACTIVE)), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/admin/ai/knowledge-bases")
                        .param("page", "0")
                        .param("size", "10")
                        .param("scopeType", "PROJECT")
                        .param("projectId", "18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(5));

        verify(knowledgeBaseRepository).findByScopeTypeAndProjectIdOrderByUpdatedAtDesc(eq(KnowledgeBase.ScopeType.PROJECT), eq(18L), any());
    }

    @Test
    void pageFiltersByOwnerIdAndProjectId() throws Exception {
        when(knowledgeBaseRepository.findByOwnerIdAndProjectIdOrderByUpdatedAtDesc(eq(1L), eq(18L), any()))
                .thenReturn(new PageImpl<>(List.of(knowledgeBase(6L, KnowledgeBase.Status.ACTIVE)), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/admin/ai/knowledge-bases")
                        .param("page", "0")
                        .param("size", "10")
                        .param("ownerId", "1")
                        .param("projectId", "18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(6));

        verify(knowledgeBaseRepository).findByOwnerIdAndProjectIdOrderByUpdatedAtDesc(eq(1L), eq(18L), any());
    }

    @Test
    void pageFiltersByScopeTypeOwnerIdAndProjectId() throws Exception {
        when(knowledgeBaseRepository.findByScopeTypeAndOwnerIdAndProjectIdOrderByUpdatedAtDesc(
                eq(KnowledgeBase.ScopeType.PROJECT), eq(1L), eq(18L), any()))
                .thenReturn(new PageImpl<>(List.of(knowledgeBase(7L, KnowledgeBase.Status.ACTIVE)), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/admin/ai/knowledge-bases")
                        .param("page", "0")
                        .param("size", "10")
                        .param("scopeType", "PROJECT")
                        .param("ownerId", "1")
                        .param("projectId", "18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(7));

        verify(knowledgeBaseRepository).findByScopeTypeAndOwnerIdAndProjectIdOrderByUpdatedAtDesc(
                eq(KnowledgeBase.ScopeType.PROJECT), eq(1L), eq(18L), any());
    }

    @Test
    void freezeUpdatesKnowledgeBaseStatus() throws Exception {
        when(knowledgeBaseService.updateKnowledgeBaseStatus(3L, KnowledgeBase.Status.DISABLED))
                .thenReturn(knowledgeBase(3L, KnowledgeBase.Status.DISABLED));

        mockMvc.perform(put("/api/admin/ai/knowledge-bases/3/freeze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        verify(knowledgeBaseService).updateKnowledgeBaseStatus(3L, KnowledgeBase.Status.DISABLED);
    }

    @Test
    void archiveUpdatesKnowledgeBaseStatus() throws Exception {
        when(knowledgeBaseService.updateKnowledgeBaseStatus(3L, KnowledgeBase.Status.ARCHIVED))
                .thenReturn(knowledgeBase(3L, KnowledgeBase.Status.ARCHIVED));

        mockMvc.perform(put("/api/admin/ai/knowledge-bases/3/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        verify(knowledgeBaseService).updateKnowledgeBaseStatus(3L, KnowledgeBase.Status.ARCHIVED);
    }

    @Test
    void deleteDelegatesToKnowledgeBaseService() throws Exception {
        doNothing().when(knowledgeBaseService).deleteKnowledgeBase(3L);

        mockMvc.perform(delete("/api/admin/ai/knowledge-bases/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(knowledgeBaseService).deleteKnowledgeBase(3L);
    }

    private KnowledgeBase knowledgeBase(Long id, KnowledgeBase.Status status) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(id);
        knowledgeBase.setOwnerId(1L);
        knowledgeBase.setProjectId(18L);
        knowledgeBase.setName("测试");
        knowledgeBase.setScopeType(KnowledgeBase.ScopeType.PROJECT);
        knowledgeBase.setVisibility(KnowledgeBase.Visibility.PRIVATE);
        knowledgeBase.setSourceType(KnowledgeBase.SourceType.PROJECT_DOC);
        knowledgeBase.setChunkStrategy(KnowledgeBase.ChunkStrategy.PARAGRAPH);
        knowledgeBase.setStatus(status);
        return knowledgeBase;
    }
}
