package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeImportTask;
import com.alikeyou.itmoduleai.service.KnowledgeAccessGuard;
import com.alikeyou.itmoduleai.service.KnowledgeImportTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
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
class KnowledgeImportTaskControllerHttpTest {

    @Mock
    private KnowledgeImportTaskService knowledgeImportTaskService;
    @Mock
    private KnowledgeAccessGuard knowledgeAccessGuard;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        KnowledgeImportTaskController controller = new KnowledgeImportTaskController(
                knowledgeImportTaskService,
                knowledgeAccessGuard
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getTaskReturnsTaskAfterReadGuard() throws Exception {
        KnowledgeImportTask task = task(51L, KnowledgeImportTask.Status.RUNNING);
        when(knowledgeAccessGuard.requireImportTaskRead(51L)).thenReturn(task);
        when(knowledgeImportTaskService.getTask(51L)).thenReturn(task);

        mockMvc.perform(get("/api/ai/knowledge-import-tasks/51"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(51))
                .andExpect(jsonPath("$.data.status").value("RUNNING"));
    }

    @Test
    void getTaskReturnsNotFoundWhenGuardCannotLoadTask() throws Exception {
        when(knowledgeAccessGuard.requireImportTaskRead(404L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Knowledge import task not found"));

        mockMvc.perform(get("/api/ai/knowledge-import-tasks/404"))
                .andExpect(status().isNotFound());

        verify(knowledgeImportTaskService, never()).getTask(any());
    }

    @Test
    void listByKnowledgeBaseReturnsTasksAfterKnowledgeBaseReadGuard() throws Exception {
        when(knowledgeAccessGuard.requireKnowledgeBaseRead(12L)).thenReturn(knowledgeBase(12L));
        when(knowledgeImportTaskService.listByKnowledgeBase(12L))
                .thenReturn(List.of(
                        task(52L, KnowledgeImportTask.Status.SUCCESS),
                        task(53L, KnowledgeImportTask.Status.FAILED)));

        mockMvc.perform(get("/api/ai/knowledge-import-tasks/knowledge-base/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.data[1].status").value("FAILED"));
    }

    @Test
    void listByKnowledgeBaseRejectsUnauthorizedUser() throws Exception {
        when(knowledgeAccessGuard.requireKnowledgeBaseRead(12L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden"));

        mockMvc.perform(get("/api/ai/knowledge-import-tasks/knowledge-base/12"))
                .andExpect(status().isForbidden());

        verify(knowledgeImportTaskService, never()).listByKnowledgeBase(any());
    }

    @Test
    void cancelTaskReturnsCancelledTask() throws Exception {
        KnowledgeImportTask task = task(54L, KnowledgeImportTask.Status.CANCELLED);
        task.setCancelRequested(Boolean.TRUE);
        when(knowledgeAccessGuard.requireImportTaskEdit(54L)).thenReturn(task);
        when(knowledgeImportTaskService.cancelTask(54L)).thenReturn(task);

        mockMvc.perform(post("/api/ai/knowledge-import-tasks/54/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(54))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelRequested").value(true));
    }

    @Test
    void cancelTaskRejectsUnauthorizedUser() throws Exception {
        when(knowledgeAccessGuard.requireImportTaskEdit(55L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden"));

        mockMvc.perform(post("/api/ai/knowledge-import-tasks/55/cancel"))
                .andExpect(status().isForbidden());

        verify(knowledgeImportTaskService, never()).cancelTask(any());
    }

    private KnowledgeImportTask task(Long id, KnowledgeImportTask.Status status) {
        KnowledgeImportTask task = new KnowledgeImportTask();
        task.setId(id);
        task.setKnowledgeBase(knowledgeBase(12L));
        task.setZipName("repo.zip");
        task.setStatus(status);
        task.setCurrentStage(status == KnowledgeImportTask.Status.CANCELLED
                ? KnowledgeImportTask.Stage.CANCELLED
                : KnowledgeImportTask.Stage.FINISHED);
        task.setCancelRequested(Boolean.FALSE);
        return task;
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
}
