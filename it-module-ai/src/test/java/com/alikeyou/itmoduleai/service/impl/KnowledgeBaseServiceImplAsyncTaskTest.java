package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import com.alikeyou.itmoduleai.dto.request.KnowledgeDocumentCreateRequest;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.alikeyou.itmoduleai.entity.KnowledgeIndexTask;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseMemberRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeChunkEmbeddingRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeChunkRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeDocumentRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeIndexTaskRepository;
import com.alikeyou.itmoduleai.service.CodeIndexService;
import com.alikeyou.itmoduleai.service.KnowledgeAccessGuard;
import com.alikeyou.itmoduleai.service.KnowledgeChunkingService;
import com.alikeyou.itmoduleai.service.KnowledgeEmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeBaseServiceImplAsyncTaskTest {

    private KnowledgeBaseRepository knowledgeBaseRepository;
    private KnowledgeDocumentRepository knowledgeDocumentRepository;
    private KnowledgeChunkRepository knowledgeChunkRepository;
    private KnowledgeIndexTaskRepository knowledgeIndexTaskRepository;
    private KnowledgeAccessGuard knowledgeAccessGuard;
    private CapturingExecutor executor;
    private KnowledgeBaseServiceImpl service;

    private final AtomicLong taskIdGenerator = new AtomicLong(100L);
    private final Map<Long, KnowledgeIndexTask> taskStore = new HashMap<>();

    @BeforeEach
    void setUp() {
        knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        KnowledgeBaseMemberRepository knowledgeBaseMemberRepository = mock(KnowledgeBaseMemberRepository.class);
        knowledgeDocumentRepository = mock(KnowledgeDocumentRepository.class);
        knowledgeChunkRepository = mock(KnowledgeChunkRepository.class);
        knowledgeIndexTaskRepository = mock(KnowledgeIndexTaskRepository.class);
        knowledgeAccessGuard = mock(KnowledgeAccessGuard.class);
        executor = new CapturingExecutor();

        AiCurrentUserProvider currentUserProvider = mock(AiCurrentUserProvider.class);
        service = new KnowledgeBaseServiceImpl(
                knowledgeBaseRepository,
                knowledgeBaseMemberRepository,
                knowledgeDocumentRepository,
                knowledgeChunkRepository,
                mock(KnowledgeChunkEmbeddingRepository.class),
                knowledgeIndexTaskRepository,
                mock(KnowledgeChunkingService.class),
                mock(CodeIndexService.class),
                mock(KnowledgeEmbeddingService.class),
                knowledgeAccessGuard,
                currentUserProvider,
                executor
        );
        stubIndexTaskRepositoryStateMachine();
    }

    @Test
    void createIndexTaskReturnsPendingAndRunsInBackground() {
        KnowledgeBase kb = knowledgeBase(1L);
        when(knowledgeAccessGuard.requireKnowledgeBaseEdit(1L)).thenReturn(kb);
        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(kb));
        when(knowledgeBaseRepository.save(any(KnowledgeBase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(knowledgeDocumentRepository.findByKnowledgeBase_IdOrderByIdAsc(1L)).thenReturn(List.of());

        KnowledgeIndexTask created = service.createIndexTask(1L, null);

        assertThat(created.getStatus()).isEqualTo(KnowledgeIndexTask.Status.PENDING);
        assertThat(created.getStartedAt()).isNull();
        assertThat(created.getFinishedAt()).isNull();
        assertThat(executor.size()).isEqualTo(1);
        verify(knowledgeIndexTaskRepository, never())
                .transitionStatusToRunning(eq(created.getId()), any(), any(), any(), any());

        executor.runNext();

        assertThat(taskStore.get(created.getId()).getStatus()).isEqualTo(KnowledgeIndexTask.Status.SUCCESS);
    }

    @Test
    void autoIndexTaskIsQueuedNotExecutedInline() {
        KnowledgeBase kb = knowledgeBase(2L);
        when(knowledgeAccessGuard.requireKnowledgeBaseEdit(2L)).thenReturn(kb);
        when(knowledgeBaseRepository.findById(2L)).thenReturn(Optional.of(kb));
        when(knowledgeBaseRepository.save(any(KnowledgeBase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(knowledgeDocumentRepository.findByKnowledgeBase_IdOrderByIdAsc(2L)).thenReturn(List.of());

        AtomicLong docIdGenerator = new AtomicLong(200L);
        Map<Long, KnowledgeDocument> documents = new HashMap<>();
        when(knowledgeDocumentRepository.save(any(KnowledgeDocument.class))).thenAnswer(invocation -> {
            KnowledgeDocument doc = invocation.getArgument(0);
            if (doc.getId() == null) {
                doc.setId(docIdGenerator.getAndIncrement());
            }
            if (doc.getCreatedAt() == null) {
                doc.setCreatedAt(Instant.now());
            }
            doc.setUpdatedAt(Instant.now());
            documents.put(doc.getId(), doc);
            return doc;
        });
        when(knowledgeDocumentRepository.findById(any())).thenAnswer(invocation -> Optional.ofNullable(documents.get(invocation.getArgument(0))));
        when(knowledgeChunkRepository.findByDocument_IdOrderByChunkIndexAsc(any())).thenReturn(List.of());

        KnowledgeDocumentCreateRequest request = new KnowledgeDocumentCreateRequest();
        request.setTitle("doc");
        request.setContentText("hello async index");
        request.setAutoIndex(Boolean.TRUE);

        KnowledgeDocument createdDoc = service.addDocument(2L, request);

        assertThat(createdDoc.getStatus()).isEqualTo(KnowledgeDocument.Status.UPLOADED);
        assertThat(executor.size()).isEqualTo(1);
        verify(knowledgeIndexTaskRepository, never())
                .transitionStatusToRunning(any(), any(), any(), any(), any());
    }

    @Test
    void duplicateRunnerExecutionStartsTaskOnlyOnce() {
        KnowledgeBase kb = knowledgeBase(3L);
        when(knowledgeAccessGuard.requireKnowledgeBaseEdit(3L)).thenReturn(kb);
        when(knowledgeBaseRepository.findById(3L)).thenReturn(Optional.of(kb));
        when(knowledgeBaseRepository.save(any(KnowledgeBase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(knowledgeDocumentRepository.findByKnowledgeBase_IdOrderByIdAsc(3L)).thenReturn(List.of());

        KnowledgeIndexTask created = service.createIndexTask(3L, null);
        Runnable runner = executor.peekFirst();
        assertThat(runner).isNotNull();

        runner.run();
        runner.run();

        verify(knowledgeIndexTaskRepository).transitionStatusFromRunning(
                eq(created.getId()),
                eq(KnowledgeIndexTask.Status.RUNNING),
                eq(KnowledgeIndexTask.Status.SUCCESS),
                eq((String) null),
                any(),
                any()
        );
        assertThat(taskStore.get(created.getId()).getStatus()).isEqualTo(KnowledgeIndexTask.Status.SUCCESS);
    }

    private void stubIndexTaskRepositoryStateMachine() {
        when(knowledgeIndexTaskRepository.save(any(KnowledgeIndexTask.class))).thenAnswer(invocation -> {
            KnowledgeIndexTask task = invocation.getArgument(0);
            if (task.getId() == null) {
                task.setId(taskIdGenerator.getAndIncrement());
            }
            taskStore.put(task.getId(), task);
            return task;
        });
        when(knowledgeIndexTaskRepository.findById(any())).thenAnswer(invocation -> Optional.ofNullable(taskStore.get(invocation.getArgument(0))));
        when(knowledgeIndexTaskRepository.findByIdWithRelations(any())).thenAnswer(invocation -> Optional.ofNullable(taskStore.get(invocation.getArgument(0))));
        when(knowledgeIndexTaskRepository.transitionStatusToRunning(any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            KnowledgeIndexTask.Status from = invocation.getArgument(1);
            KnowledgeIndexTask.Status to = invocation.getArgument(2);
            Instant startedAt = invocation.getArgument(3);
            Instant updatedAt = invocation.getArgument(4);
            KnowledgeIndexTask task = taskStore.get(id);
            if (task == null || task.getStatus() != from) {
                return 0;
            }
            task.setStatus(to);
            task.setStartedAt(startedAt);
            task.setUpdatedAt(updatedAt);
            task.setErrorMessage(null);
            return 1;
        });
        when(knowledgeIndexTaskRepository.transitionStatusFromRunning(any(), any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            KnowledgeIndexTask.Status from = invocation.getArgument(1);
            KnowledgeIndexTask.Status to = invocation.getArgument(2);
            String error = invocation.getArgument(3);
            Instant finishedAt = invocation.getArgument(4);
            Instant updatedAt = invocation.getArgument(5);
            KnowledgeIndexTask task = taskStore.get(id);
            if (task == null || task.getStatus() != from) {
                return 0;
            }
            task.setStatus(to);
            task.setErrorMessage(error);
            task.setFinishedAt(finishedAt);
            task.setUpdatedAt(updatedAt);
            return 1;
        });
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
