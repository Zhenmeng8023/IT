package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.dto.request.KnowledgeDocumentCreateRequest;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeImportTask;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeChunkEmbeddingRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeChunkRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeDocumentRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeImportTaskRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeIndexTaskRepository;
import com.alikeyou.itmoduleai.service.CodeIndexService;
import com.alikeyou.itmoduleai.service.KnowledgeAccessGuard;
import com.alikeyou.itmoduleai.service.KnowledgeChunkingService;
import com.alikeyou.itmoduleai.service.KnowledgeEmbeddingService;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

        service = newService(executor);
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
    void corruptedZipTransitionsToFailedAndCleansImportDirectory() throws Exception {
        KnowledgeBase kb = knowledgeBase(4L);
        when(knowledgeAccessGuard.requireKnowledgeBaseEdit(4L)).thenReturn(kb);

        KnowledgeImportTask created = service.createZipImportTask(4L, corruptZipFile(), new KnowledgeDocumentCreateRequest());
        Path taskDir = Path.of(created.getTempZipPath()).getParent();

        executor.runNext();

        KnowledgeImportTask task = taskStore.get(created.getId());
        assertThat(task.getStatus()).isEqualTo(KnowledgeImportTask.Status.FAILED);
        assertThat(task.getCurrentStage()).isEqualTo(KnowledgeImportTask.Stage.FINISHED);
        assertThat(task.getFinishedAt()).isNotNull();
        assertThat(task.getErrorMessage()).isNotBlank();
        assertThat(taskDir).doesNotExist();
    }

    @Test
    void successfulEmptyZipImportCleansImportDirectory() throws Exception {
        KnowledgeBase kb = knowledgeBase(5L);
        when(knowledgeAccessGuard.requireKnowledgeBaseEdit(5L)).thenReturn(kb);

        KnowledgeImportTask created = service.createZipImportTask(5L, emptyZipFile(), new KnowledgeDocumentCreateRequest());
        Path taskDir = Path.of(created.getTempZipPath()).getParent();

        executor.runNext();

        KnowledgeImportTask task = taskStore.get(created.getId());
        assertThat(task.getStatus()).isEqualTo(KnowledgeImportTask.Status.SUCCESS);
        assertThat(task.getProgressPercent()).isEqualTo(100);
        assertThat(taskDir).doesNotExist();
    }

    @Test
    void cancelCanBeRequestedRepeatedlyBeforeRunnerConsumesIt() throws Exception {
        KnowledgeBase kb = knowledgeBase(6L);
        when(knowledgeAccessGuard.requireKnowledgeBaseEdit(6L)).thenReturn(kb);
        KnowledgeImportTask created = service.createZipImportTask(6L, emptyZipFile(), new KnowledgeDocumentCreateRequest());
        when(knowledgeAccessGuard.requireImportTaskEdit(created.getId())).thenAnswer(invocation -> taskStore.get(created.getId()));

        service.cancelTask(created.getId());
        service.cancelTask(created.getId());

        KnowledgeImportTask task = taskStore.get(created.getId());
        assertThat(task.getCancelRequested()).isTrue();
        assertThat(task.getStatus()).isEqualTo(KnowledgeImportTask.Status.PENDING);
        verify(knowledgeImportTaskRepository, times(2)).requestCancel(any(), any(), any());
    }

    @Test
    void terminalTaskCancelDoesNotChangeState() {
        KnowledgeImportTask task = task(700L, KnowledgeImportTask.Status.SUCCESS);
        taskStore.put(task.getId(), task);
        when(knowledgeAccessGuard.requireImportTaskEdit(700L)).thenReturn(task);

        KnowledgeImportTask result = service.cancelTask(700L);

        assertThat(result).isSameAs(task);
        assertThat(result.getStatus()).isEqualTo(KnowledgeImportTask.Status.SUCCESS);
        verify(knowledgeImportTaskRepository, never()).requestCancel(any(), any(), any());
    }

    @Test
    void cancellationTransitionCleansImportDirectory() throws Exception {
        KnowledgeBase kb = knowledgeBase(7L);
        when(knowledgeAccessGuard.requireKnowledgeBaseEdit(7L)).thenReturn(kb);
        KnowledgeImportTask created = service.createZipImportTask(7L, zipWithTextFile(), new KnowledgeDocumentCreateRequest());
        Path taskDir = Path.of(created.getTempZipPath()).getParent();
        when(knowledgeAccessGuard.requireImportTaskEdit(created.getId())).thenAnswer(invocation -> taskStore.get(created.getId()));

        service.cancelTask(created.getId());
        executor.runNext();

        KnowledgeImportTask task = taskStore.get(created.getId());
        assertThat(task.getStatus()).isEqualTo(KnowledgeImportTask.Status.CANCELLED);
        assertThat(task.getCurrentStage()).isEqualTo(KnowledgeImportTask.Stage.CANCELLED);
        assertThat(taskDir).doesNotExist();
    }

    @Test
    void repeatedRunnerAfterFailureDoesNotRetryTerminalTask() throws Exception {
        KnowledgeBase kb = knowledgeBase(8L);
        when(knowledgeAccessGuard.requireKnowledgeBaseEdit(8L)).thenReturn(kb);
        KnowledgeImportTask created = service.createZipImportTask(8L, corruptZipFile(), new KnowledgeDocumentCreateRequest());
        Runnable runner = executor.peekFirst();

        runner.run();
        runner.run();

        KnowledgeImportTask task = taskStore.get(created.getId());
        assertThat(task.getStatus()).isEqualTo(KnowledgeImportTask.Status.FAILED);
        assertThat(task.getCurrentStage()).isEqualTo(KnowledgeImportTask.Stage.FINISHED);
    }

    private KnowledgeImportTaskServiceImpl newService(Executor executor) {
        return new KnowledgeImportTaskServiceImpl(
                mock(KnowledgeBaseRepository.class),
                mock(KnowledgeDocumentRepository.class),
                mock(KnowledgeChunkRepository.class),
                mock(KnowledgeChunkEmbeddingRepository.class),
                mock(KnowledgeIndexTaskRepository.class),
                knowledgeImportTaskRepository,
                mock(KnowledgeChunkingService.class),
                mock(CodeIndexService.class),
                mock(KnowledgeEmbeddingService.class),
                knowledgeAccessGuard,
                executor
        );
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

    private MockMultipartFile corruptZipFile() {
        return new MockMultipartFile("file", "repo.zip", "application/zip", "not-a-zip".getBytes());
    }

    private MockMultipartFile zipWithTextFile() throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(bos)) {
            zos.putNextEntry(new ZipEntry("docs/readme.md"));
            zos.write("hello".getBytes());
            zos.closeEntry();
            zos.finish();
            return new MockMultipartFile("file", "repo.zip", "application/zip", bos.toByteArray());
        }
    }

    private KnowledgeImportTask task(Long id, KnowledgeImportTask.Status status) {
        KnowledgeImportTask task = new KnowledgeImportTask();
        task.setId(id);
        task.setKnowledgeBase(knowledgeBase(100L));
        task.setZipName("repo.zip");
        task.setStatus(status);
        task.setCurrentStage(KnowledgeImportTask.Stage.FINISHED);
        task.setCancelRequested(Boolean.FALSE);
        task.setProgressPercent(status == KnowledgeImportTask.Status.SUCCESS ? 100 : 0);
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
