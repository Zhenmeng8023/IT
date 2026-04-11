package com.alikeyou.itmoduleinteractive.controller;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.utils.UserUtil;
import com.alikeyou.itmoduleinteractive.dto.MessageDTO;
import com.alikeyou.itmoduleinteractive.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@Tag(name = "消息管理", description = "聊天消息相关接口（通用）")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @PostMapping
    @Operation(summary = "发送消息", description = "在指定会话中发送一条消息（通用接口，适用于私信、群聊等）")
    public ResponseEntity<?> sendMessage(
            @RequestBody MessageDTO dto,
            Authentication authentication) {
        try {
            UserInfo currentUser = UserUtil.getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            if (dto.getConversationId() == null || dto.getContent() == null) {
                return ResponseEntity.badRequest().body("会话 ID 和内容不能为空");
            }

            MessageDTO sentMessage = messageService.sendMessage(dto, currentUser.getId());
            return ResponseEntity.ok(sentMessage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("发送消息失败：" + e.getMessage());
        }
    }

    @GetMapping("/conversation/{conversationId}")
    @Operation(summary = "获取会话消息", description = "获取指定会话的所有消息")
    public ResponseEntity<?> getConversationMessages(
            @Parameter(description = "会话 ID") @PathVariable Long conversationId,
            Authentication authentication) {
        try {
            UserInfo currentUser = UserUtil.getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            List<MessageDTO> messages = messageService.getConversationMessages(conversationId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取消息失败：" + e.getMessage());
        }
    }

    @GetMapping("/conversation/{conversationId}/recent")
    @Operation(summary = "获取最近消息", description = "获取指定会话的最近 N 条消息")
    public ResponseEntity<?> getRecentMessages(
            @Parameter(description = "会话 ID") @PathVariable Long conversationId,
            @Parameter(description = "消息数量，默认 20")
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {
        try {
            UserInfo currentUser = UserUtil.getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            List<MessageDTO> messages = messageService.getRecentMessages(conversationId, limit);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取最近消息失败：" + e.getMessage());
        }
    }

    @PutMapping("/conversation/{conversationId}/read")
    @Operation(summary = "标记消息为已读", description = "将会话中的所有消息标记为已读")
    public ResponseEntity<?> markMessagesAsRead(
            @Parameter(description = "会话 ID") @PathVariable Long conversationId,
            Authentication authentication) {
        try {
            UserInfo currentUser = UserUtil.getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            messageService.markMessagesAsRead(conversationId, currentUser.getId());
            Map<String, String> response = new HashMap<>();
            response.put("message", "消息已标记为已读");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("标记已读失败：" + e.getMessage());
        }
    }

    @GetMapping("/conversation/{conversationId}/unread-count")
    @Operation(summary = "获取未读消息数", description = "获取指定会话的未读消息数量")
    public ResponseEntity<?> getUnreadCount(
            @Parameter(description = "会话 ID") @PathVariable Long conversationId,
            Authentication authentication) {
        try {
            UserInfo currentUser = UserUtil.getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            long count = messageService.getUnreadCount(conversationId);
            Map<String, Long> response = new HashMap<>();
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取未读数失败：" + e.getMessage());
        }
    }
}
