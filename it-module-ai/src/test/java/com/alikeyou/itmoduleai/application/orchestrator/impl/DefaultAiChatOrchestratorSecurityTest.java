package com.alikeyou.itmoduleai.application.orchestrator.impl;

import com.alikeyou.itmoduleai.application.support.AiContextMessageBuilder;
import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import com.alikeyou.itmoduleai.application.support.AiKnowledgeResolver;
import com.alikeyou.itmoduleai.application.support.AiModelSelector;
import com.alikeyou.itmoduleai.application.support.AiPromptResolver;
import com.alikeyou.itmoduleai.application.support.AiScenePostProcessor;
import com.alikeyou.itmoduleai.dto.request.AiChatSendRequest;
import com.alikeyou.itmoduleai.entity.AiSession;
import com.alikeyou.itmoduleai.provider.AiProviderManager;
import com.alikeyou.itmoduleai.repository.AiCallLogRepository;
import com.alikeyou.itmoduleai.repository.AiMessageRepository;
import com.alikeyou.itmoduleai.repository.AiSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultAiChatOrchestratorSecurityTest {

    private AiSessionRepository aiSessionRepository;
    private AiProviderManager aiProviderManager;
    private PlatformTransactionManager transactionManager;
    private DefaultAiChatOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        aiSessionRepository = mock(AiSessionRepository.class);
        aiProviderManager = mock(AiProviderManager.class);
        transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        AiCurrentUserProvider currentUserProvider = mock(AiCurrentUserProvider.class);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(100L);

        orchestrator = new DefaultAiChatOrchestrator(
                aiSessionRepository,
                mock(AiMessageRepository.class),
                mock(AiCallLogRepository.class),
                aiProviderManager,
                mock(AiModelSelector.class),
                mock(AiPromptResolver.class),
                mock(AiContextMessageBuilder.class),
                mock(AiKnowledgeResolver.class),
                new ObjectMapper(),
                transactionManager,
                mock(AiScenePostProcessor.class),
                currentUserProvider
        );
    }

    @Test
    void nonStreamingChatRejectsOtherUsersSessionBeforeProviderCall() {
        when(aiSessionRepository.findById(10L)).thenReturn(Optional.of(session(10L, 200L)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orchestrator.chat(request(10L)));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(aiProviderManager, never()).resolve(any());
    }

    @Test
    void streamingChatUsesSameOwnerCheckBeforeProviderCall() {
        when(aiSessionRepository.findById(10L)).thenReturn(Optional.of(session(10L, 200L)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orchestrator.stream(request(10L)));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(aiProviderManager, never()).resolve(any());
    }

    private AiChatSendRequest request(Long sessionId) {
        AiChatSendRequest request = new AiChatSendRequest();
        request.setSessionId(sessionId);
        request.setContent("hello");
        return request;
    }

    private AiSession session(Long id, Long userId) {
        AiSession session = new AiSession();
        session.setId(id);
        session.setUserId(userId);
        session.setBizType(AiSession.BizType.GENERAL);
        session.setSceneCode("general.chat");
        session.setMemoryMode(AiSession.MemoryMode.SHORT);
        session.setStatus(AiSession.Status.ACTIVE);
        return session;
    }
}
