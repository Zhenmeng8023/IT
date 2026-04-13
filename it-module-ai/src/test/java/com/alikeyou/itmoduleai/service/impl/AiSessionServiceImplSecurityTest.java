package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import com.alikeyou.itmoduleai.dto.request.AiMessageCreateRequest;
import com.alikeyou.itmoduleai.dto.request.AiSessionBindKnowledgeBaseRequest;
import com.alikeyou.itmoduleai.dto.request.AiSessionCreateRequest;
import com.alikeyou.itmoduleai.entity.AiMessage;
import com.alikeyou.itmoduleai.entity.AiSession;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.repository.AiMessageRepository;
import com.alikeyou.itmoduleai.repository.AiModelRepository;
import com.alikeyou.itmoduleai.repository.AiPromptTemplateRepository;
import com.alikeyou.itmoduleai.repository.AiSessionKnowledgeBaseRepository;
import com.alikeyou.itmoduleai.repository.AiSessionRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseMemberRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiSessionServiceImplSecurityTest {

    private AiSessionRepository aiSessionRepository;
    private AiMessageRepository aiMessageRepository;
    private AiSessionKnowledgeBaseRepository aiSessionKnowledgeBaseRepository;
    private KnowledgeBaseRepository knowledgeBaseRepository;
    private KnowledgeBaseMemberRepository knowledgeBaseMemberRepository;
    private AiCurrentUserProvider currentUserProvider;
    private AiSessionServiceImpl service;

    @BeforeEach
    void setUp() {
        aiSessionRepository = mock(AiSessionRepository.class);
        aiMessageRepository = mock(AiMessageRepository.class);
        aiSessionKnowledgeBaseRepository = mock(AiSessionKnowledgeBaseRepository.class);
        knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        knowledgeBaseMemberRepository = mock(KnowledgeBaseMemberRepository.class);
        currentUserProvider = mock(AiCurrentUserProvider.class);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(100L);

        service = new AiSessionServiceImpl(
                aiSessionRepository,
                aiMessageRepository,
                aiSessionKnowledgeBaseRepository,
                mock(AiModelRepository.class),
                mock(AiPromptTemplateRepository.class),
                knowledgeBaseRepository,
                knowledgeBaseMemberRepository,
                currentUserProvider,
                new ObjectMapper()
        );
    }

    @Test
    void createSessionIgnoresRequestUserId() {
        when(aiSessionRepository.save(any(AiSession.class))).thenAnswer(invocation -> {
            AiSession saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(11L);
            }
            return saved;
        });

        AiSessionCreateRequest request = new AiSessionCreateRequest();
        request.setUserId(999L);
        request.setSceneCode("general.chat");
        request.setSessionTitle("chat");

        AiSession created = service.createSession(request);

        assertEquals(100L, created.getUserId());
    }

    @Test
    void createMessageIgnoresRequestSenderUserId() {
        when(aiSessionRepository.findById(10L)).thenReturn(Optional.of(session(10L, 100L)));
        when(aiSessionRepository.save(any(AiSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiMessageRepository.save(any(AiMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiMessageCreateRequest request = new AiMessageCreateRequest();
        request.setSenderUserId(999L);
        request.setContent("hello");

        AiMessage message = service.createMessage(10L, request);

        assertEquals(100L, message.getSenderUserId());
    }

    @Test
    void cannotReadOtherUsersSession() {
        when(aiSessionRepository.findById(10L)).thenReturn(Optional.of(session(10L, 200L)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.getSession(10L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void cannotDeleteOtherUsersSession() {
        when(aiSessionRepository.findById(10L)).thenReturn(Optional.of(session(10L, 200L)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.delete(10L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(aiSessionRepository, never()).save(any(AiSession.class));
    }

    @Test
    void cannotPageMessagesFromOtherUsersSession() {
        when(aiSessionRepository.findById(10L)).thenReturn(Optional.of(session(10L, 200L)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.pageMessages(10L, Pageable.unpaged()));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(aiMessageRepository, never()).findBySession_IdOrderByCreatedAtAsc(any(), any());
    }

    @Test
    void cannotBindAnotherUsersPrivateKnowledgeBase() {
        when(aiSessionRepository.findById(10L)).thenReturn(Optional.of(session(10L, 100L)));
        when(knowledgeBaseRepository.findById(99L)).thenReturn(Optional.of(knowledgeBase(99L, 200L)));
        when(knowledgeBaseMemberRepository.existsByKnowledgeBase_IdAndUserId(99L, 100L)).thenReturn(false);

        AiSessionBindKnowledgeBaseRequest request = new AiSessionBindKnowledgeBaseRequest();
        request.setKnowledgeBaseIds(List.of(99L));
        request.setDefaultKnowledgeBaseId(99L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.bindKnowledgeBases(10L, request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(aiSessionKnowledgeBaseRepository, never()).deleteBySession_Id(any());
        verify(aiSessionKnowledgeBaseRepository, never()).saveAll(any());
    }

    private AiSession session(Long id, Long userId) {
        AiSession session = new AiSession();
        session.setId(id);
        session.setUserId(userId);
        session.setBizType(AiSession.BizType.GENERAL);
        session.setSceneCode("general.chat");
        session.setSessionTitle("chat");
        session.setMemoryMode(AiSession.MemoryMode.SHORT);
        session.setStatus(AiSession.Status.ACTIVE);
        session.setCreatedAt(Instant.now());
        session.setUpdatedAt(Instant.now());
        return session;
    }

    private KnowledgeBase knowledgeBase(Long id, Long ownerId) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(id);
        knowledgeBase.setOwnerId(ownerId);
        knowledgeBase.setName("private kb");
        knowledgeBase.setScopeType(KnowledgeBase.ScopeType.PERSONAL);
        knowledgeBase.setVisibility(KnowledgeBase.Visibility.PRIVATE);
        knowledgeBase.setStatus(KnowledgeBase.Status.ACTIVE);
        knowledgeBase.setSourceType(KnowledgeBase.SourceType.MANUAL);
        knowledgeBase.setChunkStrategy(KnowledgeBase.ChunkStrategy.PARAGRAPH);
        return knowledgeBase;
    }
}
