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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeImportTaskServiceImplAsyncTaskTest {

    private KnowledgeImportTaskRepository knowledgeImportTaskRepository;
    private KnowledgeAccessGuard knowledgeAccessGuard;
    private CapturingExecutor executor;
    private KnowledgeImportTaskServiceImpl service;

    private final AtomicLong taskIdGenerator = new AtomicLong(500L);
    private final Map<Long, KnowledgeImportTask> taskStore = new HashMap<>();

    @BeforeEach
    void setUp() throws IOException {
        knowledgeImportTaskRepository = mock(KnowledgeImportTaskRepository.class);
        knowledgeAccessGuard = mock(KnowledgeAccessGuard.class);
        executor = new CapturingExecutor();

        service = new KnowledgeImportTaskServiceImpl(
                mock(KnowledgeBaseRepository.class),
                mock(KnowledgeDocumentRepository.class),
                mock(KnowledgeChunkRepository.class),
                mock(KnowledgeIndexTaskRepository.class),
                knowledgeImportTaskRepository,
                new ObjectMapper(),
                knowledgeAccessGuard,
                executor
        );
        Path storageRoot = Files.createTempDirectory("kb-import-test-storage-");
        Path importRoot = Files.createTempDirectory("kb-import-test-import-");
        ReflectionTestUtils.setField(service, "storageRoot", storageRoot.toString());
        ReflectionTestUtils.setField(service, "importRoot", importRoot.toString());
        stubImportTaskRepositoryStateMachine();
    }

    @Test
    void createZipImportTaskReturnsPendingAndQueuesRunner() throws Exception {
        KnowledgeBase kb = knowledgeBase(1L);
        when(knowledgeAccessGuard.requireKnowledgeBaseEdit(1L)).thenReturn(kb);
        MockMultipartFile zip = emptyZipFile();

        KnowledgeImportTask created = service.createZipImportTask(1L, zip, new KnowledgeDocumentCreateRequest());

        assertThat(created.getStatus()).isEqualTo(KnowledgeImportTask.Status.PENDING);
        assertThat(executor.size()).isEqualTo(1);
        verify(knowledgeImportTaskRepository, never()).transitionStatusToRunning(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void cancelMarksRequestAndRunnerTransitionsToCancelled() throws Exception {
        KnowledgeBase kb = knowledgeBase(2L);
        when(knowledgeAccessGuard.requireKnowledgeBaseEdit(2L)).thenReturn(kb);
        MockMultipartFile zip = emptyZipFile();

        KnowledgeImportTask created = service.createZipImportTask(2L, zip, new KnowledgeDocumentCreateRequest());
        when(knowledgeAccessGuard.requireImportTaskEdit(created.getId())).thenAnswer(invocation -> taskStore.get(created.getId()));
        when(knowledgeAccessGuard.requireImportTaskRead(created.getId())).thenAnswer(invocation -> taskStore.get(created.getId()));

        service.cancelTask(created.getId());
        executor.runNext();

        KnowledgeImportTask task = taskStore.get(created.getId());
        assertThat(task.getCancelRequested()).isTrue();
        assertThat(task.getStatus()).isEqualTo(KnowledgeImportTask.Status.CANCELLED);
        assertThat(task.getCurrentStage()).isEqualTo(KnowledgeImportTask.Stage.CANCELLED);
    }

    @Test
    void duplicateRunnerExecutionIsBlockedByStateMachine() throws Exception {
        KnowledgeBase kb = knowledgeBase(3L);
        when(knowledgeAccessGuard.requireKnowledgeBaseEdit(3L)).thenReturn(kb);
        MockMultipartFile zip = emptyZipFile();

        KnowledgeImportTask created = service.createZipImportTask(3L, zip, new KnowledgeDocumentCreateRequest());
        Runnable runner = executor.peekFirst();
        assertThat(runner).isNotNull();

        runner.run();
        runner.run();

        KnowledgeImportTask task = taskStore.get(created.getId());
        assertThat(task.getStatus()).isEqualTo(KnowledgeImportTask.Status.SUCCESS);
    }

    private void stubImportTaskRepositoryStateMachine() {
        when(knowledgeImportTaskRepository.save(any(KnowledgeImportTask.class))).thenAnswer(invocation -> {
            KnowledgeImportTask task = invocation.getArgument(0);
            if (task.getId() == null) {
                task.setId(taskIdGenerator.getAndIncrement());
            }
            if (task.getCreatedAt() == null) {
                task.setCreatedAt(Instant.now());
            }
            taskStore.put(task.getId(), task);
            return task;
        });
        when(knowledgeImportTaskRepository.findById(any())).thenAnswer(invocation -> Optional.ofNullable(taskStore.get(invocation.getArgument(0))));
        when(knowledgeImportTaskRepository.requestCancel(any(), any(), any())).thenAnswer(invocation -> {
            Long taskId = invocation.getArgument(0);
            Instant updatedAt = invocation.getArgument(1);
            @SuppressWarnings("unchecked")
            List<KnowledgeImportTask.Status> active = invocation.getArgument(2);
            KnowledgeImportTask task = taskStore.get(taskId);
            if (task == null || !active.contains(task.getStatus())) {
                return 0;
            }
            task.setCancelRequested(Boolean.TRUE);
            task.setUpdatedAt(updatedAt);
            return 1;
        });
        when(knowledgeImportTaskRepository.transitionStatusToRunning(any(), any(), any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            Long taskId = invocation.getArgument(0);
            KnowledgeImportTask.Status from = invocation.getArgument(1);
            KnowledgeImportTask.Status to = invocation.getArgument(2);
            KnowledgeImportTask.Stage stage = invocation.getArgument(3);
            Integer progress = invocation.getArgument(4);
            String currentFile = invocation.getArgument(5);
            Instant updatedAt = invocation.getArgument(6);
            KnowledgeImportTask task = taskStore.get(taskId);
            if (task == null || task.getStatus() != from) {
                return 0;
            }
            task.setStatus(to);
            task.setCurrentStage(stage);
            task.setProgressPercent(progress);
            task.setCurrentFile(currentFile);
            task.setErrorMessage(null);
            task.setUpdatedAt(updatedAt);
            return 1;
        });
        when(knowledgeImportTaskRepository.transitionStatusFromRunning(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            Long taskId = invocation.getArgument(0);
            KnowledgeImportTask.Status from = invocation.getArgument(1);
            KnowledgeImportTask.Status to = invocation.getArgument(2);
            KnowledgeImportTask.Stage stage = invocation.getArgument(3);
            Integer progress = invocation.getArgument(4);
            String currentFile = invocation.getArgument(5);
            String errorMessage = invocation.getArgument(6);
            Instant finishedAt = invocation.getArgument(7);
            Instant updatedAt = invocation.getArgument(8);
            KnowledgeImportTask task = taskStore.get(taskId);
            if (task == null || task.getStatus() != from) {
                return 0;
            }
            task.setStatus(to);
            task.setCurrentStage(stage);
            task.setProgressPercent(progress);
            task.setCurrentFile(currentFile);
            task.setErrorMessage(errorMessage);
            task.setFinishedAt(finishedAt);
            task.setUpdatedAt(updatedAt);
            return 1;
        });
    }

    private MockMultipartFile emptyZipFile() throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(bos)) {
            zos.finish();
            return new MockMultipartFile("file", "repo.zip", "application/zip", bos.toByteArray());
        }
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

    private static final class CapturingExecutor implements Executor {
        private final List<Runnable> tasks = new ArrayList<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }

        int size() {
            return tasks.size();
        }

        Runnable peekFirst() {
            return tasks.isEmpty() ? null : tasks.get(0);
        }

        void runNext() {
            Runnable task = tasks.remove(0);
            task.run();
        }
    }
}
