package com.alikeyou.itmodulecommon.controller;

import com.alikeyou.itmodulecommon.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI 功能", description = "通用 AI 服务接口")
public class AiController {

    @Autowired
    private AiChatService aiChatService;

    @PostMapping("/chat")
    @Operation(summary = "AI 对话", description = "与 AI 进行基础对话")
    public ResponseEntity<?> chat(
            @RequestBody Map<String, String> request) {
        try {
            String message = request.get("message");
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "消息内容不能为空"
                ));
            }

            String response = aiChatService.chat(message);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "AI 服务异常：" + e.getMessage()
            ));
        }
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI 对话（流式输出）", description = "与 AI 进行基础对话，支持流式输出")
    public Flux<ServerSentEvent<String>> chatStream(
            @RequestParam String message) {
        if (message == null || message.trim().isEmpty()) {
            return Flux.just(ServerSentEvent.builder("消息内容不能为空").build());
        }

        return aiChatService.chatStream(message)
                .map(data -> ServerSentEvent.builder(data).build());
    }

    @PostMapping("/explain-translate")
    @Operation(summary = "解释或翻译", description = "对用户选中的内容进行解释或翻译")
    public ResponseEntity<?> explainOrTranslate(
            @RequestBody Map<String, String> request) {
        try {
            String content = request.get("content");
            String action = request.get("action"); // "explain" 或 "translate"

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "内容不能为空"
                ));
            }

            String response = aiChatService.explainOrTranslate(content, action);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("action", action);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "处理失败：" + e.getMessage()
            ));
        }
    }

    @PostMapping("/explain-code")
    @Operation(summary = "代码解读", description = "对选中的代码进行内容与作用的分析")
    public ResponseEntity<?> explainCode(
            @RequestBody Map<String, String> request) {
        try {
            String code = request.get("code");
            String language = request.get("language"); // 如：java, python, javascript

            if (code == null || code.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "代码不能为空"
                ));
            }

            String response = aiChatService.explainCode(code, language);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "代码解读失败：" + e.getMessage()
            ));
        }
    }

    @PostMapping("/chat-with-context")
    @Operation(summary = "AI 交流（带上下文）", description = "与 AI 进行交流，支持多轮对话")
    public ResponseEntity<?> chatWithContext(
            @RequestBody Map<String, String> request,
            @RequestParam(required = false) String userId) {
        try {
            String message = request.get("message");
            String context = request.get("context");

            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "消息内容不能为空"
                ));
            }

            String response = aiChatService.chatWithContext(message, context);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "AI 交流失败：" + e.getMessage()
            ));
        }
    }

    @GetMapping(value = "/chat-with-context/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI 交流（带上下文流式输出）", description = "与 AI 进行交流，支持多轮对话和流式输出")
    public Flux<ServerSentEvent<String>> chatWithContextStream(
            @RequestParam String message,
            @RequestParam(required = false) String context,
            @RequestParam(required = false) String userId) {
        if (message == null || message.trim().isEmpty()) {
            return Flux.just(ServerSentEvent.builder("消息内容不能为空").build());
        }

        return aiChatService.chatWithContextStream(message, context)
                .map(data -> ServerSentEvent.builder(data).build());
    }

    @GetMapping(value = "/explain-code/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "代码解读（流式输出）", description = "对选中的代码进行内容与作用的分析，支持流式输出")
    public Flux<ServerSentEvent<String>> explainCodeStream(
            @RequestParam String code,
            @RequestParam(required = false) String language) {
        if (code == null || code.trim().isEmpty()) {
            return Flux.just(ServerSentEvent.builder("代码不能为空").build());
        }

        return aiChatService.explainCodeStream(code, language)
                .map(data -> ServerSentEvent.builder(data).build());
    }

    @GetMapping(value = "/explain-translate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "解释或翻译（流式输出）", description = "对用户选中的内容进行解释或翻译，支持流式输出")
    public Flux<ServerSentEvent<String>> explainOrTranslateStream(
            @RequestParam String content,
            @RequestParam String action) {
        if (content == null || content.trim().isEmpty()) {
            return Flux.just(ServerSentEvent.builder("内容不能为空").build());
        }

        return aiChatService.explainOrTranslateStream(content, action)
                .map(data -> ServerSentEvent.builder(data).build());
    }
}
