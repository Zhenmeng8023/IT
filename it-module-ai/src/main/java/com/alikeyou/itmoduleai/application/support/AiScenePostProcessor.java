package com.alikeyou.itmoduleai.application.support;

import com.alikeyou.itmoduleai.enums.AiStructuredApplyTarget;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class AiScenePostProcessor {

    private static final Pattern JSON_FENCE = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final AiSceneActionCatalog SCENE_ACTION_CATALOG = new AiSceneActionCatalog();

    private final ObjectMapper objectMapper;

    public ProcessedAiResult process(String sceneCode, String rawText) {
        return process(sceneCode, null, rawText);
    }

    public ProcessedAiResult process(String sceneCode, String actionCode, String rawText) {
        String raw = rawText == null ? "" : rawText.trim();
        if (!StringUtils.hasText(raw)) {
            return new ProcessedAiResult("", Collections.emptyMap(), Collections.emptyList());
        }

        String structuredCode = SCENE_ACTION_CATALOG.resolveStructuredCode(sceneCode, actionCode);
        List<AiStructuredApplyTarget> applyTargets = SCENE_ACTION_CATALOG.resolveApplyTargets(sceneCode, actionCode);

        if (!StringUtils.hasText(structuredCode)) {
            return new ProcessedAiResult(raw, Collections.emptyMap(), Collections.emptyList());
        }

        try {
            return switch (structuredCode) {
                case "blog.polish" -> withTargets(processBlogPolish(raw), applyTargets);
                case "blog.summary" -> withTargets(processBlogSummary(raw), applyTargets);
                case "project.detail.summary" -> withTargets(processProjectSummary(raw), applyTargets);
                case "project.detail.tasks" -> withTargets(processProjectTasks(raw), applyTargets);
                case "project.detail.risks" -> withTargets(processProjectRisks(raw), applyTargets);
                case "project.detail.next-steps" -> withTargets(processProjectNextSteps(raw), applyTargets);
                case "knowledge.base.explain" -> withTargets(processKnowledgeBaseExplain(raw), applyTargets);
                case "page.explain" -> withTargets(processPageExplain(raw), applyTargets);
                default -> new ProcessedAiResult(raw, Collections.emptyMap(), Collections.emptyList());
            };
        } catch (RuntimeException ex) {
            return new ProcessedAiResult(raw, Collections.emptyMap(), Collections.emptyList());
        }
    }

    private ProcessedAiResult processBlogPolish(String raw) {
        Map<String, Object> json = tryParseJsonLike(raw);
        String polished = text(firstNonBlank(
                json.get("polishedContent"),
                json.get("content"),
                raw
        ));
        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("polishedContent", polished);
        structured.put("changeSummary", slice(textList(json.get("changeSummary")), 6));
        structured.put("warnings", slice(textList(json.get("warnings")), 5));
        structured.put("titleSuggestions", slice(textList(json.get("titleSuggestions")), 5));
        return new ProcessedAiResult(polished, structured, Collections.emptyList());
    }

    private ProcessedAiResult processBlogSummary(String raw) {
        Map<String, Object> json = tryParseJsonLike(raw);
        String summary = limit(text(firstNonBlank(json.get("summary"), raw)), 300);
        List<String> tags = cleanupTags(textList(json.get("tags")));
        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("summary", summary);
        structured.put("tags", slice(tags, 8));
        structured.put("rejectTags", slice(cleanupTags(textList(json.get("rejectTags"))), 8));
        return new ProcessedAiResult(summary, structured, Collections.emptyList());
    }

    private ProcessedAiResult processProjectSummary(String raw) {
        Map<String, Object> json = tryParseJsonLike(raw);
        String summary = limit(text(firstNonBlank(json.get("summary"), json.get("overview"), raw)), 360);
        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("summary", summary);
        structured.put("highlights", slice(textList(firstNonNull(json.get("highlights"), json.get("features"))), 8));
        structured.put("scenarios", slice(textList(json.get("scenarios")), 6));
        structured.put("risks", slice(textList(json.get("risks")), 6));
        structured.put("nextSteps", slice(textList(firstNonNull(json.get("nextSteps"), json.get("nextActions"))), 8));
        return new ProcessedAiResult(summary, structured, Collections.emptyList());
    }

    private ProcessedAiResult processProjectTasks(String raw) {
        Map<String, Object> json = tryParseJsonLike(raw);
        Map<String, Object> structured = new LinkedHashMap<>();

        List<Map<String, Object>> phases = normalizePhases(json.get("phases"));
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (Map<String, Object> phase : phases) {
            Object phaseTasks = phase.get("tasks");
            if (phaseTasks instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        tasks.add(new LinkedHashMap<>((Map<String, Object>) map));
                    }
                }
            }
        }
        tasks.addAll(normalizeTaskList(json.get("tasks"), 20));
        tasks = distinctTaskList(tasks, 20);

        structured.put("phases", phases);
        structured.put("tasks", tasks);
        structured.put("executionOrder", slice(textList(json.get("executionOrder")), 10));
        structured.put("risks", slice(textList(json.get("risks")), 6));

        String displayText = text(firstNonBlank(json.get("summary"), raw));
        return new ProcessedAiResult(displayText, structured, Collections.emptyList());
    }

    private ProcessedAiResult processProjectRisks(String raw) {
        Map<String, Object> json = tryParseJsonLike(raw);
        List<Map<String, Object>> risks = normalizeRiskList(firstNonNull(json.get("risks"), json.get("items")), 12);
        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("risks", risks);
        structured.put("highPriority", slice(textList(json.get("highPriority")), 8));
        structured.put("blockers", slice(textList(json.get("blockers")), 8));
        structured.put("mitigations", slice(textList(json.get("mitigations")), 12));

        String summary = text(firstNonBlank(json.get("summary"), json.get("riskSummary"), raw));
        return new ProcessedAiResult(summary, structured, Collections.emptyList());
    }

    private ProcessedAiResult processProjectNextSteps(String raw) {
        Map<String, Object> json = tryParseJsonLike(raw);
        List<Map<String, Object>> nextSteps = normalizeNextStepList(firstNonNull(json.get("nextSteps"), json.get("steps")), 12);

        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("nextSteps", nextSteps);
        structured.put("immediate", slice(textList(json.get("immediate")), 6));
        structured.put("dependencies", slice(textList(json.get("dependencies")), 8));
        structured.put("milestones", slice(textList(json.get("milestones")), 8));

        String display = text(firstNonBlank(json.get("summary"), raw));
        return new ProcessedAiResult(display, structured, Collections.emptyList());
    }

    private ProcessedAiResult processKnowledgeBaseExplain(String raw) {
        Map<String, Object> json = tryParseJsonLike(raw);
        String answer = limit(text(firstNonBlank(json.get("answer"), json.get("explain"), raw)), 600);
        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("answer", answer);
        structured.put("keyPoints", slice(textList(firstNonNull(json.get("keyPoints"), json.get("highlights"))), 10));
        structured.put("references", slice(referenceList(firstNonNull(json.get("references"), json.get("citations"))), 10));
        structured.put("unansweredQuestions", slice(textList(json.get("unansweredQuestions")), 8));
        return new ProcessedAiResult(answer, structured, Collections.emptyList());
    }

    private ProcessedAiResult processPageExplain(String raw) {
        Map<String, Object> json = tryParseJsonLike(raw);
        String pageSummary = limit(text(firstNonBlank(json.get("pageSummary"), json.get("summary"), raw)), 600);
        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("pageSummary", pageSummary);
        structured.put("sections", slice(sectionList(firstNonNull(json.get("sections"), json.get("blocks"))), 12));
        structured.put("dataFlow", slice(textList(json.get("dataFlow")), 10));
        structured.put("userActions", slice(textList(json.get("userActions")), 10));
        structured.put("caveats", slice(textList(json.get("caveats")), 8));
        return new ProcessedAiResult(pageSummary, structured, Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizePhases(Object value) {
        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> phases = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            if (phases.size() >= 6) {
                break;
            }
            Map<String, Object> phase = new LinkedHashMap<>();
            phase.put("name", limit(text(firstNonBlank(map.get("name"), map.get("phase"))), 40));
            phase.put("objective", limit(text(firstNonBlank(map.get("objective"), map.get("goal"))), 120));
            phase.put("tasks", normalizeTaskList(map.get("tasks"), 10));
            phases.add(phase);
        }
        return phases;
    }

    private List<Map<String, Object>> normalizeTaskList(Object value, int max) {
        if (!(value instanceof List<?> list) || max <= 0) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            if (tasks.size() >= max) {
                break;
            }
            Map<String, Object> task = new LinkedHashMap<>();
            task.put("title", limit(text(firstNonBlank(map.get("title"), map.get("name"))), 60));
            task.put("goal", limit(text(map.get("goal")), 160));
            task.put("owner", limit(text(map.get("owner")), 40));
            task.put("deliverable", limit(text(map.get("deliverable")), 120));
            task.put("priority", limit(text(map.get("priority")), 16));
            task.put("estimate", limit(text(map.get("estimate")), 24));
            task.put("dependsOn", slice(textList(map.get("dependsOn")), 6));
            tasks.add(task);
        }
        return tasks;
    }

    private List<Map<String, Object>> distinctTaskList(List<Map<String, Object>> tasks, int max) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Map<String, Object> task : tasks) {
            String title = text(task == null ? null : task.get("title"));
            if (!StringUtils.hasText(title)) {
                continue;
            }
            String key = title.toLowerCase(Locale.ROOT);
            if (!seen.add(key)) {
                continue;
            }
            result.add(task);
            if (result.size() >= max) {
                break;
            }
        }
        return result;
    }

    private List<Map<String, Object>> normalizeRiskList(Object value, int max) {
        if (!(value instanceof List<?> list) || max <= 0) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> risks = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            if (risks.size() >= max) {
                break;
            }
            Map<String, Object> risk = new LinkedHashMap<>();
            risk.put("risk", limit(text(firstNonBlank(map.get("risk"), map.get("title"), map.get("name"))), 120));
            risk.put("impact", limit(text(map.get("impact")), 120));
            risk.put("probability", limit(text(map.get("probability")), 24));
            risk.put("mitigation", limit(text(firstNonBlank(map.get("mitigation"), map.get("response"))), 160));
            risk.put("owner", limit(text(map.get("owner")), 40));
            risks.add(risk);
        }
        return risks;
    }

    private List<Map<String, Object>> normalizeNextStepList(Object value, int max) {
        if (!(value instanceof List<?> list) || max <= 0) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> steps = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            if (steps.size() >= max) {
                break;
            }
            Map<String, Object> step = new LinkedHashMap<>();
            step.put("step", limit(text(firstNonBlank(map.get("step"), map.get("title"), map.get("action"))), 160));
            step.put("owner", limit(text(map.get("owner")), 40));
            step.put("due", limit(text(firstNonBlank(map.get("due"), map.get("deadline"))), 32));
            step.put("dependency", limit(text(firstNonBlank(map.get("dependency"), map.get("dependsOn"))), 120));
            step.put("outcome", limit(text(firstNonBlank(map.get("outcome"), map.get("deliverable"))), 160));
            steps.add(step);
        }
        return steps;
    }

    private List<Map<String, Object>> referenceList(Object value) {
        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> references = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> normalized = new LinkedHashMap<>();
                normalized.put("title", limit(text(firstNonBlank(map.get("title"), map.get("documentTitle"))), 120));
                normalized.put("path", limit(text(firstNonBlank(map.get("path"), map.get("file"))), 200));
                normalized.put("chunkId", text(map.get("chunkId")));
                references.add(normalized);
            } else {
                String text = limit(text(item), 200);
                if (StringUtils.hasText(text)) {
                    references.add(Map.of("title", text));
                }
            }
            if (references.size() >= 12) {
                break;
            }
        }
        return references;
    }

    private List<Map<String, Object>> sectionList(Object value) {
        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> sections = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            Map<String, Object> section = new LinkedHashMap<>();
            section.put("title", limit(text(firstNonBlank(map.get("title"), map.get("name"))), 100));
            section.put("purpose", limit(text(firstNonBlank(map.get("purpose"), map.get("description"))), 200));
            section.put("dataSource", limit(text(firstNonBlank(map.get("dataSource"), map.get("data"))), 120));
            section.put("interaction", limit(text(firstNonBlank(map.get("interaction"), map.get("event"))), 200));
            sections.add(section);
            if (sections.size() >= 12) {
                break;
            }
        }
        return sections;
    }

    private Map<String, Object> tryParseJsonLike(String raw) {
        String content = raw == null ? "" : raw.trim();
        Matcher matcher = JSON_FENCE.matcher(content);
        if (matcher.find() && matcher.group(1) != null) {
            content = matcher.group(1).trim();
        }
        if (!StringUtils.hasText(content)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(content, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception ignored) {
            return new LinkedHashMap<>();
        }
    }

    private ProcessedAiResult withTargets(ProcessedAiResult source, List<AiStructuredApplyTarget> applyTargets) {
        if (source == null) {
            return new ProcessedAiResult("", Collections.emptyMap(), safeTargets(applyTargets));
        }
        return new ProcessedAiResult(
                source.displayText(),
                source.structured(),
                safeTargets(applyTargets)
        );
    }

    private List<AiStructuredApplyTarget> safeTargets(List<AiStructuredApplyTarget> targets) {
        if (targets == null || targets.isEmpty()) {
            return Collections.emptyList();
        }
        return List.copyOf(targets);
    }

    private Object firstNonBlank(Object... values) {
        if (values == null) {
            return "";
        }
        for (Object value : values) {
            String text = text(value);
            if (StringUtils.hasText(text)) {
                return value;
            }
        }
        return "";
    }

    private Object firstNonNull(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String limit(String value, int max) {
        String text = this.text(value);
        if (max <= 0 || text.length() <= max) {
            return text;
        }
        return text.substring(0, max).trim();
    }

    private List<String> textList(Object value) {
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                String text = text(item);
                if (StringUtils.hasText(text) && !result.contains(text)) {
                    result.add(text);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    private List<String> cleanupTags(List<String> tags) {
        Set<String> blocked = Set.of(
                "\u6280\u672f",
                "\u5f00\u53d1",
                "\u9879\u76ee",
                "\u535a\u5ba2",
                "\u6458\u8981",
                "\u6807\u7b7e",
                "\u603b\u7ed3"
        );
        List<String> result = new ArrayList<>();
        for (String tag : tags) {
            String value = text(tag).replaceFirst("^#", "");
            if (!StringUtils.hasText(value)) {
                continue;
            }
            if (value.length() < 2 || value.length() > 16) {
                continue;
            }
            if (blocked.contains(value.toLowerCase(Locale.ROOT))) {
                continue;
            }
            if (!result.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }

    private <T> List<T> slice(List<T> list, int max) {
        if (list == null || list.isEmpty() || max <= 0) {
            return Collections.emptyList();
        }
        return list.subList(0, Math.min(list.size(), max));
    }

    public record ProcessedAiResult(String displayText,
                                    Map<String, Object> structured,
                                    List<AiStructuredApplyTarget> applyTargets) {
    }
}
