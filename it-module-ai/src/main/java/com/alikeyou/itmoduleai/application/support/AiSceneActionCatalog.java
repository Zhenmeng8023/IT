package com.alikeyou.itmoduleai.application.support;

import com.alikeyou.itmoduleai.entity.AiSession;
import com.alikeyou.itmoduleai.enums.AiAnalysisMode;
import com.alikeyou.itmoduleai.enums.AiStructuredApplyTarget;
import org.springframework.util.StringUtils;

import java.util.*;

public class AiSceneActionCatalog {

    private static final Set<String> KNOWN_SCENE_CODES = Set.of(
            "general.chat",
            "project.assistant",
            "blog.assistant",
            "circle.assistant",
            "paid-content.assistant",
            "project.detail.summary",
            "project.detail.tasks",
            "project.detail.risks",
            "project.detail.next-steps",
            "blog.polish",
            "blog.summary",
            "knowledge.base.explain",
            "page.explain"
    );

    private static final Set<String> STRUCTURED_CODES = Set.of(
            "project.detail.summary",
            "project.detail.tasks",
            "project.detail.risks",
            "project.detail.next-steps",
            "blog.polish",
            "blog.summary",
            "knowledge.base.explain",
            "page.explain"
    );

    private static final Map<String, String> SCENE_ALIASES = Map.ofEntries(
            Map.entry("project.detail.next.steps", "project.detail.next-steps"),
            Map.entry("project.detail.next_steps", "project.detail.next-steps"),
            Map.entry("knowledgebase.explain", "knowledge.base.explain"),
            Map.entry("knowledge.base.qa", "knowledge.base.explain"),
            Map.entry("page.detail.explain", "page.explain")
    );

    private static final Map<String, String> ACTION_ALIASES = Map.ofEntries(
            Map.entry("code_locate", "code.locate"),
            Map.entry("codelocate", "code.locate"),
            Map.entry("locate", "code.locate"),
            Map.entry("find.file", "code.locate"),
            Map.entry("which.file", "code.locate"),
            Map.entry("entry", "code.locate"),

            Map.entry("code_logic", "code.logic"),
            Map.entry("codelogic", "code.logic"),
            Map.entry("logic", "code.logic"),
            Map.entry("call.chain", "code.logic"),
            Map.entry("callchain", "code.logic"),
            Map.entry("implementation", "code.logic"),

            Map.entry("doc_qa", "doc.qa"),
            Map.entry("docqa", "doc.qa"),
            Map.entry("qa", "doc.qa"),
            Map.entry("knowledge.qa", "doc.qa"),

            Map.entry("project.detail.next.steps", "project.detail.next-steps"),
            Map.entry("project.detail.next_steps", "project.detail.next-steps")
    );

    private static final Map<String, AiAnalysisMode> ACTION_MODE_HINT = Map.of(
            "code.locate", AiAnalysisMode.CODE_LOCATE,
            "code.logic", AiAnalysisMode.CODE_LOGIC,
            "doc.qa", AiAnalysisMode.DOC_QA
    );

    private static final Map<String, List<AiStructuredApplyTarget>> APPLY_TARGETS = Map.ofEntries(
            Map.entry("project.detail.summary", List.of(
                    AiStructuredApplyTarget.DISPLAY_TEXT,
                    AiStructuredApplyTarget.PROJECT_DETAIL_SUMMARY
            )),
            Map.entry("project.detail.tasks", List.of(
                    AiStructuredApplyTarget.DISPLAY_TEXT,
                    AiStructuredApplyTarget.PROJECT_DETAIL_TASKS,
                    AiStructuredApplyTarget.PROJECT_DETAIL_RISKS
            )),
            Map.entry("project.detail.risks", List.of(
                    AiStructuredApplyTarget.DISPLAY_TEXT,
                    AiStructuredApplyTarget.PROJECT_DETAIL_RISKS
            )),
            Map.entry("project.detail.next-steps", List.of(
                    AiStructuredApplyTarget.DISPLAY_TEXT,
                    AiStructuredApplyTarget.PROJECT_DETAIL_NEXT_STEPS
            )),
            Map.entry("blog.polish", List.of(
                    AiStructuredApplyTarget.DISPLAY_TEXT,
                    AiStructuredApplyTarget.BLOG_POLISH
            )),
            Map.entry("blog.summary", List.of(
                    AiStructuredApplyTarget.DISPLAY_TEXT,
                    AiStructuredApplyTarget.BLOG_SUMMARY,
                    AiStructuredApplyTarget.BLOG_TAGS,
                    AiStructuredApplyTarget.BLOG_REJECT_TAGS
            )),
            Map.entry("knowledge.base.explain", List.of(
                    AiStructuredApplyTarget.DISPLAY_TEXT,
                    AiStructuredApplyTarget.KNOWLEDGE_BASE_EXPLAIN
            )),
            Map.entry("page.explain", List.of(
                    AiStructuredApplyTarget.DISPLAY_TEXT,
                    AiStructuredApplyTarget.PAGE_EXPLAIN
            ))
    );

