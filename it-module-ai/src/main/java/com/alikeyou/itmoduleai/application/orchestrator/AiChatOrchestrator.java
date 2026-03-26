package com.alikeyou.itmoduleai.application.orchestrator;

import com.alikeyou.itmoduleai.dto.request.AiChatSendRequest;
import com.alikeyou.itmoduleai.dto.response.AiChatStreamChunkResponse;
import com.alikeyou.itmoduleai.dto.response.AiChatTurnResponse;
import reactor.core.publisher.Flux;

public interface AiChatOrchestrator {

    AiChatTurnResponse chat(AiChatSendRequest request);

    Flux<AiChatStreamChunkResponse> stream(AiChatSendRequest request);
}
