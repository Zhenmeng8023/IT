package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.dto.request.AiFeedbackCreateRequest;
import com.alikeyou.itmoduleai.dto.response.AiCallLogDebugResponse;
import com.alikeyou.itmoduleai.dto.response.AiRetrievalLogDebugResponse;
import com.alikeyou.itmoduleai.dto.response.EmbeddingProfileView;
import com.alikeyou.itmoduleai.entity.AiCallLog;
import com.alikeyou.itmoduleai.entity.AiFeedbackLog;
import com.alikeyou.itmoduleai.entity.AiRetrievalLog;
import com.alikeyou.itmoduleai.service.AiLogService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/logs")
@RequiredArgsConstructor
public class AiLogController {

    private final AiLogService aiLogService;
    private final ObjectMapper objectMapper;

    @PostMapping("/feedback")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<AiFeedbackLog> saveFeedback(@RequestBody AiFeedbackCreateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求体不能为空");
        }
        request.setUserId(resolveCurrentUserId());
        return ApiResponse.ok("反馈成功", aiLogService.saveFeedback(request));
    }

    @GetMapping("/user/{userId}/calls")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Page<AiCallLog>> pageUserCalls(@PathVariable Long userId, Pageable pageable) {
        Long effectiveUserId = hasAuthority("view:ai:log") ? userId : resolveCurrentUserId();
        return ApiResponse.ok(aiLogService.pageUserCallLogs(effectiveUserId, pageable));
    }

    @GetMapping("/session/{sessionId}/calls")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Page<AiCallLog>> pageSessionCalls(@PathVariable Long sessionId, Pageable pageable) {
        return ApiResponse.ok(aiLogService.pageSessionCallLogs(sessionId, pageable));
    }

    @GetMapping("/call/{callLogId}/retrievals")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<AiRetrievalLog>> listRetrievals(@PathVariable Long callLogId) {
        return ApiResponse.ok(aiLogService.listRetrievalLogs(callLogId));
    }

    @GetMapping("/call/{callLogId}/debug")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ApiResponse<AiCallLogDebugResponse> debugCall(@PathVariable Long callLogId) {
        AiCallLog callLog = aiLogService.getCallLog(callLogId);
        List<AiRetrievalLogDebugResponse> retrievals = aiLogService.listRetrievalLogs(callLogId).stream()
                .map(this::toRetrievalDebugResponse)
                .toList();
        Map<String, Object> retrievalSummary = parseMap(callLog.getRetrievalSummaryJson());
        Map<String, Object> groundingReport = parseMap(callLog.getGroundingReportJson());
        Map<String, Object> metadata = parseMap(callLog.getMetadataJson());
        return ApiResponse.ok(buildCallDebugResponse(callLog, retrievalSummary, groundingReport, metadata, retrievals));
    }

    @GetMapping("/call/{callLogId}/retrievals/debug")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ApiResponse<List<AiRetrievalLogDebugResponse>> debugRetrievals(@PathVariable Long callLogId) {
        aiLogService.getCallLog(callLogId);
        return ApiResponse.ok(aiLogService.listRetrievalLogs(callLogId).stream()
                .map(this::toRetrievalDebugResponse)
                .toList());
    }

    @GetMapping("/message/{messageId}/feedbacks")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<AiFeedbackLog>> listMessageFeedbacks(@PathVariable Long messageId) {
        return ApiResponse.ok(aiLogService.listMessageFeedbacks(messageId));
    }

    @GetMapping("/user/{userId}/feedbacks")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<AiFeedbackLog>> listUserFeedbacks(@PathVariable Long userId) {
        Long effectiveUserId = hasAuthority("view:ai:log") ? userId : resolveCurrentUserId();
        return ApiResponse.ok(aiLogService.listUserFeedbacks(effectiveUserId));
    }

    private AiCallLogDebugResponse buildCallDebugResponse(AiCallLog callLog,
                                                          Map<String, Object> retrievalSummary,
                                                          Map<String, Object> groundingReport,
                                                          Map<String, Object> metadata,
                                                          List<AiRetrievalLogDebugResponse> retrievals) {
        return AiCallLogDebugResponse.builder()
                .id(callLog.getId())
                .userId(callLog.getUserId())
                .sessionId(callLog.getSessionId())
                .messageId(callLog.getMessageId())
                .aiModelId(callLog.getAiModelId())
                .aiModelName(callLog.getAiModelName())
                .aiModelProviderCode(callLog.getAiModelProviderCode())
                .requestType(callLog.getRequestType() == null ? null : callLog.getRequestType().name())
                .requestStage(callLog.getRequestStage() == null ? null : callLog.getRequestStage().name())
                .status(callLog.getStatus() == null ? null : callLog.getStatus().name())
                .requestText(callLog.getRequestText())
                .responseText(callLog.getResponseText())
                .requestParamsJson(callLog.getRequestParams())
                .retrievalSummaryJson(callLog.getRetrievalSummaryJson())
                .groundingReportJson(callLog.getGroundingReportJson())
                .metadataJson(callLog.getMetadataJson())
                .degradeReason(callLog.getDegradeReason())
                .promptTokens(callLog.getPromptTokens())
                .completionTokens(callLog.getCompletionTokens())
                .totalTokens(callLog.getTotalTokens())
                .costAmount(callLog.getCostAmount())
                .latencyMs(callLog.getLatencyMs())
                .errorCode(callLog.getErrorCode())
                .errorMessage(callLog.getErrorMessage())
                .createdAt(callLog.getCreatedAt())
                .requestParams(retrievalSummary == null ? parseMap(callLog.getRequestParams()) : parseMap(callLog.getRequestParams()))
                .retrievalSummary(retrievalSummary)
                .groundingReport(groundingReport)
                .metadata(metadata)
                .finalContextSource(firstText(
                        retrievalSummary == null ? null : retrievalSummary.get("finalContextSource"),
                        retrievalSummary == null ? null : retrievalSummary.get("contextSource"),
                        metadata == null ? null : metadata.get("finalContextSource")))
                .embeddingProfile(extractEmbeddingProfile(retrievalSummary, callLog))
                .retrievals(retrievals)
                .build();
    }

    private AiRetrievalLogDebugResponse toRetrievalDebugResponse(AiRetrievalLog log) {
        return AiRetrievalLogDebugResponse.builder()
                .id(log.getId())
                .callLogId(log.getCallLogId())
                .knowledgeBaseId(log.getKnowledgeBaseId())
                .knowledgeBaseName(log.getKnowledgeBaseName())
                .documentId(log.getDocumentId())
                .documentTitle(log.getDocumentTitle())
                .chunkId(log.getChunkId())
                .chunkEmbeddingProvider(log.getChunkEmbeddingProvider())
                .chunkEmbeddingModel(log.getChunkEmbeddingModel())
                .queryText(log.getQueryText())
                .retrievalMethod(log.getRetrievalMethod() == null ? null : log.getRetrievalMethod().name())
                .stageCode(log.getStageCode() == null ? null : log.getStageCode().name())
                .phase(log.getPhase())
                .stageOrder(log.getStageOrder())
                .candidateSource(log.getCandidateSource() == null ? null : log.getCandidateSource().name())
                .score(log.getScore())
                .scoreKeyword(log.getScoreKeyword())
                .scoreVector(log.getScoreVector())
                .scoreGraph(log.getScoreGraph())
                .scoreRerank(log.getScoreRerank())
                .rerankModel(log.getRerankModel())
                .groundingStatus(log.getGroundingStatus() == null ? null : log.getGroundingStatus().name())
                .groundingEvidenceJson(log.getGroundingEvidenceJson())
                .degradeReason(log.getDegradeReason())
                .hitReasonJson(log.getHitReasonJson())
                .scoreDetailJson(log.getScoreDetailJson())
                .rankNo(log.getRankNo())
                .metadataJson(log.getMetadataJson())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private EmbeddingProfileView extractEmbeddingProfile(Map<String, Object> retrievalSummary, AiCallLog callLog) {
        Map<String, Object> source = new LinkedHashMap<>();
        if (retrievalSummary != null) {
            Object nested = retrievalSummary.get("embeddingProfile");
            if (nested instanceof Map<?, ?> map) {
                map.forEach((key, value) -> source.put(String.valueOf(key), value));
            }
            source.putIfAbsent("provider", retrievalSummary.get("embeddingProvider"));
            source.putIfAbsent("modelName", retrievalSummary.get("embeddingModel"));
            source.putIfAbsent("dimension", retrievalSummary.get("embeddingDimension"));
            source.putIfAbsent("batchSize", retrievalSummary.get("batchSize"));
            source.putIfAbsent("source", retrievalSummary.get("embeddingSource"));
            source.putIfAbsent("warning", retrievalSummary.get("embeddingWarning"));
            source.putIfAbsent("providerSupported", retrievalSummary.get("embeddingProviderSupported"));
            source.putIfAbsent("needsRebuild", retrievalSummary.get("embeddingNeedsRebuild"));
            source.putIfAbsent("activeProvider", retrievalSummary.get("activeEmbeddingProvider"));
            source.putIfAbsent("activeModelName", retrievalSummary.get("activeEmbeddingModelName"));
            source.putIfAbsent("activeDimension", retrievalSummary.get("activeEmbeddingDimension"));
            source.putIfAbsent("activeEmbeddingCount", retrievalSummary.get("activeEmbeddingCount"));
        }
        if (source.isEmpty()) {
            return null;
        }
        return EmbeddingProfileView.builder()
                .requestedProvider(text(source.get("requestedProvider")))
                .requestedModelName(text(source.get("requestedModelName")))
                .requestedDimension(intValue(source.get("requestedDimension")))
                .configuredProvider(text(source.get("configuredProvider")))
                .configuredModelName(text(source.get("configuredModelName")))
                .provider(text(source.get("provider")))
                .modelName(text(source.get("modelName")))
                .dimension(intValue(source.get("dimension")))
                .batchSize(intValue(source.get("batchSize")))
                .source(text(source.get("source")))
                .providerSupported(boolValue(source.get("providerSupported")))
                .warning(text(source.get("warning")))
                .activeProvider(text(source.get("activeProvider")))
                .activeModelName(text(source.get("activeModelName")))
                .activeDimension(intValue(source.get("activeDimension")))
                .activeEmbeddingCount(longValue(source.get("activeEmbeddingCount")))
                .needsRebuild(boolValue(source.get("needsRebuild")))
                .build();
    }

    private Map<String, Object> parseMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
            return parsed == null ? Map.of() : parsed;
        } catch (Exception ex) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("raw", json);
            return fallback;
        }
    }

    private String firstText(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (StringUtils.hasText(text) && !"null".equalsIgnoreCase(text)) {
                return text;
            }
        }
        return null;
    }

    private String text(Object value) {
        return value == null ? null : firstText(value);
    }

    private Integer intValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Boolean boolValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value).trim());
    }

    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authority == null || authority.isBlank()) {
            return false;
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities == null) {
            return false;
        }
        for (GrantedAuthority grantedAuthority : authorities) {
            if (grantedAuthority != null && authority.equals(grantedAuthority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private Long resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录或登录状态已失效");
        }
        Long userId = extractUserId(authentication.getPrincipal());
        if (userId == null) {
            userId = extractUserId(authentication.getDetails());
        }
        if (userId == null) {
            userId = parseLong(authentication.getName());
        }
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "无法识别当前登录用户");
        }
        return userId;
    }

    private Long extractUserId(Object source) {
        if (source == null) {
            return null;
        }
        if (source instanceof Number number) {
            return number.longValue();
        }
        if (source instanceof CharSequence sequence) {
            return parseLong(sequence.toString());
        }
        if (source instanceof Map<?, ?> map) {
            for (String key : List.of("id", "userId", "uid")) {
                Object value = map.get(key);
                Long parsed = extractUserId(value);
                if (parsed != null) {
                    return parsed;
                }
            }
            return null;
        }
        for (String methodName : List.of("getId", "getUserId", "getUid")) {
            try {
                Method method = source.getClass().getMethod(methodName);
                Object value = method.invoke(source);
                Long parsed = extractUserId(value);
                if (parsed != null) {
                    return parsed;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Long parseLong(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
