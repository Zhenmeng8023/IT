package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.application.orchestrator.AiChatOrchestrator;
import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import com.alikeyou.itmoduleai.dto.request.AiSessionCreateRequest;
import com.alikeyou.itmoduleai.dto.response.AiChatTurnResponse;
import com.alikeyou.itmoduleai.entity.AiSession;
import com.alikeyou.itmoduleai.service.AiSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AiChatControllerHttpTest {

    @Mock
    private AiChatOrchestrator aiChatOrchestrator;
    @Mock
    private AiSessionService aiSessionService;
    @Mock
    private AiCurrentUserProvider currentUserProvider;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        AiChatController controller = new AiChatController(aiChatOrchestrator, aiSessionService, currentUserProvider);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void chatTurnCreatesSessionAndReturnsAssistantResponse() throws Exception {
        when(currentUserProvider.requireCurrentUserId()).thenReturn(7L);
        AiSession session = new AiSession();
        session.setId(101L);
        when(aiSessionService.createSession(any(AiSessionCreateRequest.class))).thenReturn(session);
        when(aiChatOrchestrator.chat(any())).thenReturn(AiChatTurnResponse.builder()
                .sessionId(101L)
                .assistantMessageId(202L)
                .content("answer")
                .build());

        mockMvc.perform(post("/api/ai/chat/turn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "how does retrieval work?",
                                  "projectId": 9,
                                  "knowledgeBaseIds": [31],
                                  "traceDepth": 9,
                                  "requestParams": {
                                    "action": " locate ",
                                    "context": {" file ": "Demo.java", "ignored": null}
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("sent"))
                .andExpect(jsonPath("$.data.sessionId").value(101))
                .andExpect(jsonPath("$.data.assistantMessageId").value(202))
                .andExpect(jsonPath("$.data.content").value("answer"));

        ArgumentCaptor<com.alikeyou.itmoduleai.dto.request.AiChatSendRequest> requestCaptor =
                ArgumentCaptor.forClass(com.alikeyou.itmoduleai.dto.request.AiChatSendRequest.class);
        verify(aiChatOrchestrator).chat(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getSessionId()).isEqualTo(101L);
        assertThat(requestCaptor.getValue().getTraceDepth()).isEqualTo(3);
        assertThat(requestCaptor.getValue().getActionCode()).isEqualTo("code.locate");
    }

    @Test
    void chatTurnRejectsBlankContent() throws Exception {
        when(currentUserProvider.requireCurrentUserId()).thenReturn(7L);

        mockMvc.perform(post("/api/ai/chat/turn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("content", "  "))))
                .andExpect(status().isBadRequest());

        verify(aiSessionService, never()).createSession(any());
        verify(aiChatOrchestrator, never()).chat(any());
    }

    @Test
    void chatTurnRejectsAnonymousUser() throws Exception {
        when(currentUserProvider.requireCurrentUserId())
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated"));

        mockMvc.perform(post("/api/ai/chat/turn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("content", "hello"))))
                .andExpect(status().isUnauthorized());

        verify(aiSessionService, never()).createSession(any());
        verify(aiChatOrchestrator, never()).chat(any());
    }
}
