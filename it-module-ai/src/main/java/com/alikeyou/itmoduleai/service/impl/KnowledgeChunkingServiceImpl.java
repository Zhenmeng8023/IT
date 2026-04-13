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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class KnowledgeChunkingServiceImpl implements KnowledgeChunkingService {

    private static final Pattern JAVA_PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([a-zA-Z0-9_.$]+)\\s*;");
    private static final Pattern JAVA_IMPORT_PATTERN = Pattern.compile("^\\s*import\\s+(?:static\\s+)?([a-zA-Z0-9_.$*]+)\\s*;");
    private static final Pattern JAVA_TYPE_PATTERN = Pattern.compile("\\b(class|interface|enum|record)\\s+([A-Za-z_$][\\w$]*)");
    private static final Pattern JAVA_METHOD_PATTERN = Pattern.compile("^(?:@[\\w.]+\\s*)*(?:public|private|protected|static|final|abstract|synchronized|native|default|\\s)+[\\w<>\\[\\], ?]+\\s+([A-Za-z_$][\\w$]*)\\s*\\([^;]*\\).*");
    private static final Pattern JAVA_ROUTE_PATTERN = Pattern.compile("@(?:RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\\s*\\(([^)]*)\\)");
    private static final Pattern JS_FUNCTION_PATTERN = Pattern.compile("^(?:export\\s+)?(?:async\\s+)?function\\s+([A-Za-z_$][\\w$]*)\\s*\\(.*");
    private static final Pattern JS_CLASS_PATTERN = Pattern.compile("^(?:export\\s+default\\s+|export\\s+)?class\\s+([A-Za-z_$][\\w$]*).*");
    private static final Pattern JS_CONST_PATTERN = Pattern.compile("^(?:export\\s+)?(?:const|let|var)\\s+([A-Za-z_$][\\w$]*)\\s*=.*");
    private static final Pattern JS_METHOD_PATTERN = Pattern.compile("^([A-Za-z_$][\\w$]*)\\s*[:(]\\s*(?:async\\s*)?(?:function)?\\s*\\(.*");
    private static final Pattern JS_IMPORT_FROM_PATTERN = Pattern.compile("^\\s*import\\s+.+\\s+from\\s+['\"]([^'\"]+)['\"]");
    private static final Pattern JS_IMPORT_SIDE_EFFECT_PATTERN = Pattern.compile("^\\s*import\\s+['\"]([^'\"]+)['\"]");
    private static final Pattern JS_ROUTE_PATH_PATTERN = Pattern.compile("\\bpath\\s*:\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern JS_ROUTE_PUSH_PATTERN = Pattern.compile("(?:router|\\$router)\\.(?:push|replace)\\s*\\(\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern VUE_ROUTE_LINK_PATTERN = Pattern.compile("<router-link[^>]*\\bto\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>");
    private static final Pattern JS_API_CALL_PATTERN = Pattern.compile("\\b(?:axios|fetch|\\$http|request)\\s*\\(");
    private static final Pattern API_URL_PATTERN = Pattern.compile("https?://[A-Za-z0-9._~:/?#\\[\\]@!$&'()*+,;=-]+");
    private static final Pattern CALL_PATTERN = Pattern.compile("\\b([A-Za-z_$][\\w$]*)\\s*\\(");
    private static final Pattern VUE_OPTION_PATTERN = Pattern.compile("^(props|emits|data|setup|methods|computed|watch|components|directives)\\s*[:(].*");
    private static final Pattern SQL_START_PATTERN = Pattern.compile("^(with|select|insert|update|delete|merge|create|alter|drop|truncate|grant|revoke|begin|commit|rollback)\\b.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_OBJECT_PATTERN = Pattern.compile("(?i)\\b(?:table|view|index|procedure|function|trigger)\\s+([`\"\\[]?[A-Za-z0-9_.$-]+[`\"\\]]?)");
    private static final Pattern SQL_DML_TABLE_PATTERN = Pattern.compile("(?i)\\b(?:from|join|into|update|delete\\s+from)\\s+([`\"\\[]?[A-Za-z0-9_.$-]+[`\"\\]]?)");
    private static final Pattern SQL_INLINE_PATTERN = Pattern.compile("(?i)\\b(select|insert|update|delete|merge)\\b");
    private static final Set<String> CONTROL_KEYWORDS = Set.of(
            "if", "for", "while", "switch", "catch", "return", "throw", "new", "typeof", "await", "function", "class", "super"
    );

    private final ObjectMapper objectMapper;

    @Override
    public List<KnowledgeChunkPreviewResponse> previewChunks(
            KnowledgeBase knowledgeBase,
            KnowledgeDocument document,
            String rawText,
            KnowledgeChunkPreviewRequest request
    ) {
        IndexBuildResult draft = buildIndexDraft(knowledgeBase, document, rawText, request);
        List<KnowledgeChunkPreviewResponse> previews = new ArrayList<>(draft.chunks().size());
        for (ChunkDraft chunk : draft.chunks()) {
            previews.add(KnowledgeChunkPreviewResponse.builder()
                    .chunkIndex(chunk.chunkIndex())
                    .title(chunk.title())
                    .content(chunk.content())
                    .tokenCount(chunk.tokenCount())
                    .charCount(chunk.charCount())
                    .startOffset(chunk.startOffset())
                    .endOffset(chunk.endOffset())
                    .metadataJson(chunk.metadataJson())
                    .build());
        }
        return previews;
    }

    @Override
    public IndexBuildResult buildIndexDraft(
            KnowledgeBase knowledgeBase,
            KnowledgeDocument document,
            String rawText,
            KnowledgeChunkPreviewRequest request
    ) {
        String source = normalizeLineEndings(rawText);
        if (!StringUtils.hasText(source)) {
            return IndexBuildResult.empty();
        }

        String strategy = resolveStrategy(knowledgeBase, document, request);
        int maxChars = normalizeMaxChars(request == null ? null : request.getMaxChars(), strategy);
        int overlapChars = normalizeOverlapChars(request == null ? null : request.getOverlapChars(), maxChars);
        String path = resolvePath(document);
        String language = resolveLanguage(document, path);
        List<SourceLine> lines = toLines(source);

        List<ChunkPart> parts = switch (strategy) {
            case "MARKDOWN" -> chunkMarkdown(document, path, language, lines, maxChars, overlapChars);
            case "CODE", "CUSTOM" -> chunkCode(document, path, language, lines, maxChars, overlapChars);
            case "PARAGRAPH" -> chunkParagraph(document, path, language, lines, maxChars, overlapChars);
            default -> chunkFixed(document, path, language, source, maxChars, overlapChars);
        };

        List<ChunkDraft> chunkDrafts = toChunkDrafts(knowledgeBase, document, strategy, parts);
        List<SymbolDraft> symbolDrafts = buildSymbolDrafts(language, path, lines, chunkDrafts);
        List<ReferenceDraft> referenceDrafts = buildReferenceDrafts(language, path, lines, chunkDrafts, symbolDrafts);
        return new IndexBuildResult(chunkDrafts, symbolDrafts, referenceDrafts);
    }

    private List<ChunkDraft> toChunkDrafts(KnowledgeBase knowledgeBase,
                                           KnowledgeDocument document,
                                           String strategy,
                                           List<ChunkPart> parts) {
        List<ChunkDraft> drafts = new ArrayList<>();
        int idx = 0;
        for (ChunkPart part : parts) {
            if (!StringUtils.hasText(part.content())) {
                continue;
            }
            drafts.add(new ChunkDraft(
                    idx++,
                    part.title(),
                    part.content(),
                    estimateTokens(part.content()),
                    part.content().length(),
                    part.startOffset(),
                    part.endOffset(),
                    part.startLine(),
                    part.endLine(),
                    toMetadataJson(knowledgeBase, document, part, strategy),
                    part.language(),
                    part.path(),
                    part.symbolName(),
                    part.symbolType(),
                    part.sectionName()
            ));
        }
        return drafts;
    }

    private List<SymbolDraft> buildSymbolDrafts(String language,
                                                String path,
                                                List<SourceLine> lines,
                                                List<ChunkDraft> chunks) {
        if (!isCodeIndexLanguage(language)) {
            return List.of();
        }
        List<SymbolDraft> symbols = new ArrayList<>();
        switch (language) {
            case "java" -> buildJavaSymbols(path, lines, chunks, symbols);
            case "sql" -> buildSqlSymbols(path, lines, chunks, symbols);
            case "javascript", "typescript", "jsx", "tsx", "vue" -> buildJsLikeSymbols(language, path, lines, chunks, symbols);
            default -> {
            }
        }
        return symbols;
    }

    private void buildJavaSymbols(String path,
                                  List<SourceLine> lines,
                                  List<ChunkDraft> chunks,
                                  List<SymbolDraft> symbols) {
        String packageName = "";
        String currentType = null;
        for (SourceLine line : lines) {
            String trimmed = line.text().trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            Matcher packageMatcher = JAVA_PACKAGE_PATTERN.matcher(trimmed);
            if (packageMatcher.find()) {
                packageName = packageMatcher.group(1);
                continue;
            }

            Matcher type = JAVA_TYPE_PATTERN.matcher(trimmed);
            if (type.find()) {
                String symbolName = type.group(2);
                currentType = symbolName;
                String qualifiedName = StringUtils.hasText(packageName)
                        ? packageName + "." + symbolName
                        : symbolName;
                symbols.add(new SymbolDraft(
                        symbolKey(path, "TYPE", symbolName, line.lineNo()),
                        symbolName,
                        qualifiedName,
                        type.group(1).toUpperCase(Locale.ROOT),
                        "MODULE",
                        trimmed,
                        null,
                        detectVisibility(trimmed),
                        Boolean.TRUE,
                        Boolean.TRUE,
                        line.lineNo(),
                        null,
                        line.lineNo(),
                        null,
                        findChunkIndex(chunks, line.lineNo()),
                        "java",
                        path,
                        toJson(Map.of(
                                "parser", "regex-java",
                                "line", line.lineNo()
                        ))
                ));
                continue;
            }

            Matcher method = JAVA_METHOD_PATTERN.matcher(trimmed);
            if (method.matches() && !trimmed.contains(" class ")) {
                String methodName = method.group(1);
                if (isControlKeyword(methodName)) {
                    continue;
                }
                String owner = StringUtils.hasText(currentType) ? currentType : path;
                symbols.add(new SymbolDraft(
                        symbolKey(path, "METHOD", methodName, line.lineNo()),
                        methodName,
                        owner + "#" + methodName,
                        "METHOD",
                        StringUtils.hasText(currentType) ? "CLASS" : "MODULE",
                        trimmed,
                        null,
                        detectVisibility(trimmed),
                        Boolean.TRUE,
                        Boolean.TRUE,
                        line.lineNo(),
                        null,
                        line.lineNo(),
                        null,
                        findChunkIndex(chunks, line.lineNo()),
                        "java",
                        path,
                        toJson(Map.of(
                                "parser", "regex-java",
                                "line", line.lineNo(),
                                "owner", owner
                        ))
                ));
            }
        }
    }

    private void buildJsLikeSymbols(String language,
                                    String path,
                                    List<SourceLine> lines,
                                    List<ChunkDraft> chunks,
                                    List<SymbolDraft> symbols) {
        String normalizedLanguage = normalizeJsFamilyLanguage(language);
        for (SourceLine line : lines) {
            String trimmed = line.text().trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            SymbolInfo boundary = detectBoundary(normalizedLanguage, trimmed, "module");
            if (boundary == null || "import".equals(boundary.type())) {
                continue;
            }
            String symbolKind = switch (boundary.type()) {
                case "class" -> "CLASS";
                case "object-method", "vue-option" -> "METHOD";
                default -> "FUNCTION";
            };
            symbols.add(new SymbolDraft(
                    symbolKey(path, symbolKind, boundary.name(), line.lineNo()),
                    boundary.name(),
                    path + "#" + boundary.name(),
                    symbolKind,
                    "MODULE",
                    trimmed,
                    null,
                    null,
                    Boolean.TRUE,
                    trimmed.startsWith("export"),
                    line.lineNo(),
                    null,
                    line.lineNo(),
                    null,
                    findChunkIndex(chunks, line.lineNo()),
                    normalizedLanguage,
                    path,
                    toJson(Map.of(
                            "parser", "regex-js",
                            "line", line.lineNo()
                    ))
            ));
        }
    }

    private void buildSqlSymbols(String path,
                                 List<SourceLine> lines,
                                 List<ChunkDraft> chunks,
                                 List<SymbolDraft> symbols) {
        for (SourceLine line : lines) {
            String trimmed = line.text().trim();
            if (!SQL_START_PATTERN.matcher(trimmed).matches()) {
                continue;
            }
            SymbolInfo info = sqlSymbol(trimmed);
            String verb = info.sectionName().toUpperCase(Locale.ROOT);
            boolean declaration = "CREATE".equals(verb) || "ALTER".equals(verb) || "DROP".equals(verb);
            symbols.add(new SymbolDraft(
                    symbolKey(path, "SQL_" + verb, info.name(), line.lineNo()),
                    info.name(),
                    path + "#" + info.name(),
                    declaration ? "SQL_OBJECT" : "SQL_STATEMENT",
                    "MODULE",
                    trimmed,
                    null,
                    null,
                    declaration,
                    Boolean.TRUE,
                    line.lineNo(),
                    null,
                    line.lineNo(),
                    null,
                    findChunkIndex(chunks, line.lineNo()),
                    "sql",
                    path,
                    toJson(Map.of(
                            "parser", "regex-sql",
                            "verb", verb,
                            "line", line.lineNo()
                    ))
            ));
        }
    }

    private boolean isCodeIndexLanguage(String language) {
        String normalized = normalizeLanguage(language);
        return "java".equals(normalized)
                || "javascript".equals(normalized)
                || "typescript".equals(normalized)
                || "vue".equals(normalized)
                || "sql".equals(normalized)
                || "jsx".equals(normalized)
                || "tsx".equals(normalized);
    }

    private String normalizeJsFamilyLanguage(String language) {
        if ("vue".equals(language)) {
            return "typescript";
        }
        if ("jsx".equals(language)) {
            return "javascript";
        }
        if ("tsx".equals(language)) {
            return "typescript";
        }
        return language;
    }

    private Integer findChunkIndex(List<ChunkDraft> chunks, int lineNo) {
        for (ChunkDraft chunk : chunks) {
            if (chunk.startLine() == null || chunk.endLine() == null) {
                continue;
            }
            if (lineNo >= chunk.startLine() && lineNo <= chunk.endLine()) {
                return chunk.chunkIndex();
            }
        }
        return chunks.isEmpty() ? null : chunks.get(0).chunkIndex();
    }

    private String symbolKey(String path, String kind, String name, int lineNo) {
        return safe(path) + ":" + safe(kind) + ":" + safe(name) + ":" + lineNo;
    }

    private String detectVisibility(String declarationLine) {
        String lower = safe(declarationLine).toLowerCase(Locale.ROOT);
        if (lower.contains("public ")) return "PUBLIC";
        if (lower.contains("protected ")) return "PROTECTED";
        if (lower.contains("private ")) return "PRIVATE";
        return "DEFAULT";
    }

    private List<ReferenceDraft> buildReferenceDrafts(String language,
                                                      String path,
                                                      List<SourceLine> lines,
                                                      List<ChunkDraft> chunks,
                                                      List<SymbolDraft> symbols) {
        if (!isCodeIndexLanguage(language)) {
            return List.of();
        }
        List<ReferenceDraft> refs = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String normalizedLanguage = normalizeJsFamilyLanguage(language);

        for (SourceLine line : lines) {
            String trimmed = line.text().trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            Integer chunkIndex = findChunkIndex(chunks, line.lineNo());
            String fromSymbolKey = findOwnerSymbolKey(symbols, line.lineNo());

            if ("java".equals(language)) {
                addJavaReferences(path, trimmed, line.lineNo(), chunkIndex, fromSymbolKey, refs, seen);
            } else if ("sql".equals(language)) {
                addSqlReferences(path, trimmed, line.lineNo(), chunkIndex, fromSymbolKey, refs, seen);
            } else {
                addJsLikeReferences(normalizedLanguage, path, trimmed, line.lineNo(), chunkIndex, fromSymbolKey, refs, seen);
            }
        }
        return refs;
    }

    private void addJavaReferences(String path,
                                   String line,
                                   int lineNo,
                                   Integer chunkIndex,
                                   String fromSymbolKey,
                                   List<ReferenceDraft> refs,
                                   Set<String> seen) {
        Matcher importMatcher = JAVA_IMPORT_PATTERN.matcher(line);
        if (importMatcher.find()) {
            String imported = importMatcher.group(1);
            addReference(refs, seen, new ReferenceDraft(
                    referenceKey(path, "IMPORT", imported, lineNo),
                    fromSymbolKey,
                    "IMPORT",
                    imported,
                    imported,
                    lineNo,
                    lineNo,
                    chunkIndex,
                    "EXTERNAL",
                    path,
                    "java",
                    toJson(Map.of("parser", "regex-java", "line", lineNo))
            ));
        }

        Matcher routeMatcher = JAVA_ROUTE_PATTERN.matcher(line);
        if (routeMatcher.find()) {
            String routePath = extractRoutePath(routeMatcher.group(1));
            if (StringUtils.hasText(routePath)) {
                addReference(refs, seen, new ReferenceDraft(
                        referenceKey(path, "ROUTE", routePath, lineNo),
                        fromSymbolKey,
                        "ROUTE",
                        routePath,
                        routePath,
                        lineNo,
                        lineNo,
                        chunkIndex,
                        "EXTERNAL",
                        path,
                        "java",
                        toJson(Map.of("parser", "regex-java", "line", lineNo))
                ));
            }
        }

        addCallReferences("java", path, line, lineNo, chunkIndex, fromSymbolKey, refs, seen);
        addInlineSqlReferences("java", path, line, lineNo, chunkIndex, fromSymbolKey, refs, seen);
        addApiUrlReferences("java", path, line, lineNo, chunkIndex, fromSymbolKey, refs, seen);
    }

    private void addJsLikeReferences(String language,
                                     String path,
                                     String line,
                                     int lineNo,
                                     Integer chunkIndex,
                                     String fromSymbolKey,
                                     List<ReferenceDraft> refs,
                                     Set<String> seen) {
        Matcher importFrom = JS_IMPORT_FROM_PATTERN.matcher(line);
        if (importFrom.find()) {
            String imported = importFrom.group(1);
            addReference(refs, seen, new ReferenceDraft(
                    referenceKey(path, "IMPORT", imported, lineNo),
                    fromSymbolKey,
                    "IMPORT",
                    imported,
                    imported,
                    lineNo,
                    lineNo,
                    chunkIndex,
                    "EXTERNAL",
                    path,
                    language,
                    toJson(Map.of("parser", "regex-js", "line", lineNo))
            ));
        } else {
            Matcher importSideEffect = JS_IMPORT_SIDE_EFFECT_PATTERN.matcher(line);
            if (importSideEffect.find()) {
                String imported = importSideEffect.group(1);
                addReference(refs, seen, new ReferenceDraft(
                        referenceKey(path, "IMPORT", imported, lineNo),
                        fromSymbolKey,
                        "IMPORT",
                        imported,
                        imported,
                        lineNo,
                        lineNo,
                        chunkIndex,
                        "EXTERNAL",
                        path,
                        language,
                        toJson(Map.of("parser", "regex-js", "line", lineNo))
                ));
            }
        }

        Matcher routePath = JS_ROUTE_PATH_PATTERN.matcher(line);
        while (routePath.find()) {
            String route = routePath.group(1);
            addReference(refs, seen, new ReferenceDraft(
                    referenceKey(path, "ROUTE", route, lineNo),
                    fromSymbolKey,
                    "ROUTE",
                    route,
                    route,
                    lineNo,
                    lineNo,
                    chunkIndex,
                    "EXTERNAL",
                    path,
                    language,
                    toJson(Map.of("parser", "regex-js", "line", lineNo))
            ));
        }

        Matcher routePush = JS_ROUTE_PUSH_PATTERN.matcher(line);
        while (routePush.find()) {
            String route = routePush.group(1);
            addReference(refs, seen, new ReferenceDraft(
                    referenceKey(path, "ROUTE", route, lineNo),
                    fromSymbolKey,
                    "ROUTE",
                    route,
                    route,
                    lineNo,
                    lineNo,
                    chunkIndex,
                    "EXTERNAL",
                    path,
                    language,
                    toJson(Map.of("parser", "regex-js", "line", lineNo))
            ));
        }

        Matcher vueRoute = VUE_ROUTE_LINK_PATTERN.matcher(line);
        while (vueRoute.find()) {
            String route = vueRoute.group(1);
            addReference(refs, seen, new ReferenceDraft(
                    referenceKey(path, "ROUTE", route, lineNo),
                    fromSymbolKey,
                    "ROUTE",
                    route,
                    route,
                    lineNo,
                    lineNo,
                    chunkIndex,
                    "EXTERNAL",
                    path,
                    language,
                    toJson(Map.of("parser", "regex-vue", "line", lineNo))
            ));
        }

        if (JS_API_CALL_PATTERN.matcher(line).find()) {
            addReference(refs, seen, new ReferenceDraft(
                    referenceKey(path, "API", "http-call", lineNo),
                    fromSymbolKey,
                    "API",
                    "http-call",
                    null,
                    lineNo,
                    lineNo,
                    chunkIndex,
                    "EXTERNAL",
                    path,
                    language,
                    toJson(Map.of("parser", "regex-js", "line", lineNo))
            ));
        }

        addApiUrlReferences(language, path, line, lineNo, chunkIndex, fromSymbolKey, refs, seen);
        addCallReferences(language, path, line, lineNo, chunkIndex, fromSymbolKey, refs, seen);
        addInlineSqlReferences(language, path, line, lineNo, chunkIndex, fromSymbolKey, refs, seen);
    }

    private void addSqlReferences(String path,
                                  String line,
                                  int lineNo,
                                  Integer chunkIndex,
                                  String fromSymbolKey,
                                  List<ReferenceDraft> refs,
                                  Set<String> seen) {
        Matcher table = SQL_DML_TABLE_PATTERN.matcher(line);
        while (table.find()) {
            String tableName = sanitizeSqlName(table.group(1));
            if (!StringUtils.hasText(tableName)) {
                continue;
            }
            addReference(refs, seen, new ReferenceDraft(
                    referenceKey(path, "SQL_USE", tableName, lineNo),
                    fromSymbolKey,
                    "SQL_USE",
                    tableName,
                    tableName,
                    lineNo,
                    lineNo,
                    chunkIndex,
                    "UNRESOLVED",
                    path,
                    "sql",
                    toJson(Map.of("parser", "regex-sql", "line", lineNo))
            ));
        }
    }

    private void addApiUrlReferences(String language,
                                     String path,
                                     String line,
                                     int lineNo,
                                     Integer chunkIndex,
                                     String fromSymbolKey,
                                     List<ReferenceDraft> refs,
                                     Set<String> seen) {
        Matcher urlMatcher = API_URL_PATTERN.matcher(line);
        while (urlMatcher.find()) {
            String url = urlMatcher.group();
            addReference(refs, seen, new ReferenceDraft(
                    referenceKey(path, "API", url, lineNo),
                    fromSymbolKey,
                    "API",
                    url,
                    url,
                    lineNo,
                    lineNo,
                    chunkIndex,
                    "EXTERNAL",
                    path,
                    language,
                    toJson(Map.of("parser", "regex-url", "line", lineNo))
            ));
        }
    }

    private void addInlineSqlReferences(String language,
                                        String path,
                                        String line,
                                        int lineNo,
                                        Integer chunkIndex,
                                        String fromSymbolKey,
                                        List<ReferenceDraft> refs,
                                        Set<String> seen) {
        if (!SQL_INLINE_PATTERN.matcher(line).find()) {
            return;
        }
        Matcher table = SQL_DML_TABLE_PATTERN.matcher(line);
        while (table.find()) {
            String tableName = sanitizeSqlName(table.group(1));
            if (!StringUtils.hasText(tableName)) {
                continue;
            }
            addReference(refs, seen, new ReferenceDraft(
                    referenceKey(path, "SQL_USE", tableName, lineNo),
                    fromSymbolKey,
                    "SQL_USE",
                    tableName,
                    tableName,
                    lineNo,
                    lineNo,
                    chunkIndex,
                    "UNRESOLVED",
                    path,
                    language,
                    toJson(Map.of("parser", "regex-inline-sql", "line", lineNo))
            ));
        }
    }

    private void addCallReferences(String language,
                                   String path,
                                   String line,
                                   int lineNo,
                                   Integer chunkIndex,
                                   String fromSymbolKey,
                                   List<ReferenceDraft> refs,
                                   Set<String> seen) {
        if (isDeclarationLine(line, language)) {
            return;
        }
        Matcher callMatcher = CALL_PATTERN.matcher(line);
        while (callMatcher.find()) {
            String callName = callMatcher.group(1);
            if (isControlKeyword(callName) || "import".equals(callName)) {
                continue;
            }
            addReference(refs, seen, new ReferenceDraft(
                    referenceKey(path, "CALL", callName, lineNo),
                    fromSymbolKey,
                    "CALL",
                    callName,
                    callName,
                    lineNo,
                    lineNo,
                    chunkIndex,
                    "UNRESOLVED",
                    path,
                    language,
                    toJson(Map.of("parser", "regex-call", "line", lineNo))
            ));
        }
    }

    private boolean isDeclarationLine(String line, String language) {
        String trimmed = line.trim();
        if ("java".equals(language)) {
            return JAVA_TYPE_PATTERN.matcher(trimmed).find() || JAVA_METHOD_PATTERN.matcher(trimmed).matches();
        }
        return JS_FUNCTION_PATTERN.matcher(trimmed).matches()
                || JS_CLASS_PATTERN.matcher(trimmed).matches()
                || JS_CONST_PATTERN.matcher(trimmed).matches();
    }

    private String findOwnerSymbolKey(List<SymbolDraft> symbols, int lineNo) {
        String candidate = null;
        int bestStart = Integer.MIN_VALUE;
        for (SymbolDraft symbol : symbols) {
            if (symbol.startLine() == null || symbol.startLine() > lineNo) {
                continue;
            }
            if (symbol.endLine() != null && symbol.endLine() < lineNo) {
                continue;
            }
            if (symbol.startLine() >= bestStart) {
                bestStart = symbol.startLine();
                candidate = symbol.symbolKey();
            }
        }
        return candidate;
    }

    private void addReference(List<ReferenceDraft> refs, Set<String> seen, ReferenceDraft reference) {
        if (reference == null || !StringUtils.hasText(reference.referenceKey())) {
            return;
        }
        if (seen.add(reference.referenceKey())) {
            refs.add(reference);
        }
    }

    private String referenceKey(String path, String kind, String name, int lineNo) {
        String raw = safe(path) + ":" + safe(kind) + ":" + safe(name) + ":" + lineNo;
        return raw.length() > 120 ? Integer.toHexString(raw.hashCode()) + ":" + lineNo : raw;
    }

    private String extractRoutePath(String mapping) {
        if (!StringUtils.hasText(mapping)) {
            return null;
        }
        Matcher matcher = Pattern.compile("['\"]([^'\"]+)['\"]").matcher(mapping);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return mapping.trim();
    }

    private String sanitizeSqlName(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return raw.replace("`", "").replace("\"", "").replace("[", "").replace("]", "");
    }

    private String resolveStrategy(KnowledgeBase knowledgeBase, KnowledgeDocument document, KnowledgeChunkPreviewRequest request) {
        if (request != null && StringUtils.hasText(request.getChunkStrategy())) {
            return request.getChunkStrategy().trim().toUpperCase(Locale.ROOT);
        }
        String kbStrategy = knowledgeBase != null && knowledgeBase.getChunkStrategy() != null
                ? knowledgeBase.getChunkStrategy().name()
                : null;
        if ("FIXED".equals(kbStrategy) || "MARKDOWN".equals(kbStrategy) || "CUSTOM".equals(kbStrategy)) {
            return kbStrategy;
        }
        String path = resolvePath(document).toLowerCase(Locale.ROOT);
        String mimeType = document == null ? "" : safe(document.getMimeType()).toLowerCase(Locale.ROOT);
        if (path.endsWith(".md") || mimeType.contains("markdown")) {
            return "MARKDOWN";
        }
        if (isCodeLikeFile(path) || isCodeMimeType(mimeType)) {
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

    private boolean isCodeMimeType(String mimeType) {
        return mimeType.contains("json")
                || mimeType.contains("xml")
                || mimeType.contains("javascript")
                || mimeType.contains("typescript")
                || mimeType.contains("sql")
                || mimeType.contains("yaml");
    }

    private int normalizeMaxChars(Integer value, String strategy) {
        int fallback = switch (strategy) {
            case "CODE", "CUSTOM" -> 1400;
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
            return Math.min(120, Math.max(40, maxChars / 10));
        }
        return Math.max(0, Math.min(value, Math.max(0, maxChars - 100)));
    }

    private List<ChunkPart> chunkMarkdown(KnowledgeDocument document, String path, String language,
                                          List<SourceLine> lines, int maxChars, int overlapChars) {
        List<ChunkPart> result = new ArrayList<>();
        String currentTitle = defaultTitle(document, "Markdown");
        int start = 0;
        for (int i = 0; i < lines.size(); i++) {
            String text = lines.get(i).text().stripLeading();
            boolean heading = text.startsWith("#");
            if (heading && hasText(lines, start, i)) {
                appendLineRange(result, lines, start, i, currentTitle, language, path, headingName(currentTitle), "markdown-section", currentTitle, maxChars, overlapChars);
                start = i;
            }
            if (heading) {
                currentTitle = lines.get(i).text().strip();
            }
        }
        if (hasText(lines, start, lines.size())) {
            appendLineRange(result, lines, start, lines.size(), currentTitle, language, path, headingName(currentTitle), "markdown-section", currentTitle, maxChars, overlapChars);
        }
        return result;
    }

    private List<ChunkPart> chunkParagraph(KnowledgeDocument document, String path, String language,
                                           List<SourceLine> lines, int maxChars, int overlapChars) {
        List<ChunkPart> result = new ArrayList<>();
        String title = defaultTitle(document, "Paragraph");
        int start = -1;
        int blockStart = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (StringUtils.hasText(lines.get(i).text())) {
                if (start < 0) {
                    start = i;
                    blockStart = i;
                }
                int length = joinedLength(lines, blockStart, i + 1);
                if (length > maxChars && blockStart < i) {
                    appendLineRange(result, lines, blockStart, i, title, language, path, title, "paragraph", title, maxChars, overlapChars);
                    blockStart = i;
                }
            } else if (start >= 0 && hasText(lines, blockStart, i)) {
                appendLineRange(result, lines, blockStart, i, title, language, path, title, "paragraph", title, maxChars, overlapChars);
                start = -1;
                blockStart = -1;
            }
        }
        if (blockStart >= 0 && hasText(lines, blockStart, lines.size())) {
            appendLineRange(result, lines, blockStart, lines.size(), title, language, path, title, "paragraph", title, maxChars, overlapChars);
        }
        if (result.isEmpty()) {
            return chunkFixed(document, path, language, joinLines(lines, 0, lines.size()), maxChars, overlapChars);
        }
        return result;
    }

    private List<ChunkPart> chunkCode(KnowledgeDocument document, String path, String language,
                                      List<SourceLine> lines, int maxChars, int overlapChars) {
        return switch (language) {
            case "vue" -> chunkVue(document, path, lines, maxChars, overlapChars);
            case "sql" -> chunkSql(document, path, lines, maxChars, overlapChars);
            case "java", "javascript", "typescript", "jsx", "tsx" ->
                    chunkByBoundaries(document, path, language, lines, 0, lines.size(), "module", maxChars, overlapChars);
            default -> chunkByBoundaries(document, path, language, lines, 0, lines.size(), "code", maxChars, overlapChars);
        };
    }

    private List<ChunkPart> chunkVue(KnowledgeDocument document, String path,
                                     List<SourceLine> lines, int maxChars, int overlapChars) {
        List<ChunkPart> result = new ArrayList<>();
        int i = 0;
        while (i < lines.size()) {
            String trimmed = lines.get(i).text().trim().toLowerCase(Locale.ROOT);
            String tag = vueTag(trimmed);
            if (tag == null) {
                int start = i;
                while (i < lines.size() && vueTag(lines.get(i).text().trim().toLowerCase(Locale.ROOT)) == null) {
                    i++;
                }
                if (hasText(lines, start, i)) {
                    appendLineRange(result, lines, start, i, defaultTitle(document, "Vue file"), "vue", path, "module", "vue-fragment", "module", maxChars, overlapChars);
                }
                continue;
            }
            int start = i;
            i++;
            String closeTag = "</" + tag + ">";
            while (i < lines.size() && !lines.get(i).text().toLowerCase(Locale.ROOT).contains(closeTag)) {
                i++;
            }
            int end = Math.min(lines.size(), i + 1);
            if ("script".equals(tag)) {
                int innerStart = Math.min(start + 1, end);
                int innerEnd = Math.max(innerStart, end - 1);
                if (hasText(lines, innerStart, innerEnd)) {
                    String scriptLanguage = lines.get(start).text().toLowerCase(Locale.ROOT).contains("lang=\"ts\"")
                            || lines.get(start).text().toLowerCase(Locale.ROOT).contains("lang='ts'")
                            ? "typescript"
                            : "javascript";
                    result.addAll(chunkByBoundaries(document, path, scriptLanguage, lines, innerStart, innerEnd, "script", maxChars, overlapChars));
                } else {
                    appendLineRange(result, lines, start, end, "script", "vue", path, "script", "vue-script", "script", maxChars, overlapChars);
                }
            } else {
                appendLineRange(result, lines, start, end, tag, "vue", path, tag, "vue-" + tag, tag, maxChars, overlapChars);
            }
            i = end;
        }
        return result.isEmpty()
                ? chunkByBoundaries(document, path, "vue", lines, 0, lines.size(), "module", maxChars, overlapChars)
                : result;
    }

    private List<ChunkPart> chunkSql(KnowledgeDocument document, String path,
                                     List<SourceLine> lines, int maxChars, int overlapChars) {
        List<ChunkPart> result = new ArrayList<>();
        int start = 0;
        SymbolInfo current = new SymbolInfo("statement", "sql-statement", "statement", "SQL statement");
        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).text().trim();
            if (i > start && SQL_START_PATTERN.matcher(trimmed).matches() && hasText(lines, start, i)) {
                appendLineRange(result, lines, start, i, current.title(), "sql", path, current.name(), current.type(), current.sectionName(), maxChars, overlapChars);
                start = i;
                current = sqlSymbol(trimmed);
            } else if (i == start && StringUtils.hasText(trimmed)) {
                current = sqlSymbol(trimmed);
            }
            if (trimmed.endsWith(";") && hasText(lines, start, i + 1)) {
                appendLineRange(result, lines, start, i + 1, current.title(), "sql", path, current.name(), current.type(), current.sectionName(), maxChars, overlapChars);
                start = i + 1;
                current = new SymbolInfo("statement", "sql-statement", "statement", "SQL statement");
            }
        }
        if (hasText(lines, start, lines.size())) {
            appendLineRange(result, lines, start, lines.size(), current.title(), "sql", path, current.name(), current.type(), current.sectionName(), maxChars, overlapChars);
        }
        return result;
    }

    private List<ChunkPart> chunkByBoundaries(KnowledgeDocument document, String path, String language,
                                              List<SourceLine> lines, int from, int to, String section,
                                              int maxChars, int overlapChars) {
        List<ChunkPart> result = new ArrayList<>();
        int start = from;
        SymbolInfo current = new SymbolInfo(section, language + "-section", section, defaultTitle(document, section));
        boolean started = false;
        for (int i = from; i < to; i++) {
            String trimmed = lines.get(i).text().trim();
            SymbolInfo detected = detectBoundary(language, trimmed, section);
            if (detected != null) {
                if (started && hasText(lines, start, i) && shouldSplit(current, detected)) {
                    appendLineRange(result, lines, start, i, current.title(), language, path, current.name(), current.type(), current.sectionName(), maxChars, overlapChars);
                    start = i;
                }
                current = detected;
                started = true;
            } else if (StringUtils.hasText(trimmed)) {
                started = true;
            }
        }
        if (hasText(lines, start, to)) {
            appendLineRange(result, lines, start, to, current.title(), language, path, current.name(), current.type(), current.sectionName(), maxChars, overlapChars);
        }
        return result;
    }

    private boolean shouldSplit(SymbolInfo current, SymbolInfo detected) {
        if (current == null || detected == null) {
            return true;
        }
        return !("import".equals(current.type()) && "import".equals(detected.type()));
    }

    private SymbolInfo detectBoundary(String language, String trimmed, String section) {
        if (!StringUtils.hasText(trimmed)) {
            return null;
        }
        if ("java".equals(language)) {
            Matcher type = JAVA_TYPE_PATTERN.matcher(trimmed);
            if (type.find()) {
                return new SymbolInfo(type.group(2), type.group(1), "type", type.group(1) + " " + type.group(2));
            }
            Matcher method = JAVA_METHOD_PATTERN.matcher(trimmed);
            if (method.matches() && !trimmed.contains(" class ")) {
                return new SymbolInfo(method.group(1), "method", "method", "method " + method.group(1));
            }
            return null;
        }
        if ("javascript".equals(language) || "typescript".equals(language) || "jsx".equals(language) || "tsx".equals(language)) {
            if (trimmed.startsWith("import ")) {
                return new SymbolInfo("imports", "import", section, "imports");
            }
            if (trimmed.startsWith("export default")) {
                return new SymbolInfo("default", "export-default", section, "export default");
            }
            Matcher vueOption = VUE_OPTION_PATTERN.matcher(trimmed);
            if ("script".equals(section) && vueOption.matches()) {
                String name = vueOption.group(1);
                return new SymbolInfo(name, "vue-option", name, name);
            }
            Matcher cls = JS_CLASS_PATTERN.matcher(trimmed);
            if (cls.matches()) {
                return new SymbolInfo(cls.group(1), "class", section, "class " + cls.group(1));
            }
            Matcher fn = JS_FUNCTION_PATTERN.matcher(trimmed);
            if (fn.matches()) {
                return new SymbolInfo(fn.group(1), functionType(fn.group(1)), section, "function " + fn.group(1));
            }
            Matcher cn = JS_CONST_PATTERN.matcher(trimmed);
            if (cn.matches()) {
                String name = cn.group(1);
                return new SymbolInfo(name, functionType(name), section, "binding " + name);
            }
            Matcher method = JS_METHOD_PATTERN.matcher(trimmed);
            if (method.matches()) {
                String name = method.group(1);
                if (!isControlKeyword(name)) {
                    return new SymbolInfo(name, "object-method", section, "method " + name);
                }
            }
        }
        return null;
    }

    private boolean isControlKeyword(String name) {
        return CONTROL_KEYWORDS.contains(safe(name).toLowerCase(Locale.ROOT));
    }

    private String functionType(String name) {
        if (StringUtils.hasText(name) && name.startsWith("use") && name.length() > 3 && Character.isUpperCase(name.charAt(3))) {
            return "hook";
        }
        return "function";
    }

    private SymbolInfo sqlSymbol(String trimmed) {
        String lower = trimmed.toLowerCase(Locale.ROOT);
        String verb = lower.split("\\s+", 2)[0];
        String name = "statement";
        Matcher named = SQL_OBJECT_PATTERN.matcher(trimmed);
        if (named.find()) {
            name = sanitizeSqlName(named.group(1));
        }
        return new SymbolInfo(name, "sql-" + verb, verb, verb + " " + name);
    }

    private String vueTag(String trimmedLower) {
        if (trimmedLower.startsWith("<template")) return "template";
        if (trimmedLower.startsWith("<script")) return "script";
        if (trimmedLower.startsWith("<style")) return "style";
        return null;
    }

    private List<ChunkPart> chunkFixed(KnowledgeDocument document, String path, String language,
                                       String source, int maxChars, int overlapChars) {
        List<ChunkPart> result = new ArrayList<>();
        String title = defaultTitle(document, "Text");
        int step = Math.max(1, maxChars - overlapChars);
        for (int start = 0, index = 0; start < source.length(); start += step, index++) {
            int end = Math.min(source.length(), start + maxChars);
            String content = source.substring(start, end).trim();
            if (StringUtils.hasText(content)) {
                int startLine = 1 + countNewlines(source, 0, start);
                int endLine = startLine + countNewlines(content, 0, content.length());
                result.add(new ChunkPart(title + " #" + index, content, start, end, startLine, endLine,
                        language, path, title, "text", title));
            }
            if (end >= source.length()) {
                break;
            }
        }
        return result;
    }

    private void appendLineRange(List<ChunkPart> result, List<SourceLine> lines, int from, int to,
                                 String title, String language, String path, String symbolName,
                                 String symbolType, String sectionName, int maxChars, int overlapChars) {
        if (from < 0 || to <= from || from >= lines.size()) {
            return;
        }
        int end = Math.min(to, lines.size());
        String content = joinLines(lines, from, end).trim();
        if (!StringUtils.hasText(content)) {
            return;
        }
        SourceLine first = lines.get(from);
        SourceLine last = lines.get(end - 1);
        ChunkPart part = new ChunkPart(
                title,
                content,
                first.startOffset(),
                last.endOffset(),
                first.lineNo(),
                last.lineNo(),
                language,
                path,
                symbolName,
                symbolType,
                sectionName
        );
        appendWindowed(result, part, maxChars, overlapChars);
    }

    private void appendWindowed(List<ChunkPart> result, ChunkPart part, int maxChars, int overlapChars) {
        if (part.content().length() <= maxChars) {
            result.add(part);
            return;
        }
        int step = Math.max(1, maxChars - overlapChars);
        String source = part.content();
        for (int localStart = 0, index = 0; localStart < source.length(); localStart += step, index++) {
            int localEnd = Math.min(source.length(), localStart + maxChars);
            String text = source.substring(localStart, localEnd).trim();
            if (!StringUtils.hasText(text)) {
                continue;
            }
            int startLine = part.startLine() + countNewlines(source, 0, localStart);
            int endLine = startLine + countNewlines(text, 0, text.length());
            result.add(new ChunkPart(
                    part.title() + " #" + index,
                    text,
                    part.startOffset() + localStart,
                    part.startOffset() + localEnd,
                    startLine,
                    endLine,
                    part.language(),
                    part.path(),
                    part.symbolName(),
                    part.symbolType(),
                    part.sectionName()
            ));
            if (localEnd >= source.length()) {
                break;
            }
        }
    }

    private List<SourceLine> toLines(String source) {
        String[] raw = source.split("\n", -1);
        List<SourceLine> lines = new ArrayList<>(raw.length);
        int offset = 0;
        for (int i = 0; i < raw.length; i++) {
            String text = raw[i];
            int end = offset + text.length();
            lines.add(new SourceLine(i + 1, text, offset, end));
            offset = end + 1;
        }
        return lines;
    }

    private String joinLines(List<SourceLine> lines, int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to && i < lines.size(); i++) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(lines.get(i).text());
        }
        return sb.toString();
    }

    private boolean hasText(List<SourceLine> lines, int from, int to) {
        for (int i = Math.max(0, from); i < to && i < lines.size(); i++) {
            if (StringUtils.hasText(lines.get(i).text())) {
                return true;
            }
        }
        return false;
    }

    private int joinedLength(List<SourceLine> lines, int from, int to) {
        int length = 0;
        for (int i = from; i < to && i < lines.size(); i++) {
            length += lines.get(i).text().length() + 1;
        }
        return length;
    }

    private int countNewlines(String source, int from, int to) {
        int count = 0;
        int start = Math.max(0, from);
        int end = Math.min(source.length(), Math.max(start, to));
        for (int i = start; i < end; i++) {
            if (source.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }

    private String normalizeLineEndings(String rawText) {
        return StringUtils.hasText(rawText)
                ? rawText.replace("\r\n", "\n").replace("\r", "\n")
                : "";
    }

    private String resolvePath(KnowledgeDocument document) {
        if (document == null) {
            return "";
        }
        if (StringUtils.hasText(document.getFilePath())) return document.getFilePath();
        if (StringUtils.hasText(document.getArchiveEntryPath())) return document.getArchiveEntryPath();
        if (StringUtils.hasText(document.getSourceUrl())) return document.getSourceUrl();
        if (StringUtils.hasText(document.getFileName())) return document.getFileName();
        return "";
    }

    private String resolveLanguage(KnowledgeDocument document, String path) {
        String byPath = languageByPath(path);
        if (!"text".equals(byPath)) {
            return byPath;
        }
        if (document != null && StringUtils.hasText(document.getLanguage())) {
            return normalizeLanguage(document.getLanguage());
        }
        return "text";
    }

    private String languageByPath(String path) {
        String lower = safe(path).toLowerCase(Locale.ROOT);
        if (lower.endsWith(".vue")) return "vue";
        if (lower.endsWith(".java")) return "java";
        if (lower.endsWith(".js")) return "javascript";
        if (lower.endsWith(".jsx")) return "jsx";
        if (lower.endsWith(".ts")) return "typescript";
        if (lower.endsWith(".tsx")) return "tsx";
        if (lower.endsWith(".sql")) return "sql";
        if (lower.endsWith(".css")) return "css";
        if (lower.endsWith(".scss")) return "scss";
        if (lower.endsWith(".less")) return "less";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "html";
        if (lower.endsWith(".json")) return "json";
        if (lower.endsWith(".xml")) return "xml";
        if (lower.endsWith(".yml") || lower.endsWith(".yaml")) return "yaml";
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) return "markdown";
        return "text";
    }

    private String normalizeLanguage(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "text";
        }
        String lower = raw.trim().toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "js" -> "javascript";
            case "ts" -> "typescript";
            default -> lower;
        };
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

    private String headingName(String title) {
        if (!StringUtils.hasText(title)) {
            return null;
        }
        return title.replaceFirst("^#+\\s*", "").trim();
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
        metadata.put("language", part.language());
        metadata.put("path", part.path());
        metadata.put("symbolName", part.symbolName());
        metadata.put("symbolType", part.symbolType());
        metadata.put("startLine", part.startLine());
        metadata.put("endLine", part.endLine());
        metadata.put("sectionName", part.sectionName());
        metadata.put("startOffset", part.startOffset());
        metadata.put("endOffset", part.endOffset());
        metadata.put("indexVersion", "semantic-v3");
        metadata.put("embeddingRebuild", "required-after-reindex");
        if (knowledgeBase != null) {
            metadata.put("knowledgeBaseId", knowledgeBase.getId());
            metadata.put("knowledgeBaseName", knowledgeBase.getName());
        }
        if (document != null) {
            metadata.put("documentId", document.getId());
            metadata.put("documentTitle", document.getTitle());
            metadata.put("fileName", document.getFileName());
            metadata.put("archiveEntryPath", document.getArchiveEntryPath());
            metadata.put("mimeType", document.getMimeType());
        }
        return toJson(metadata);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record SourceLine(int lineNo, String text, int startOffset, int endOffset) {
    }

    private record SymbolInfo(String name, String type, String sectionName, String title) {
    }

    private record ChunkPart(String title,
                             String content,
                             int startOffset,
                             int endOffset,
                             int startLine,
                             int endLine,
                             String language,
                             String path,
                             String symbolName,
                             String symbolType,
                             String sectionName) {
    }
}
