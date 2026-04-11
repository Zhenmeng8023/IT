package com.alikeyou.itmoduleinteractive.realtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class UserRealtimeEmitterRegistry {

    private static final long EMITTER_TIMEOUT_MS = 30L * 60L * 1000L;

    private final Map<Long, Map<String, SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    public SseEmitter connect(Long userId, String clientId) {
        String resolvedClientId = normalizeClientId(clientId);
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        Map<String, SseEmitter> userEmitters = emittersByUser.computeIfAbsent(userId, key -> new ConcurrentHashMap<>());
        SseEmitter previous = userEmitters.put(resolvedClientId, emitter);
        if (previous != null) {
            previous.complete();
        }

        emitter.onCompletion(() -> remove(userId, resolvedClientId));
        emitter.onTimeout(() -> remove(userId, resolvedClientId));
        emitter.onError(ex -> remove(userId, resolvedClientId));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("clientId", resolvedClientId);
        payload.put("userId", userId);
        payload.put("connectedAt", Instant.now());
        send(emitter, "connected", payload);
        return emitter;
    }

    public void pushToUser(Long userId, String eventName, Object payload) {
        if (userId == null) {
            return;
        }
        Map<String, SseEmitter> userEmitters = emittersByUser.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }

        userEmitters.forEach((clientId, emitter) -> {
            try {
                send(emitter, eventName, payload);
            } catch (RuntimeException ex) {
                log.debug("推送 SSE 事件失败, userId={}, clientId={}, event={}", userId, clientId, eventName, ex);
                remove(userId, clientId);
            }
        });
    }

    public void pushToUsers(Collection<Long> userIds, String eventName, Object payload) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        Set<Long> uniqueUserIds = ConcurrentHashMap.newKeySet();
        uniqueUserIds.addAll(userIds);
        uniqueUserIds.forEach(userId -> pushToUser(userId, eventName, payload));
    }

    private void send(SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(payload, MediaType.APPLICATION_JSON));
        } catch (IOException ex) {
            throw new IllegalStateException("发送 SSE 事件失败", ex);
        }
    }

    private void remove(Long userId, String clientId) {
        Map<String, SseEmitter> userEmitters = emittersByUser.get(userId);
        if (userEmitters == null) {
            return;
        }
        userEmitters.remove(clientId);
        if (userEmitters.isEmpty()) {
            emittersByUser.remove(userId);
        }
    }

    private String normalizeClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return clientId.trim();
    }
}
