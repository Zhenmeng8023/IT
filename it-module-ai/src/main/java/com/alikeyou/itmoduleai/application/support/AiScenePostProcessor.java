package com.alikeyou.itmoduleai.application.support;

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

    private final ObjectMapper objectMapper;

    public ProcessedAiResult process(String sceneCode, String rawText) {
        String raw = rawText == null ? "" : rawText.trim();
        if (!StringUtils.hasText(raw)) {
            return new ProcessedAiResult("", Collections.emptyMap());
        }

        return switch (sceneCode == null ? "" : sceneCode) {
            case "blog.polish" -> processBlogPolish(raw);
            case "blog.summary" -> processBlogSummary(raw);
            case "project.detail.summary" -> processProjectSummary(raw);
            case "project.detail.tasks" -> processProjectTasks(raw);
            default -> new ProcessedAiResult(raw, Collections.emptyMap());
        };
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
        structured.put("changeSummary", slice(textList(json.get("changeSummary")), 4));
        structured.put("warnings", slice(textList(json.get("warnings")), 3));
        structured.put("titleSuggestions", slice(textList(json.get("titleSuggestions")), 3));
        return new ProcessedAiResult(polished, structured);
    }

    private ProcessedAiResult processBlogSummary(String raw) {
        Map<String, Object> json = tryParseJsonLike(raw);
        String summary = limit(text(firstNonBlank(json.get("summary"), raw)), 120);
        List<String> tags = cleanupTags(textList(json.get("tags")));
        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("summary", summary);
        structured.put("tags", slice(tags, 5));
        structured.put("rejectTags", slice(cleanupTags(textList(json.get("rejectTags"))), 5));
        return new ProcessedAiResult(summary, structured);
    }

    private ProcessedAiResult processProjectSummary(String raw) {
        Map<String, Object> json = tryParseJsonLike(raw);
        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("overview", limit(text(json.get("overview")), 60));
        structured.put("scenarios", slice(textList(json.get("scenarios")), 4));
        structured.put("features", slice(textList(json.get("features")), 5));
        structured.put("risks", slice(textList(json.get("risks")), 4));
        structured.put("nextActions", slice(textList(json.get("nextActions")), 4));
        return new ProcessedAiResult(raw, structured);
    }

    private ProcessedAiResult processProjectTasks(String raw) {
        Map<String, Object> json = tryParseJsonLike(raw);
        Map<String, Object> structured = new LinkedHashMap<>();

        List<Map<String, Object>> phases = new ArrayList<>();
        Object phaseObj = json.get("phases");
        if (phaseObj instanceof List<?> list) {
            int phaseCount = 0;
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> map) || phaseCount >= 3) continue;
                Map<String, Object> phase = new LinkedHashMap<>();
                phase.put("name", limit(text(map.get("name")), 20));

                List<Map<String, Object>> tasks = new ArrayList<>();
                Object taskObj = map.get("tasks");
                if (taskObj instanceof List<?> taskList) {
                    int taskCount = 0;
                    for (Object taskItem : taskList) {
                        if (!(taskItem instanceof Map<?, ?> taskMap) || taskCount >= 5) continue;
                        Map<String, Object> task = new LinkedHashMap<>();
                        task.put("title", limit(text(taskMap.get("title")), 24));
                        task.put("goal", limit(text(taskMap.get("goal")), 40));
                        task.put("deliverable", limit(text(taskMap.get("deliverable")), 40));
                        task.put("priority", limit(text(taskMap.get("priority")), 8));
                        task.put("estimate", limit(text(taskMap.get("estimate")), 16));
                        tasks.add(task);
                        taskCount++;
                    }
                }
                phase.put("tasks", tasks);
                phases.add(phase);
                phaseCount++;
            }
        }

        structured.put("phases", phases);
        structured.put("executionOrder", slice(textList(json.get("executionOrder")), 8));
        structured.put("risks", slice(textList(json.get("risks")), 4));
        return new ProcessedAiResult(raw, structured);
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
            return objectMapper.readValue(content, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private Object firstNonBlank(Object... values) {
        for (Object value : values) {
            String text = text(value);
            if (StringUtils.hasText(text)) return value;
        }
        return "";
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String limit(String value, int max) {
        String text = this.text(value);
        if (text.length() <= max) return text;
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
        Set<String> blocked = Set.of("技术", "开发", "项目", "博客", "摘要", "标签", "总结");
        List<String> result = new ArrayList<>();
        for (String tag : tags) {
            String value = text(tag).replaceFirst("^#", "");
            if (!StringUtils.hasText(value)) continue;
            if (value.length() < 2 || value.length() > 12) continue;
            if (blocked.contains(value)) continue;
            if (!result.contains(value)) result.add(value);
        }
        return result;
    }

    private <T> List<T> slice(List<T> list, int max) {
        if (list == null || list.isEmpty()) return Collections.emptyList();
        return list.subList(0, Math.min(list.size(), max));
    }

    public record ProcessedAiResult(String displayText, Map<String, Object> structured) {}
}