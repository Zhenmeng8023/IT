package com.alikeyou.itmoduleai.dto.request;

import com.alikeyou.itmoduleai.enums.AiAnalysisMode;
import com.alikeyou.itmoduleai.enums.GroundingStatus;
import com.alikeyou.itmoduleai.entity.AiCallLog;
import com.alikeyou.itmoduleai.entity.AiSession;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
public class AiChatSendRequest {

    private Long sessionId;
    private Long userId;
    private String content;
    private Long modelId;
    private Long promptTemplateId;
    private AiCallLog.RequestType requestType;
    private List<Long> knowledgeBaseIds;
    private Map<String, Object> requestParams;

    private AiSession.BizType bizType;
    private Long bizId;
    private Long projectId;
    private String sceneCode;
    private String actionCode;
    private Map<String, Object> contextPayload;
    private String clientScene;
    private String preferredAnalysisMode;
    private String sessionTitle;
    private AiSession.MemoryMode memoryMode;
    private Long defaultKnowledgeBaseId;

    private AiAnalysisMode analysisMode;
    private Boolean strictGrounding;
    private String entryFile;
    private String symbolHint;
    private Integer traceDepth;
    private GroundingStatus groundingStatus;
    private Map<String, Object> retrievalSummary;

    public AiAnalysisMode getAnalysisMode() {
        if (analysisMode != null) {
            return analysisMode;
        }
        return parseEnum(firstParam("analysisMode", "analysis_mode"), AiAnalysisMode.class);
    }

    public Boolean getStrictGrounding() {
        if (strictGrounding != null) {
            return strictGrounding;
        }
        return parseBoolean(firstParam("strictGrounding", "strict_grounding"));
    }

    public String getEntryFile() {
        if (hasText(entryFile)) {
            return entryFile;
        }
        return parseString(firstParam("entryFile", "entry_file"));
    }

    public String getSymbolHint() {
        if (hasText(symbolHint)) {
            return symbolHint;
        }
        return parseString(firstParam("symbolHint", "symbol_hint"));
    }

    public Integer getTraceDepth() {
        if (traceDepth != null) {
            return traceDepth;
        }
        return parseInteger(firstParam("traceDepth", "trace_depth"));
    }

    public GroundingStatus getGroundingStatus() {
        if (groundingStatus != null) {
            return groundingStatus;
        }
        return parseEnum(firstParam("groundingStatus", "grounding_status"), GroundingStatus.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getRetrievalSummary() {
        if (retrievalSummary != null) {
            return retrievalSummary;
        }
        Object value = firstParam("retrievalSummary", "retrieval_summary");
        return value instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : null;
    }

    public String getActionCode() {
        if (hasText(actionCode)) {
            return actionCode;
        }
        return parseString(firstParam("actionCode", "action_code", "action"));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getContextPayload() {
        if (contextPayload != null) {
            return contextPayload;
        }
        Object value = firstParam("contextPayload", "context_payload", "context");
        return value instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : null;
    }

    public String getClientScene() {
        if (hasText(clientScene)) {
            return clientScene;
        }
        return parseString(firstParam("clientScene", "client_scene"));
    }

    public String getPreferredAnalysisMode() {
        if (hasText(preferredAnalysisMode)) {
            return preferredAnalysisMode;
        }
        return parseString(firstParam("preferredAnalysisMode", "preferred_analysis_mode"));
    }

    public AiAnalysisMode getPreferredAnalysisModeEnum() {
        return parseEnum(getPreferredAnalysisMode(), AiAnalysisMode.class);
    }

    private Object firstParam(String... keys) {
        if (requestParams == null || requestParams.isEmpty() || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key != null && requestParams.containsKey(key)) {
                Object value = requestParams.get(key);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String parseString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            String text = String.valueOf(value).trim();
            return text.isEmpty() ? null : Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Boolean parseBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean flag) {
            return flag;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        if (Objects.equals("1", text)) {
            return true;
        }
        if (Objects.equals("0", text)) {
            return false;
        }
        return Boolean.parseBoolean(text);
    }

    private static <E extends Enum<E>> E parseEnum(Object value, Class<E> enumType) {
        String text = parseString(value);
        if (text == null || enumType == null) {
            return null;
        }
        String normalized = text.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase();
        try {
            return Enum.valueOf(enumType, normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
