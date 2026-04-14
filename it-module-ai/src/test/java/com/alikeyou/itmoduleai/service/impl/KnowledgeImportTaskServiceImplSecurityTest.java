package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.dto.request.KnowledgeDocumentCreateRequest;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeImportTask;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeChunkRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeDocumentRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeImportTaskRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeIndexTaskRepository;
import com.alikeyou.itmoduleai.service.KnowledgeAccessGuard;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeImportTaskServiceImplSecurityTest {

    private KnowledgeImportTaskRepository knowledgeImportTaskRepository;
    private KnowledgeAccessGuard knowledgeAccessGuard;
    private KnowledgeImportTaskServiceImpl service;

    @BeforeEach
    void setUp() {
        knowledgeImportTaskRepository = mock(KnowledgeImportTaskRepository.class);
        knowledgeAccessGuard = mock(KnowledgeAccessGuard.class);

        service = new KnowledgeImportTaskServiceImpl(
                mock(KnowledgeBaseRepository.class),
                mock(KnowledgeDocumentRepository.class),
                mock(KnowledgeChunkRepository.class),
                mock(KnowledgeIndexTaskRepository.class),
                knowledgeImportTaskRepository,
                new ObjectMapper(),
                knowledgeAccessGuard,
                command -> command.run()
        );
    }

    @Test
    void getTaskUsesGuardCheck() {
        KnowledgeImportTask task = task(1L, KnowledgeImportTask.Status.PENDING);
        when(knowledgeAccessGuard.requireImportTaskRead(1L)).thenReturn(task);

        KnowledgeImportTask result = service.getTask(1L);

        assertSame(task, result);
    }

    @Test
    void listByKnowledgeBaseRejectsUnauthorizedUser() {
        when(knowledgeAccessGuard.requireKnowledgeBaseRead(7L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.listByKnowledgeBase(7L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(knowledgeImportTaskRepository, never()).findByKnowledgeBase_IdOrderByCreatedAtDesc(any());
    }

    @Test
    void cancelTaskRejectsUnauthorizedUser() {
        when(knowledgeAccessGuard.requireImportTaskEdit(8L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.cancelTask(8L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(knowledgeImportTaskRepository, never()).save(any(KnowledgeImportTask.class));
    }

    @Test
    void cancelTaskMarksCancelRequestedForRunningTask() {
        KnowledgeImportTask task = task(9L, KnowledgeImportTask.Status.RUNNING);
        when(knowledgeAccessGuard.requireImportTaskEdit(9L)).thenReturn(task);
        doAnswer(invocation -> {
            task.setCancelRequested(Boolean.TRUE);
            return 1;
        }).when(knowledgeImportTaskRepository).requestCancel(any(), any(), any());
        when(knowledgeImportTaskRepository.findById(9L)).thenReturn(Optional.of(task));

        KnowledgeImportTask saved = service.cancelTask(9L);

        assertEquals(Boolean.TRUE, saved.getCancelRequested());
    }

    @Test
    void cancelTaskIsNoOpForFinishedTask() {
        KnowledgeImportTask task = task(11L, KnowledgeImportTask.Status.SUCCESS);
        when(knowledgeAccessGuard.requireImportTaskEdit(11L)).thenReturn(task);

        KnowledgeImportTask saved = service.cancelTask(11L);

        assertSame(task, saved);
        verify(knowledgeImportTaskRepository, never()).requestCancel(any(), any(), any());
        verify(knowledgeImportTaskRepository, never()).save(any(KnowledgeImportTask.class));
    }

    @Test
    void createZipImportTaskRejectsUnauthorizedUser() {
        when(knowledgeAccessGuard.requireKnowledgeBaseEdit(10L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createZipImportTask(10L, mock(MultipartFile.class), new KnowledgeDocumentCreateRequest()));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(knowledgeImportTaskRepository, never()).save(any(KnowledgeImportTask.class));
    }

    private KnowledgeImportTask task(Long id, KnowledgeImportTask.Status status) {
        KnowledgeImportTask task = new KnowledgeImportTask();
        task.setId(id);
        task.setKnowledgeBase(knowledgeBase(100L));
        task.setStatus(status);
        task.setCancelRequested(Boolean.FALSE);
        return task;
    }

    private KnowledgeBase knowledgeBase(Long id) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(id);
        knowledgeBase.setName("kb");
        knowledgeBase.setOwnerId(1L);
        knowledgeBase.setScopeType(KnowledgeBase.ScopeType.PERSONAL);
        knowledgeBase.setVisibility(KnowledgeBase.Visibility.PRIVATE);
        knowledgeBase.setSourceType(KnowledgeBase.SourceType.MANUAL);
        knowledgeBase.setChunkStrategy(KnowledgeBase.ChunkStrategy.PARAGRAPH);
        knowledgeBase.setStatus(KnowledgeBase.Status.ACTIVE);
        return knowledgeBase;
    }
}
