package com.alikeyou.itmodulecircle.controller;

import com.alikeyou.itmodulecircle.dto.ConversationResponse;
import com.alikeyou.itmodulecircle.dto.MessageRequest;
import com.alikeyou.itmodulecircle.dto.MessageResponse;
import com.alikeyou.itmodulecircle.entity.Conversation;
import com.alikeyou.itmodulecircle.entity.Message;
import com.alikeyou.itmodulecircle.dto.CircleCreatorInfo;
import com.alikeyou.itmodulecircle.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversation")
@Tag(name = "会话管理", description = "会话和消息的相关接口")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @PostMapping("")
    @Operation(summary = "创建新会话")
    public ResponseEntity<?> createConversation(@RequestBody Conversation conversation) {
        try {
            if (conversation.getType() == null || conversation.getType().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "会话类型不能为空");
                return ResponseEntity.badRequest().body(error);
            }

            if (conversation.getCreatorId() == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "创建者 ID 不能为空");
                return ResponseEntity.badRequest().body(error);
            }

            Conversation result = conversationService.createConversation(conversation);

            Map<String, Object> response = new HashMap<>();
            response.put("id", result.getId());
            response.put("type", result.getType());
            response.put("name", result.getName());
            response.put("creatorId", result.getCreatorId());
            response.put("createdAt", result.getCreatedAt());
            response.put("updatedAt", result.getUpdatedAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    @GetMapping("/{id}")
    @Operation(summary = "获取会话详情")
    public ResponseEntity<?> getConversationDetail(@PathVariable Long id,
                                                   @RequestParam Long userId) {
        try {
            ConversationResponse response = conversationService.getConversationDetail(id, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("")
    @Operation(summary = "获取所有会话列表")
    public ResponseEntity<?> getAllConversations() {
        try {
            List<ConversationResponse> conversations = conversationService.getAllConversations();
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除会话")
    public ResponseEntity<?> deleteConversation(@PathVariable Long id) {
        try {
            conversationService.deleteConversation(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/{conversationId}/messages")
    @Operation(summary = "发送消息")
    public ResponseEntity<?> sendMessage(@PathVariable Long conversationId,
                                         @RequestParam Long senderId,
                                         @RequestBody MessageRequest request) {
        try {
            if (request.getContent() == null || request.getContent().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "消息内容不能为空");
                return ResponseEntity.badRequest().body(error);
            }

            Message message = conversationService.sendMessage(conversationId, senderId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("id", message.getId());
            response.put("conversationId", message.getConversation().getId());
            response.put("senderId", message.getSenderId());
            response.put("content", message.getContent());
            response.put("messageType", message.getMessageType());
            response.put("sentAt", message.getSentAt());
            response.put("isRead", message.getIsRead());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{conversationId}/messages")
    @Operation(summary = "获取会话消息列表")
    public ResponseEntity<?> getConversationMessages(@PathVariable Long conversationId) {
        try {
            List<MessageResponse> messages = conversationService.getConversationMessages(conversationId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{conversationId}/messages/since")
    @Operation(summary = "获取指定时间之后的消息")
    public ResponseEntity<?> getMessagesSince(@PathVariable Long conversationId,
                                              @RequestParam Long timestamp) {
        try {
            java.time.Instant since = java.time.Instant.ofEpochMilli(timestamp);
            List<MessageResponse> messages = conversationService.getConversationMessagesAfter(conversationId, since);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/messages/{messageId}/read")
    @Operation(summary = "标记消息为已读")
    public ResponseEntity<?> markMessageAsRead(@PathVariable Long messageId) {
        try {
            conversationService.markMessageAsRead(messageId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{conversationId}/read-all")
    @Operation(summary = "标记会话所有消息为已读")
    public ResponseEntity<?> markAllAsRead(@PathVariable Long conversationId) {
        try {
            conversationService.markAllMessagesAsRead(conversationId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{conversationId}/unread-count")
    @Operation(summary = "获取未读消息数量")
    public ResponseEntity<?> getUnreadCount(@PathVariable Long conversationId) {
        try {
            Long count = conversationService.getUnreadMessageCount(conversationId);
            Map<String, Long> result = new HashMap<>();
            result.put("unreadCount", count);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
