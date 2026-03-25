package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.dto.AiRetrievalLogDTO;
import com.alikeyou.itmoduleai.service.AiChatService;
import com.alikeyou.itmoduleai.service.AiConversationService;
import com.alikeyou.itmoduleai.service.AiLogService;
import com.alikeyou.itmoduleai.dto.AiCallLogDTO;
import com.alikeyou.itmoduleinteractive.entity.Conversation;
import com.alikeyou.itmoduleinteractive.entity.Message;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "AI 功能", description = "通用 AI 服务接口")
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiChatService aiChatService;

    @Autowired
    private AiConversationService aiConversationService;

    @Autowired
    private AiLogService aiLogService;

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

    // ==================== 知识库管理接口 ====================

    @PostMapping("/knowledge-base")
    @Operation(summary = "创建知识库", description = "创建新的知识库")
    public ResponseEntity<?> createKnowledgeBase(@RequestBody Map<String, Object> request) {
        try {
            // TODO: 实现知识库创建逻辑
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "知识库创建成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "创建失败：" + e.getMessage()
            ));
        }
    }

    @GetMapping("/knowledge-base/{id}")
    @Operation(summary = "获取知识库详情", description = "根据 ID 获取知识库详细信息")
    public ResponseEntity<?> getKnowledgeBase(@PathVariable Long id) {
        try {
            // TODO: 实现获取知识库详情逻辑
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "获取成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取失败：" + e.getMessage()
            ));
        }
    }

    @GetMapping("/knowledge-base/user/{userId}")
    @Operation(summary = "获取用户的知识库列表", description = "获取指定用户的所有知识库")
    public ResponseEntity<?> getUserKnowledgeBases(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            // TODO: 实现获取用户知识库列表逻辑
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "获取成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取失败：" + e.getMessage()
            ));
        }
    }

    @PostMapping("/knowledge-base/{id}/documents")
    @Operation(summary = "上传文档到知识库", description = "上传文档到指定知识库")
    public ResponseEntity<?> uploadDocument(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            // TODO: 实现上传文档逻辑
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "文档上传成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "上传失败：" + e.getMessage()
            ));
        }
    }

    @GetMapping("/knowledge-base/{id}/documents")
    @Operation(summary = "获取知识库文档列表", description = "获取指定知识库的所有文档")
    public ResponseEntity<?> getKnowledgeDocuments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            // TODO: 实现获取文档列表逻辑
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "获取成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取失败：" + e.getMessage()
            ));
        }
    }
    // ==================== AI 使用统计接口 ====================

    @GetMapping("/usage/logs")
    @Operation(summary = "获取 AI 调用日志", description = "获取用户的 AI 调用历史记录")
    public ResponseEntity<?> getUsageLogs(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            var logs = aiLogService.getCallLogs(userId, PageRequest.of(page, size));

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", logs);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取失败：" + e.getMessage()
            ));
        }
    }

    // ==================== AI 会话管理接口 ====================

    @PostMapping("/conversation")
    @Operation(summary = "创建 AI 会话", description = "创建一个新的 AI 对话会话")
    public ResponseEntity<?> createConversation(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String aiType = (String) request.get("aiType");
            String aiModelName = (String) request.get("aiModelName");
            @SuppressWarnings("unchecked")
            Map<String, Object> configParams = (Map<String, Object>) request.get("configParams");

            if (userId == null || aiType == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "用户 ID 和 AI 类型不能为空"
                ));
            }

            var conversation = aiConversationService.createAiConversation(userId, aiType, aiModelName, configParams);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", conversation);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "创建失败：" + e.getMessage()
            ));
        }
    }

    @GetMapping("/conversation/{id}")
    @Operation(summary = "获取会话详情", description = "根据 ID 获取 AI 会话详细信息")
    public ResponseEntity<?> getConversation(@PathVariable Long id) {
        try {
            var conversation = aiConversationService.getConversation(id);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", conversation);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取失败：" + e.getMessage()
            ));
        }
    }

    @GetMapping("/conversation/{id}/messages")
    @Operation(summary = "获取会话消息列表", description = "获取指定会话的所有消息")
    public ResponseEntity<?> getConversationMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<Message> messages = aiConversationService.getConversationMessages(id, page, size);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", messages);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取失败：" + e.getMessage()
            ));
        }
    }

    @PostMapping("/conversation/{id}/message")
    @Operation(summary = "发送 AI 消息", description = "向 AI 会话发送消息并获取回复")
    public ResponseEntity<?> sendAiMessage(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String content = request.get("content");

            if (userId == null || content == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "用户 ID 和内容不能为空"
                ));
            }

            Message response = aiConversationService.sendAiMessage(id, userId, content);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "发送失败：" + e.getMessage()
            ));
        }
    }

    @GetMapping("/user/conversations")
    @Operation(summary = "获取用户的 AI 会话列表", description = "获取当前用户的所有 AI 会话")
    public ResponseEntity<?> getUserConversations(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<Conversation> conversations = aiConversationService.getUserAiConversations(userId, page, size);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", conversations);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取失败：" + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/conversation/{id}")
    @Operation(summary = "清空会话", description = "清空 AI 会话的上下文")
    public ResponseEntity<?> clearConversation(@PathVariable Long id) {
        try {
            aiConversationService.clearConversation(id);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "会话已清空");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "清空失败：" + e.getMessage()
            ));
        }
    }

    // ==================== AI 日志接口 ====================

    @GetMapping("/logs/retrieval/{callLogId}")
    @Operation(summary = "获取 AI 检索日志", description = "获取指定调用日志的检索详情")
    public ResponseEntity<?> getRetrievalLogs(@PathVariable Long callLogId) {
        try {
            List<AiRetrievalLogDTO> logs = aiLogService.getRetrievalLogs(callLogId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", logs);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取失败：" + e.getMessage()
            ));
        }
    }

    @GetMapping("/stats/usage")
    @Operation(summary = "获取 AI 使用统计", description = "获取用户的 AI 使用情况统计")
    public ResponseEntity<?> getUsageStats(
            @RequestParam Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Instant start = startDate != null ? Instant.parse(startDate) : Instant.now().minusSeconds(7 * 24 * 3600);
            Instant end = endDate != null ? Instant.parse(endDate) : Instant.now();

            AiLogService.AiUsageStats stats = aiLogService.getUserStats(userId, start, end);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", stats);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取失败：" + e.getMessage()
            ));
        }
    }
}
