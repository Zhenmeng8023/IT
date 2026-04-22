package com.alikeyou.itmoduleai.service;

import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeBaseMember;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.alikeyou.itmoduleai.entity.KnowledgeImportTask;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseMemberRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeDocumentRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeImportTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeAccessGuardTest {

    private KnowledgeBaseRepository knowledgeBaseRepository;
    private KnowledgeBaseMemberRepository knowledgeBaseMemberRepository;
    private KnowledgeDocumentRepository knowledgeDocumentRepository;
    private KnowledgeImportTaskRepository knowledgeImportTaskRepository;
    private AiCurrentUserProvider currentUserProvider;
    private KnowledgeAccessGuard guard;

    @BeforeEach
    void setUp() {
        knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        knowledgeBaseMemberRepository = mock(KnowledgeBaseMemberRepository.class);
        knowledgeDocumentRepository = mock(KnowledgeDocumentRepository.class);
        knowledgeImportTaskRepository = mock(KnowledgeImportTaskRepository.class);
        currentUserProvider = mock(AiCurrentUserProvider.class);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(10L);
        when(currentUserProvider.hasAuthority("view:admin:ai:knowledge")).thenReturn(false);

        guard = new KnowledgeAccessGuard(
                knowledgeBaseRepository,
                knowledgeBaseMemberRepository,
                knowledgeDocumentRepository,
                knowledgeImportTaskRepository,
                currentUserProvider
        );
    }

    @Test
    void nonMemberCannotReadKnowledgeBase() {
        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(knowledgeBase(1L, 99L)));
        when(knowledgeBaseMemberRepository.findByKnowledgeBase_IdAndUserId(1L, 10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> guard.requireKnowledgeBaseRead(1L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void viewerCanReadDocument() {
        KnowledgeBase knowledgeBase = knowledgeBase(1L, 99L);
        KnowledgeDocument document = document(100L, knowledgeBase);
        when(knowledgeDocumentRepository.findById(100L)).thenReturn(Optional.of(document));
        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(knowledgeBase));
        when(knowledgeBaseMemberRepository.findByKnowledgeBase_IdAndUserId(1L, 10L))
                .thenReturn(Optional.of(member(1L, 10L, KnowledgeBaseMember.RoleCode.VIEWER)));

        KnowledgeDocument checked = guard.requireDocumentRead(100L);

        assertNotNull(checked);
        assertEquals(100L, checked.getId());
    }

    @Test
    void viewerCannotEditImportTask() {
        KnowledgeBase knowledgeBase = knowledgeBase(1L, 99L);
        KnowledgeImportTask task = importTask(5L, knowledgeBase);
        when(knowledgeImportTaskRepository.findById(5L)).thenReturn(Optional.of(task));
        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(knowledgeBase));
        when(knowledgeBaseMemberRepository.findByKnowledgeBase_IdAndUserId(1L, 10L))
                .thenReturn(Optional.of(member(1L, 10L, KnowledgeBaseMember.RoleCode.VIEWER)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> guard.requireImportTaskEdit(5L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void nonMemberCannotReadImportTask() {
        KnowledgeBase knowledgeBase = knowledgeBase(1L, 99L);
        KnowledgeImportTask task = importTask(6L, knowledgeBase);
        when(knowledgeImportTaskRepository.findById(6L)).thenReturn(Optional.of(task));
        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(knowledgeBase));
        when(knowledgeBaseMemberRepository.findByKnowledgeBase_IdAndUserId(1L, 10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> guard.requireImportTaskRead(6L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void adminCanPassOwnerGuard() {
        when(currentUserProvider.hasAuthority("view:admin:ai:knowledge")).thenReturn(true);
        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(knowledgeBase(1L, 99L)));

        KnowledgeBase checked = guard.requireKnowledgeBaseOwner(1L);

        assertNotNull(checked);
        verify(knowledgeBaseMemberRepository, never()).findByKnowledgeBase_IdAndUserId(1L, 10L);
    }

    @Test
    void editorIsNotOwnerAccess() {
        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(knowledgeBase(1L, 99L)));
        when(knowledgeBaseMemberRepository.findByKnowledgeBase_IdAndUserId(1L, 10L))
                .thenReturn(Optional.of(member(1L, 10L, KnowledgeBaseMember.RoleCode.EDITOR)));

        boolean ownerAccess = guard.hasKnowledgeBaseOwnerAccess(1L);

        assertFalse(ownerAccess);
    }

    private KnowledgeBase knowledgeBase(Long id, Long ownerId) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(id);
        knowledgeBase.setOwnerId(ownerId);
        knowledgeBase.setName("kb");
        knowledgeBase.setScopeType(KnowledgeBase.ScopeType.PERSONAL);
        knowledgeBase.setVisibility(KnowledgeBase.Visibility.PRIVATE);
        knowledgeBase.setSourceType(KnowledgeBase.SourceType.MANUAL);
        knowledgeBase.setChunkStrategy(KnowledgeBase.ChunkStrategy.PARAGRAPH);
        knowledgeBase.setStatus(KnowledgeBase.Status.ACTIVE);
        return knowledgeBase;
    }

    private KnowledgeBaseMember member(Long knowledgeBaseId, Long userId, KnowledgeBaseMember.RoleCode roleCode) {
        KnowledgeBaseMember member = new KnowledgeBaseMember();
        member.setKnowledgeBase(knowledgeBase(knowledgeBaseId, 99L));
        member.setUserId(userId);
        member.setRoleCode(roleCode);
        return member;
    }

    private KnowledgeDocument document(Long id, KnowledgeBase knowledgeBase) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(id);
        document.setKnowledgeBase(knowledgeBase);
        document.setTitle("doc");
        document.setStatus(KnowledgeDocument.Status.UPLOADED);
        return document;
    }

    private KnowledgeImportTask importTask(Long id, KnowledgeBase knowledgeBase) {
        KnowledgeImportTask task = new KnowledgeImportTask();
        task.setId(id);
        task.setKnowledgeBase(knowledgeBase);
        task.setStatus(KnowledgeImportTask.Status.PENDING);
        return task;
    }
}
