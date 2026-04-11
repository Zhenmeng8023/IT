package com.alikeyou.itmoduleinteractive.controller;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.utils.UserUtil;
import com.alikeyou.itmoduleinteractive.realtime.UserRealtimeEmitterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/realtime")
@Tag(name = "实时连接", description = "多客户端实时事件推送接口")
public class RealtimeController {

    private final UserRealtimeEmitterRegistry emitterRegistry;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "建立实时连接", description = "为当前登录用户建立 SSE 长连接，支持多个客户端同时在线")
    public SseEmitter stream(@RequestParam(required = false) String clientId) {
        UserInfo currentUser = UserUtil.getCurrentUser(null);
        if (currentUser == null || currentUser.getId() == null) {
            throw new IllegalStateException("用户未登录");
        }
        return emitterRegistry.connect(currentUser.getId(), clientId);
    }
}
