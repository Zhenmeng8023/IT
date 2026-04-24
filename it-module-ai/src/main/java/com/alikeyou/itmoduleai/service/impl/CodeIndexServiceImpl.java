package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.entity.AiCodeReference;
import com.alikeyou.itmoduleai.entity.AiCodeSymbol;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeChunk;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.alikeyou.itmoduleai.repository.AiCodeReferenceRepository;
import com.alikeyou.itmoduleai.repository.AiCodeSymbolRepository;
import com.alikeyou.itmoduleai.service.CodeIndexService;
import com.alikeyou.itmoduleai.service.KnowledgeChunkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CodeIndexServiceImpl implements CodeIndexService {

    private final AiCodeSymbolRepository aiCodeSymbolRepository;
    private final AiCodeReferenceRepository aiCodeReferenceRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearDocumentCodeIndex(Long documentId) {
        if (documentId == null) {
            return;
        }
        aiCodeReferenceRepository.deleteByDocumentGraphId(documentId);
        aiCodeSymbolRepository.deleteByDocumentId(documentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CodeIndexResult rebuildDocumentCodeIndex(
            KnowledgeBase knowledgeBase,
            KnowledgeDocument document,
            List<KnowledgeChunk> chunks,
            KnowledgeChunkingService.IndexBuildResult draft
    ) {
        if (knowledgeBase == null || document == null) {
            throw new IllegalArgumentException("Knowledge base and document are required");
        }

        clearDocumentCodeIndex(document.getId());

        String language = resolveLanguage(document, draft);
        if (!isSupported(language)) {
            return new CodeIndexResult(0, 0, KnowledgeDocument.SymbolIndexStatus.NOT_APPLICABLE);
        }

        Map<Integer, KnowledgeChunk> chunkByIndex = mapChunkByIndex(chunks);
        List<KnowledgeChunkingService.SymbolDraft> symbolDrafts = draft == null ? List.of() : draft.symbols();
        Map<String, AiCodeSymbol> uniqueSymbolsToSave = new LinkedHashMap<>();
        Instant now = Instant.now();

        for (KnowledgeChunkingService.SymbolDraft symbolDraft : symbolDrafts) {
            if (!StringUtils.hasText(symbolDraft.symbolName())) {
                continue;
            }
            AiCodeSymbol symbol = new AiCodeSymbol();
            symbol.setKnowledgeBase(knowledgeBase);
            symbol.setDocument(document);
            symbol.setChunk(chunkByIndex.get(symbolDraft.chunkIndex()));
            symbol.setProjectId(document.getProjectId());
            symbol.setRepositoryId(document.getRepositoryId());
            symbol.setBranchId(document.getBranchId());
            symbol.setCommitId(document.getCommitId());
            symbol.setProjectFileId(document.getProjectFileId());
            symbol.setProjectFileVersionId(document.getProjectFileVersionId());
            symbol.setLanguage(resolveSymbolLanguage(language, symbolDraft.language()));
            symbol.setFilePath(resolveFilePath(document, symbolDraft.filePath()));
            symbol.setSymbolName(symbolDraft.symbolName());
            symbol.setQualifiedName(resolveQualifiedName(symbolDraft, symbol.getFilePath()));
            symbol.setSymbolKind(normalizeKind(symbolDraft.symbolKind(), "SYMBOL"));
            symbol.setSymbolScope(normalizeKind(symbolDraft.symbolScope(), "MODULE"));
            symbol.setSignature(trimToNull(symbolDraft.signature()));
            symbol.setReturnType(trimToNull(symbolDraft.returnType()));
            symbol.setVisibility(trimToNull(symbolDraft.visibility()));
            symbol.setModifiersJson(null);
            symbol.setStartLine(defaultLine(symbolDraft.startLine()));
            symbol.setStartColumn(null);
            symbol.setEndLine(symbolDraft.endLine());
            symbol.setEndColumn(null);
            symbol.setBodyStartLine(symbolDraft.bodyStartLine());
            symbol.setBodyEndLine(symbolDraft.bodyEndLine());
            symbol.setIsDeclaration(symbolDraft.declaration() == null || Boolean.TRUE.equals(symbolDraft.declaration()));
            symbol.setIsExported(Boolean.TRUE.equals(symbolDraft.exported()));
            symbol.setDocComment(null);
            symbol.setSymbolKey(resolveSymbolKey(symbolDraft, symbol));
            symbol.setMetadataJson(enrichMetadata(symbolDraft.metadataJson(), "symbol", language));
            symbol.setStatus(AiCodeSymbol.Status.ACTIVE);
            symbol.setCreatedAt(now);
            symbol.setUpdatedAt(now);
            if (!StringUtils.hasText(symbol.getSymbolKey())) {
                continue;
            }
            uniqueSymbolsToSave.putIfAbsent(symbol.getSymbolKey(), symbol);
        }

        List<AiCodeSymbol> symbolsToSave = new ArrayList<>(uniqueSymbolsToSave.values());
        List<AiCodeSymbol> savedSymbols = symbolsToSave.isEmpty() ? List.of() : aiCodeSymbolRepository.saveAll(symbolsToSave);
        Map<String, AiCodeSymbol> symbolByKey = new LinkedHashMap<>();
        Map<String, AiCodeSymbol> symbolByQualifiedName = new LinkedHashMap<>();
        Map<String, AiCodeSymbol> symbolByName = new LinkedHashMap<>();
        for (AiCodeSymbol symbol : savedSymbols) {
            if (StringUtils.hasText(symbol.getSymbolKey())) {
                symbolByKey.put(symbol.getSymbolKey(), symbol);
            }
            if (StringUtils.hasText(symbol.getQualifiedName())) {
                symbolByQualifiedName.put(symbol.getQualifiedName(), symbol);
            }
            if (StringUtils.hasText(symbol.getSymbolName())) {
                symbolByName.putIfAbsent(symbol.getSymbolName(), symbol);
            }
        }

        List<KnowledgeChunkingService.ReferenceDraft> referenceDrafts = draft == null ? List.of() : draft.references();
        Map<String, AiCodeReference> uniqueReferencesToSave = new LinkedHashMap<>();
        for (KnowledgeChunkingService.ReferenceDraft referenceDraft : referenceDrafts) {
            if (!StringUtils.hasText(referenceDraft.refKind())) {
                continue;
            }
            AiCodeReference reference = new AiCodeReference();
            reference.setKnowledgeBase(knowledgeBase);
            reference.setProjectId(document.getProjectId());
            reference.setRepositoryId(document.getRepositoryId());
            reference.setBranchId(document.getBranchId());
            reference.setCommitId(document.getCommitId());
            reference.setFromDocument(document);
            reference.setFromChunk(chunkByIndex.get(referenceDraft.fromChunkIndex()));
            reference.setFromFilePath(resolveFilePath(document, referenceDraft.fromFilePath()));
            reference.setRefKind(normalizeKind(referenceDraft.refKind(), "REFERENCE"));
            reference.setRefName(trimToNull(referenceDraft.refName()));
            reference.setTargetQualifiedName(trimToNull(referenceDraft.targetQualifiedName()));
            reference.setStartLine(referenceDraft.startLine());
            reference.setStartColumn(null);
            reference.setEndLine(referenceDraft.endLine());
            reference.setEndColumn(null);
            reference.setReferenceKey(resolveReferenceKey(referenceDraft, reference));
            reference.setMetadataJson(enrichMetadata(referenceDraft.metadataJson(), "reference", language));
            reference.setStatus(AiCodeReference.Status.ACTIVE);
            reference.setCreatedAt(now);
            reference.setUpdatedAt(now);
            if (!StringUtils.hasText(reference.getReferenceKey())) {
                continue;
            }

            AiCodeSymbol fromSymbol = StringUtils.hasText(referenceDraft.fromSymbolKey())
                    ? symbolByKey.get(normalizeSymbolKey(referenceDraft.fromSymbolKey(), document.getId()))
                    : null;
            reference.setFromSymbol(fromSymbol);

            AiCodeSymbol toSymbol = resolveToSymbol(referenceDraft.targetQualifiedName(), symbolByQualifiedName, symbolByName);
            reference.setToSymbol(toSymbol);
            if (toSymbol != null) {
                reference.setToDocument(toSymbol.getDocument());
                reference.setToChunk(toSymbol.getChunk());
                reference.setResolutionStatus(AiCodeReference.ResolutionStatus.RESOLVED);
            } else {
                reference.setResolutionStatus(resolveResolutionStatus(referenceDraft.resolutionStatus(), reference.getRefKind()));
            }
            uniqueReferencesToSave.putIfAbsent(reference.getReferenceKey(), reference);
        }

        List<AiCodeReference> referencesToSave = new ArrayList<>(uniqueReferencesToSave.values());
        List<AiCodeReference> savedReferences = referencesToSave.isEmpty() ? List.of() : aiCodeReferenceRepository.saveAll(referencesToSave);
        return new CodeIndexResult(savedSymbols.size(), savedReferences.size(), KnowledgeDocument.SymbolIndexStatus.INDEXED);
    }

    private String resolveLanguage(KnowledgeDocument document, KnowledgeChunkingService.IndexBuildResult draft) {
        if (document != null && StringUtils.hasText(document.getLanguage())) {
            return normalizeLanguage(document.getLanguage());
        }
        if (draft != null && draft.chunks() != null && !draft.chunks().isEmpty()) {
            String chunkLanguage = draft.chunks().get(0).language();
            if (StringUtils.hasText(chunkLanguage)) {
                return normalizeLanguage(chunkLanguage);
            }
        }
        String path = resolveFilePath(document, null);
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".java")) return "java";
        if (lower.endsWith(".js")) return "javascript";
        if (lower.endsWith(".jsx")) return "javascript";
        if (lower.endsWith(".ts")) return "typescript";
        if (lower.endsWith(".tsx")) return "typescript";
        if (lower.endsWith(".vue")) return "vue";
        if (lower.endsWith(".sql")) return "sql";
        return "text";
    }

    private String resolveSymbolLanguage(String fallbackLanguage, String draftLanguage) {
        if (StringUtils.hasText(draftLanguage)) {
            return normalizeLanguage(draftLanguage);
        }
        return normalizeLanguage(fallbackLanguage);
    }

    private boolean isSupported(String language) {
        return "java".equals(language)
                || "javascript".equals(language)
                || "typescript".equals(language)
                || "vue".equals(language)
                || "sql".equals(language);
    }

    private Map<Integer, KnowledgeChunk> mapChunkByIndex(List<KnowledgeChunk> chunks) {
        Map<Integer, KnowledgeChunk> mapping = new LinkedHashMap<>();
        if (chunks == null) {
            return mapping;
        }
        for (KnowledgeChunk chunk : chunks) {
            if (chunk != null && chunk.getChunkIndex() != null) {
                mapping.put(chunk.getChunkIndex(), chunk);
            }
        }
        return mapping;
    }

    private AiCodeSymbol resolveToSymbol(String targetQualifiedName,
                                         Map<String, AiCodeSymbol> symbolByQualifiedName,
                                         Map<String, AiCodeSymbol> symbolByName) {
        if (!StringUtils.hasText(targetQualifiedName)) {
            return null;
        }
        AiCodeSymbol byQualified = symbolByQualifiedName.get(targetQualifiedName);
        if (byQualified != null) {
            return byQualified;
        }
        return symbolByName.get(targetQualifiedName);
    }

    private AiCodeReference.ResolutionStatus resolveResolutionStatus(String rawStatus, String refKind) {
        Optional<AiCodeReference.ResolutionStatus> parsed = parseResolutionStatus(rawStatus);
        if (parsed.isPresent()) {
            return parsed.get();
        }
        if ("IMPORT".equalsIgnoreCase(refKind) || "API".equalsIgnoreCase(refKind) || "ROUTE".equalsIgnoreCase(refKind)) {
            return AiCodeReference.ResolutionStatus.EXTERNAL;
        }
        return AiCodeReference.ResolutionStatus.UNRESOLVED;
    }

    private Optional<AiCodeReference.ResolutionStatus> parseResolutionStatus(String rawStatus) {
        if (!StringUtils.hasText(rawStatus)) {
            return Optional.empty();
        }
        try {
            return Optional.of(AiCodeReference.ResolutionStatus.valueOf(rawStatus.trim().toUpperCase(Locale.ROOT)));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String resolveQualifiedName(KnowledgeChunkingService.SymbolDraft draft, String filePath) {
        if (StringUtils.hasText(draft.qualifiedName())) {
            return draft.qualifiedName();
        }
        return filePath + "#" + draft.symbolName();
    }

    private String resolveSymbolKey(KnowledgeChunkingService.SymbolDraft draft, AiCodeSymbol symbol) {
        if (StringUtils.hasText(draft.symbolKey())) {
            return normalizeSymbolKey(draft.symbolKey(), symbol.getDocumentId());
        }
        return normalizeSymbolKey(
                symbol.getFilePath() + ":" +
                symbol.getSymbolKind() + ":" +
                symbol.getSymbolName() + ":" +
                symbol.getStartLine(),
                symbol.getDocumentId()
        );
    }

    private String normalizeSymbolKey(String rawKey, Long documentId) {
        String trimmed = trimToNull(rawKey);
        if (trimmed == null) {
            return null;
        }
        String prefix = safe(documentId) + ":";
        if (trimmed.startsWith(prefix)) {
            return trimmed;
        }
        return prefix + trimmed;
    }

    private String resolveReferenceKey(KnowledgeChunkingService.ReferenceDraft draft, AiCodeReference reference) {
        if (StringUtils.hasText(draft.referenceKey())) {
            return normalizeReferenceKey(draft.referenceKey(), reference.getFromDocumentId());
        }
        return normalizeReferenceKey(
                reference.getFromFilePath() + ":" + reference.getRefKind() + ":" + safe(reference.getRefName()) + ":" + safe(reference.getStartLine()),
                reference.getFromDocumentId()
        );
    }

    private String normalizeReferenceKey(String rawKey, Long documentId) {
        String trimmed = trimToNull(rawKey);
        if (trimmed == null) {
            return null;
        }
        String prefix = safe(documentId) + ":";
        if (trimmed.startsWith(prefix)) {
            return trimmed;
        }
        return prefix + trimmed;
    }

    private String enrichMetadata(String metadataJson, String entryType, String language) {
        String base = trimToNull(metadataJson);
        if (base == null || "{}".equals(base)) {
            return "{\"entryType\":\"" + entryType + "\",\"language\":\"" + language + "\"}";
        }
        return base;
    }

    private String resolveFilePath(KnowledgeDocument document, String fallbackPath) {
        if (StringUtils.hasText(fallbackPath)) {
            return fallbackPath;
        }
        if (document == null) {
            return "";
        }
        if (StringUtils.hasText(document.getFilePath())) {
            return document.getFilePath();
        }
        if (StringUtils.hasText(document.getArchiveEntryPath())) {
            return document.getArchiveEntryPath();
        }
        if (StringUtils.hasText(document.getSourceUrl())) {
            return document.getSourceUrl();
        }
        if (StringUtils.hasText(document.getFileName())) {
            return document.getFileName();
        }
        return "";
    }

    private int defaultLine(Integer line) {
        return line == null || line < 1 ? 1 : line;
    }

    private String normalizeKind(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeLanguage(String value) {
        if (!StringUtils.hasText(value)) {
            return "text";
        }
        String lower = value.trim().toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "js", "jsx" -> "javascript";
            case "ts", "tsx" -> "typescript";
            default -> lower;
        };
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
