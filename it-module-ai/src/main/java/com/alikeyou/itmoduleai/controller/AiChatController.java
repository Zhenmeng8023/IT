package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.application.orchestrator.AiChatOrchestrator;
import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.dto.request.AiChatSendRequest;
import com.alikeyou.itmoduleai.dto.response.AiChatStreamChunkResponse;
import com.alikeyou.itmoduleai.dto.response.AiChatTurnResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatOrchestrator aiChatOrchestrator;

    @PostMapping("/turn")
    public ApiResponse<AiChatTurnResponse> chat(@RequestBody AiChatSendRequest request) {
        return ApiResponse.ok("发送成功", aiChatOrchestrator.chat(request));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AiChatStreamChunkResponse> stream(@RequestBody AiChatSendRequest request) {
        return aiChatOrchestrator.stream(request);
    }
}