    public String normalizeSceneCode(String sceneCode,
                                     String clientScene,
                                     String actionCode,
                                     AiSession.BizType bizType) {
        String normalized = normalizeSceneCodeOrNull(sceneCode, clientScene, actionCode);
        if (normalized != null) {
            return normalized;
        }
        AiSession.BizType safeBizType = bizType == null ? AiSession.BizType.GENERAL : bizType;
        return switch (safeBizType) {
            case GENERAL -> "general.chat";
            case PROJECT -> "project.assistant";
            case BLOG -> "blog.assistant";
            case CIRCLE -> "circle.assistant";
            case PAID_CONTENT -> "paid-content.assistant";
        };
    }

    public String normalizeSceneCodeOrNull(String sceneCode,
                                           String clientScene,
                                           String actionCode) {
        String fromRequest = normalizeSceneLikeCode(sceneCode);
        if (fromRequest != null) {
            return fromRequest;
        }
        String fromClient = normalizeSceneLikeCode(clientScene);
        if (fromClient != null) {
            return fromClient;
        }
        String fromAction = structuredCodeFromAction(actionCode);
        if (fromAction != null) {
            return fromAction;
        }
        return null;
    }

    public String normalizeActionCode(String actionCode) {
        String normalized = normalizeCode(actionCode);
        if (normalized == null) {
            return null;
        }
        String aliased = ACTION_ALIASES.getOrDefault(normalized, normalized);
        if (STRUCTURED_CODES.contains(aliased) || ACTION_MODE_HINT.containsKey(aliased)) {
            return aliased;
        }
        return normalized;
    }

    public AiAnalysisMode resolveAnalysisModeFromAction(String actionCode) {
        String normalized = normalizeActionCode(actionCode);
        if (normalized == null) {
            return null;
        }
        return ACTION_MODE_HINT.get(normalized);
    }

    public String resolveStructuredCode(String sceneCode, String actionCode) {
        String actionStructured = structuredCodeFromAction(actionCode);
        if (actionStructured != null) {
            return actionStructured;
        }
        String normalizedScene = normalizeSceneLikeCode(sceneCode);
        if (normalizedScene != null && STRUCTURED_CODES.contains(normalizedScene)) {
            return normalizedScene;
        }
        return null;
    }

    public List<AiStructuredApplyTarget> resolveApplyTargets(String sceneCode, String actionCode) {
        String structuredCode = resolveStructuredCode(sceneCode, actionCode);
        if (structuredCode == null) {
            return Collections.emptyList();
        }
        List<AiStructuredApplyTarget> targets = APPLY_TARGETS.get(structuredCode);
        if (targets == null || targets.isEmpty()) {
            return Collections.emptyList();
        }
        return List.copyOf(targets);
    }

    public boolean isKnownSceneCode(String sceneCode) {
        String normalized = normalizeSceneLikeCode(sceneCode);
        return normalized != null && KNOWN_SCENE_CODES.contains(normalized);
    }

    private String structuredCodeFromAction(String actionCode) {
        String normalizedAction = normalizeActionCode(actionCode);
        if (normalizedAction == null) {
            return null;
        }
        return STRUCTURED_CODES.contains(normalizedAction) ? normalizedAction : null;
    }

    private String normalizeSceneLikeCode(String code) {
        String normalized = normalizeCode(code);
        if (normalized == null) {
            return null;
        }
        String aliased = SCENE_ALIASES.getOrDefault(normalized, normalized);
        if (KNOWN_SCENE_CODES.contains(aliased)) {
            return aliased;
        }
        return normalized;
    }

    private String normalizeCode(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT)
                .replace('_', '.')
                .replace(' ', '.')
                .replaceAll("\\.+", ".")
                .replaceAll("\\.-", "-")
                .replaceAll("-\\.", "-");
        if (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return StringUtils.hasText(normalized) ? normalized : null;
    }
}
