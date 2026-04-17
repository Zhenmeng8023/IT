package com.alikeyou.itmoduleai.application.support;

import com.alikeyou.itmoduleai.enums.AiAnalysisMode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Component
public class CodeQueryIntentClassifier {

    private static final AiSceneActionCatalog SCENE_ACTION_CATALOG = new AiSceneActionCatalog();

    public AiAnalysisMode classify(AiAnalysisMode requestedMode, String question, String symbolHint) {
        return classify(requestedMode, null, question, symbolHint);
    }

    public AiAnalysisMode classify(AiAnalysisMode requestedMode,
                                   String actionCode,
                                   String question,
                                   String symbolHint) {
        AiAnalysisMode normalizedRequested = normalizeRequestedMode(requestedMode);
        if (normalizedRequested == AiAnalysisMode.DOC_QA
                || normalizedRequested == AiAnalysisMode.CODE_LOCATE
                || normalizedRequested == AiAnalysisMode.CODE_LOGIC) {
            return normalizedRequested;
        }

        AiAnalysisMode actionMode = SCENE_ACTION_CATALOG.resolveAnalysisModeFromAction(actionCode);
        if (actionMode != null) {
            return actionMode;
        }

        String text = normalize((question == null ? "" : question) + " " + (symbolHint == null ? "" : symbolHint));
        if (!StringUtils.hasText(text)) {
            return AiAnalysisMode.DOC_QA;
        }

        if (containsAny(text,
                "\u5165\u53e3", "\u5b9a\u4e49", "\u6587\u4ef6", "\u5728\u54ea", "\u4f4d\u7f6e",
                "where", "which file", "entry", "definition", "locate", "find file")) {
            return AiAnalysisMode.CODE_LOCATE;
        }

        if (containsAny(text,
                "\u6d41\u7a0b", "\u4e3a\u4ec0\u4e48", "\u8c03\u7528\u94fe", "\u903b\u8f91", "\u5b9e\u73b0",
                "flow", "why", "call chain", "call graph", "implementation", "how does")) {
            return AiAnalysisMode.CODE_LOGIC;
        }

        if (containsAny(text,
                "\u603b\u7ed3", "\u6458\u8981", "\u6da6\u8272", "\u6807\u7b7e", "\u89e3\u91ca\u9875\u9762", "\u77e5\u8bc6\u5e93\u95ee\u7b54",
                "summary", "summarize", "polish", "tags", "explain page", "knowledge base")) {
            return AiAnalysisMode.DOC_QA;
        }

        return AiAnalysisMode.DOC_QA;
    }

    private AiAnalysisMode normalizeRequestedMode(AiAnalysisMode mode) {
        if (mode == null) {
            return null;
        }
        return switch (mode) {
            case GENERAL -> null;
            case CODE_ANALYSIS -> AiAnalysisMode.CODE_LOGIC;
            default -> mode;
        };
    }

    private boolean containsAny(String source, String... needles) {
        for (String needle : needles) {
            if (source.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
