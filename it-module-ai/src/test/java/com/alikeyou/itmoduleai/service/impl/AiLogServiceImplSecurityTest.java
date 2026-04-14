package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import com.alikeyou.itmoduleai.dto.request.AiFeedbackCreateRequest;
import com.alikeyou.itmoduleai.entity.AiCallLog;
import com.alikeyou.itmoduleai.entity.AiFeedbackLog;
import com.alikeyou.itmoduleai.entity.AiMessage;
import com.alikeyou.itmoduleai.entity.AiSession;
import com.alikeyou.itmoduleai.repository.AiCallLogRepository;
import com.alikeyou.itmoduleai.repository.AiFeedbackLogRepository;
import com.alikeyou.itmoduleai.repository.AiMessageRepository;
import com.alikeyou.itmoduleai.repository.AiRetrievalLogRepository;
import com.alikeyou.itmoduleai.repository.AiSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiLogServiceImplSecurityTest {

    private AiCallLogRepository aiCallLogRepository;
    private AiRetrievalLogRepository aiRetrievalLogRepository;
    private AiFeedbackLogRepository aiFeedbackLogRepository;
    private AiMessageRepository aiMessageRepository;
    private AiSessionRepository aiSessionRepository;
    private AiCurrentUserProvider currentUserProvider;
    private AiLogServiceImpl service;

    @BeforeEach
    void setUp() {
        aiCallLogRepository = mock(AiCallLogRepository.class);
        aiRetrievalLogRepository = mock(AiRetrievalLogRepository.class);
        aiFeedbackLogRepository = mock(AiFeedbackLogRepository.class);
        aiMessageRepository = mock(AiMessageRepository.class);
        aiSessionRepository = mock(AiSessionRepository.class);
        currentUserProvider = mock(AiCurrentUserProvider.class);

        service = new AiLogServiceImpl(
                aiCallLogRepository,
                aiRetrievalLogRepository,
                aiFeedbackLogRepository,
                aiMessageRepository,
                aiSessionRepository,
                currentUserProvider
        );
    }

    @Test
    void pageUserCallLogsClampsNonAdminToCurrentUser() {
        when(currentUserProvider.hasAuthority("view:ai:log")).thenReturn(false);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(10L);

        service.pageUserCallLogs(99L, Pageable.unpaged());

        verify(aiCallLogRepository).findByUserIdOrderByCreatedAtDesc(10L, Pageable.unpaged());
        verify(aiCallLogRepository, never()).findByUserIdOrderByCreatedAtDesc(99L, Pageable.unpaged());
    }

    @Test
    void pageSessionCallLogsRejectsOtherUsersSession() {
        when(currentUserProvider.hasAuthority("view:ai:log")).thenReturn(false);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(10L);
        when(aiSessionRepository.findById(7L)).thenReturn(Optional.of(session(7L, 20L)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.pageSessionCallLogs(7L, Pageable.unpaged()));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(aiCallLogRepository, never()).findBySession_IdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void listRetrievalLogsRejectsOtherUsersCallLog() {
        AiCallLog callLog = new AiCallLog();
        callLog.setId(8L);
        callLog.setUserId(20L);
        callLog.setSession(session(3L, 20L));
        when(currentUserProvider.hasAuthority("view:ai:log")).thenReturn(false);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(10L);
        when(aiCallLogRepository.findById(8L)).thenReturn(Optional.of(callLog));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.listRetrievalLogs(8L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(aiRetrievalLogRepository, never()).findByCallLog_IdOrderByRankNoAsc(any());
    }

    @Test
    void listMessageFeedbacksRejectsOtherUsersMessage() {
        AiMessage message = new AiMessage();
        message.setId(9L);
        message.setSenderUserId(20L);
        message.setSession(session(4L, 20L));
        when(currentUserProvider.hasAuthority("view:ai:log")).thenReturn(false);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(10L);
        when(aiMessageRepository.findById(9L)).thenReturn(Optional.of(message));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.listMessageFeedbacks(9L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(aiFeedbackLogRepository, never()).findByMessage_IdOrderByCreatedAtDesc(any());
    }

    @Test
    void saveFeedbackRejectsOtherUsersCallLogAssociation() {
        AiCallLog callLog = new AiCallLog();
        callLog.setId(10L);
        callLog.setUserId(20L);
        callLog.setSession(session(5L, 20L));
        AiFeedbackCreateRequest request = new AiFeedbackCreateRequest();
        request.setUserId(99L);
        request.setCallLogId(10L);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(10L);
        when(currentUserProvider.hasAuthority("view:ai:log")).thenReturn(false);
        when(aiCallLogRepository.findById(10L)).thenReturn(Optional.of(callLog));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.saveFeedback(request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(aiFeedbackLogRepository, never()).save(any());
    }

    @Test
    void saveFeedbackUsesCurrentUserId() {
        AiFeedbackCreateRequest request = new AiFeedbackCreateRequest();
        request.setUserId(99L);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(10L);
        when(aiFeedbackLogRepository.save(any(AiFeedbackLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiFeedbackLog saved = service.saveFeedback(request);

        assertEquals(10L, saved.getUserId());
    }

    private AiSession session(Long id, Long userId) {
        AiSession session = new AiSession();
        session.setId(id);
        session.setUserId(userId);
        return session;
    }
}
