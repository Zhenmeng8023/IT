package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.dto.request.KnowledgeChunkPreviewRequest;
import com.alikeyou.itmoduleai.dto.response.KnowledgeChunkPreviewResponse;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.alikeyou.itmoduleai.service.KnowledgeChunkingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KnowledgeChunkingServiceImpl implements KnowledgeChunkingService {

    private final ObjectMapper objectMapper;

    @Override
    public List<KnowledgeChunkPreviewResponse> previewChunks(
            KnowledgeBase knowledgeBase,
            KnowledgeDocument document,
            String rawText,
            KnowledgeChunkPreviewRequest request
    ) {
        String source = StringUtils.hasText(rawText) ? rawText : "";
        if (!StringUtils.hasText(source)) {
            return List.of();
        }

        String strategy = resolveStrategy(knowledgeBase, document, request);
        int maxChars = normalizeMaxChars(request == null ? null : request.getMaxChars(), strategy);
        int overlapChars = normalizeOverlapChars(request == null ? null : request.getOverlapChars(), maxChars);

        List<ChunkPart> parts = switch (strategy) {
            case "MARKDOWN" -> chunkMarkdown(document, source, maxChars, overlapChars);
            case "CODE", "CUSTOM" -> chunkCodeLike(document, source, maxChars, overlapChars);
            case "PARAGRAPH" -> chunkParagraph(document, source, maxChars, overlapChars);
            default -> chunkFixed(document, source, maxChars, overlapChars);
        };

        List<KnowledgeChunkPreviewResponse> result = new ArrayList<>();
        int idx = 0;
        for (ChunkPart part : parts) {
            if (!StringUtils.hasText(part.content())) {
                continue;
            }
            result.add(KnowledgeChunkPreviewResponse.builder()
                    .chunkIndex(idx++)
                    .title(part.title())
                    .content(part.content())
                    .tokenCount(estimateTokens(part.content()))
                    .charCount(part.content().length())
                    .startOffset(part.startOffset())
                    .endOffset(part.endOffset())
                    .metadataJson(toMetadataJson(knowledgeBase, document, part, strategy))
                    .build());
        }
        return result;
    }

    private String resolveStrategy(KnowledgeBase knowledgeBase, KnowledgeDocument document, KnowledgeChunkPreviewRequest request) {
        if (request != null && StringUtils.hasText(request.getChunkStrategy())) {
            return request.getChunkStrategy().trim().toUpperCase(Locale.ROOT);
        }

        String kbStrategy = knowledgeBase != null && knowledgeBase.getChunkStrategy() != null
                ? knowledgeBase.getChunkStrategy().name()
                : null;

        String archiveEntryPath = document == null ? "" : safe(document.getArchiveEntryPath());
        String fileName = document == null ? "" : safe(document.getFileName());
        String mimeType = document == null ? "" : safe(document.getMimeType());
        String lower = (archiveEntryPath + " " + fileName + " " + mimeType).toLowerCase(Locale.ROOT);

        if ("FIXED".equals(kbStrategy) || "MARKDOWN".equals(kbStrategy) || "CUSTOM".equals(kbStrategy)) {
            return kbStrategy;
        }

        if (lower.endsWith(".md") || lower.contains("markdown")) {
            return "MARKDOWN";
        }

        if (isCodeLikeFile(lower)) {
            return "CODE";
        }

        return "PARAGRAPH";
    }

    private boolean isCodeLikeFile(String lower) {
        return lower.endsWith(".java")
                || lower.endsWith(".js")
                || lower.endsWith(".ts")
                || lower.endsWith(".jsx")
                || lower.endsWith(".tsx")
                || lower.endsWith(".vue")
                || lower.endsWith(".css")
                || lower.endsWith(".scss")
                || lower.endsWith(".less")
                || lower.endsWith(".html")
                || lower.endsWith(".json")
                || lower.endsWith(".sql")
                || lower.endsWith(".xml")
                || lower.endsWith(".yml")
                || lower.endsWith(".yaml");
    }

    private int normalizeMaxChars(Integer value, String strategy) {
        int fallback = switch (strategy) {
            case "CODE", "CUSTOM" -> 1200;
            case "MARKDOWN" -> 1500;
            default -> 1000;
        };
        if (value == null) {
            return fallback;
        }
        return Math.max(300, Math.min(value, 5000));
    }

    private int normalizeOverlapChars(Integer value, int maxChars) {
        if (value == null) {
            return Math.min(120, Math.max(40, maxChars / 8));
        }
        return Math.max(0, Math.min(value, Math.max(0, maxChars - 100)));
    }

    private List<ChunkPart> chunkMarkdown(KnowledgeDocument document, String source, int maxChars, int overlapChars) {
        List<ChunkPart> result = new ArrayList<>();
        String[] lines = source.split("\\R", -1);
        String currentTitle = defaultTitle(document, "Markdown");
        StringBuilder buffer = new StringBuilder();
        int offset = 0;
        int blockStart = 0;
        for (String line : lines) {
            String normalized = line == null ? "" : line;
            boolean heading = normalized.stripLeading().startsWith("#");
            if (heading && buffer.length() > 0) {
                appendWindowed(result, currentTitle, buffer.toString().trim(), blockStart, blockStart + buffer.length(), maxChars, overlapChars);
                buffer.setLength(0);
                blockStart = offset;
            }
            if (heading) {
                currentTitle = normalized.strip();
            }
            buffer.append(normalized).append('\n');
            offset += normalized.length() + 1;
        }
        if (buffer.length() > 0) {
            appendWindowed(result, currentTitle, buffer.toString().trim(), blockStart, blockStart + buffer.length(), maxChars, overlapChars);
        }
        return result;
    }

    private List<ChunkPart> chunkParagraph(KnowledgeDocument document, String source, int maxChars, int overlapChars) {
        List<ChunkPart> result = new ArrayList<>();
        String[] paragraphs = source.split("(\\R\\s*\\R)+");
        String title = defaultTitle(document, "段落");
        StringBuilder buffer = new StringBuilder();
        int offset = 0;
        int blockStart = 0;
        for (String paragraph : paragraphs) {
            String text = paragraph == null ? "" : paragraph.trim();
            if (!StringUtils.hasText(text)) {
                continue;
            }
            if (buffer.length() == 0) {
                blockStart = offset;
            }
            if (buffer.length() + text.length() + 2 > maxChars && buffer.length() > 0) {
                appendWindowed(result, title, buffer.toString().trim(), blockStart, blockStart + buffer.length(), maxChars, overlapChars);
                buffer.setLength(0);
                blockStart = offset;
            }
            buffer.append(text).append("\n\n");
            offset += text.length() + 2;
        }
        if (buffer.length() > 0) {
            appendWindowed(result, title, buffer.toString().trim(), blockStart, blockStart + buffer.length(), maxChars, overlapChars);
        }
        if (result.isEmpty()) {
            return chunkFixed(document, source, maxChars, overlapChars);
        }
        return result;
    }

    private List<ChunkPart> chunkCodeLike(KnowledgeDocument document, String source, int maxChars, int overlapChars) {
        List<ChunkPart> result = new ArrayList<>();
        String[] lines = source.split("\\R", -1);
        StringBuilder buffer = new StringBuilder();
        String currentTitle = defaultTitle(document, "代码块");
        int offset = 0;
        int blockStart = 0;
        for (String line : lines) {
            String text = line == null ? "" : line;
            String trimmed = text.trim();
            boolean boundary = trimmed.startsWith("<template")
                    || trimmed.startsWith("<script")
                    || trimmed.startsWith("<style")
                    || trimmed.startsWith("export default")
                    || trimmed.startsWith("export const ")
                    || trimmed.startsWith("export function ")
                    || trimmed.startsWith("export async function ")
                    || trimmed.startsWith("import ")
                    || trimmed.startsWith("const ")
                    || trimmed.startsWith("let ")
                    || trimmed.startsWith("var ")
                    || trimmed.startsWith("function ")
                    || trimmed.startsWith("async function ")
                    || trimmed.startsWith("defineStore(")
                    || trimmed.startsWith("createRouter(")
                    || trimmed.startsWith("router.")
                    || trimmed.startsWith("setup(")
                    || trimmed.startsWith("watch(")
                    || trimmed.startsWith("computed(")
                    || trimmed.startsWith("onMounted(")
                    || trimmed.startsWith("//")
                    || trimmed.startsWith("class ")
                    || trimmed.contains(" class ")
                    || trimmed.startsWith("public class")
                    || trimmed.startsWith("interface ")
                    || trimmed.startsWith("enum ")
                    || trimmed.startsWith("def ")
                    || trimmed.startsWith("CREATE TABLE")
                    || trimmed.startsWith("create table")
                    || trimmed.startsWith("--")
                    || trimmed.startsWith("/*");
            if (boundary && buffer.length() > 0) {
                appendWindowed(result, currentTitle, buffer.toString().trim(), blockStart, blockStart + buffer.length(), maxChars, overlapChars);
                buffer.setLength(0);
                blockStart = offset;
            }
            if (boundary && StringUtils.hasText(trimmed)) {
                String title = trimmed;
                if (title.length() > 100) {
                    title = title.substring(0, 100);
                }
                currentTitle = title;
            }
            buffer.append(text).append('\n');
            offset += text.length() + 1;
        }
        if (buffer.length() > 0) {
            appendWindowed(result, currentTitle, buffer.toString().trim(), blockStart, blockStart + buffer.length(), maxChars, overlapChars);
        }
        if (result.isEmpty()) {
            return chunkFixed(document, source, maxChars, overlapChars);
        }
        return result;
    }

    private List<ChunkPart> chunkFixed(KnowledgeDocument document, String source, int maxChars, int overlapChars) {
        List<ChunkPart> result = new ArrayList<>();
        String title = defaultTitle(document, "文本块");
        int step = Math.max(1, maxChars - overlapChars);
        for (int start = 0, index = 0; start < source.length(); start += step, index++) {
            int end = Math.min(source.length(), start + maxChars);
            String content = source.substring(start, end).trim();
            if (!StringUtils.hasText(content)) {
                continue;
            }
            result.add(new ChunkPart(title + " #" + index, content, start, end));
            if (end >= source.length()) {
                break;
            }
        }
        return result;
    }

    private void appendWindowed(List<ChunkPart> result, String title, String blockText, int absoluteStart, int absoluteEnd, int maxChars, int overlapChars) {
        if (!StringUtils.hasText(blockText)) {
            return;
        }
        if (blockText.length() <= maxChars) {
            result.add(new ChunkPart(title, blockText, absoluteStart, absoluteEnd));
            return;
        }
        int step = Math.max(1, maxChars - overlapChars);
        for (int localStart = 0, index = 0; localStart < blockText.length(); localStart += step, index++) {
            int localEnd = Math.min(blockText.length(), localStart + maxChars);
            String text = blockText.substring(localStart, localEnd).trim();
            if (!StringUtils.hasText(text)) {
                continue;
            }
            result.add(new ChunkPart(title + " #" + index, text, absoluteStart + localStart, absoluteStart + localEnd));
            if (localEnd >= blockText.length()) {
                break;
            }
        }
    }

    private String defaultTitle(KnowledgeDocument document, String fallback) {
        if (document == null) {
            return fallback;
        }
        if (StringUtils.hasText(document.getTitle())) {
            return document.getTitle();
        }
        if (StringUtils.hasText(document.getFileName())) {
            return document.getFileName();
        }
        if (StringUtils.hasText(document.getArchiveEntryPath())) {
            return document.getArchiveEntryPath();
        }
        return fallback;
    }

    private int estimateTokens(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        return Math.max(1, text.length() / 4);
    }

    private String toMetadataJson(KnowledgeBase knowledgeBase, KnowledgeDocument document, ChunkPart part, String strategy) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("strategy", strategy);
        metadata.put("title", part.title());
        metadata.put("startOffset", part.startOffset());
        metadata.put("endOffset", part.endOffset());
        if (knowledgeBase != null) {
            metadata.put("knowledgeBaseId", knowledgeBase.getId());
            metadata.put("knowledgeBaseName", knowledgeBase.getName());
        }
        if (document != null) {
            metadata.put("documentId", document.getId());
            metadata.put("documentTitle", document.getTitle());
            metadata.put("fileName", document.getFileName());
            metadata.put("archiveEntryPath", document.getArchiveEntryPath());
            metadata.put("language", document.getLanguage());
            metadata.put("mimeType", document.getMimeType());
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record ChunkPart(String title, String content, int startOffset, int endOffset) {
    }
}
