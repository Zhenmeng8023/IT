package com.alikeyou.itmoduleinteractive.controller;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.utils.UserUtil;
import com.alikeyou.itmoduleinteractive.dto.ConversationDTO;
import com.alikeyou.itmoduleinteractive.service.ConversationService;
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
@RequestMapping("/api/conversations")
@Tag(name = "会话管理", description = "私信和群聊会话相关接口（通用）")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @PostMapping
    @Operation(summary = "创建会话", description = "创建新的私信或群聊会话")
    public ResponseEntity<?> createConversation(
            @RequestBody ConversationDTO dto,
            Authentication authentication) {
        try {
            UserInfo currentUser = UserUtil.getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            ConversationDTO created = conversationService.createConversation(dto, currentUser.getId());
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("创建会话失败：" + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取会话详情", description = "根据 ID 获取会话详细信息")
    public ResponseEntity<?> getConversation(
            @Parameter(description = "会话 ID") @PathVariable Long id,
            Authentication authentication) {
        try {
            UserInfo currentUser = UserUtil.getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            return conversationService.getConversationById(id, currentUser.getId())
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取会话失败：" + e.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "获取我的会话列表", description = "获取当前用户的所有会话")
    public ResponseEntity<?> getUserConversations(Authentication authentication) {
        try {
            UserInfo currentUser = UserUtil.getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            List<ConversationDTO> conversations = conversationService.getUserConversations(currentUser.getId());
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取会话列表失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除会话", description = "删除指定会话及其所有消息")
    public ResponseEntity<?> deleteConversation(
            @Parameter(description = "会话 ID") @PathVariable Long id,
            Authentication authentication) {
        try {
            UserInfo currentUser = UserUtil.getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            conversationService.deleteConversation(id, currentUser.getId());
            Map<String, String> response = new HashMap<>();
            response.put("message", "会话删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("删除会话失败：" + e.getMessage());
        }
    }
}
