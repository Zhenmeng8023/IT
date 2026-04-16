package com.alikeyou.itmoduleproject.support.diff;

import com.alikeyou.itmoduleproject.entity.ProjectSnapshotItem;
import com.alikeyou.itmoduleproject.support.ProjectPathUtils;
import com.alikeyou.itmoduleproject.support.ProjectSnapshotDiffSupport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

public final class ProjectMergeDiffSupport {

    private ProjectMergeDiffSupport() {
    }

    public static MergeCheckResult buildMergeCheck(Long mergeRequestId,
                                                   Long repositoryId,
                                                   Long sourceBranchId,
                                                   Long targetBranchId,
                                                   Long baseCommitId,
                                                   Long sourceCommitId,
                                                   Long targetCommitId,
                                                   Map<String, ProjectSnapshotItem> baseSnapshot,
                                                   Map<String, ProjectSnapshotItem> sourceSnapshot,
                                                   Map<String, ProjectSnapshotItem> targetSnapshot) {
        return buildMergeCheck(mergeRequestId,
                repositoryId,
                sourceBranchId,
                targetBranchId,
                baseCommitId,
                sourceCommitId,
                targetCommitId,
                baseSnapshot,
                sourceSnapshot,
                targetSnapshot,
                null);
    }

    public static MergeCheckResult buildMergeCheck(Long mergeRequestId,
                                                   Long repositoryId,
                                                   Long sourceBranchId,
                                                   Long targetBranchId,
                                                   Long baseCommitId,
                                                   Long sourceCommitId,
                                                   Long targetCommitId,
                                                   Map<String, ProjectSnapshotItem> baseSnapshot,
                                                   Map<String, ProjectSnapshotItem> sourceSnapshot,
                                                   Map<String, ProjectSnapshotItem> targetSnapshot,
                                                   Function<Long, Boolean> binaryResolver) {
        Map<String, ProjectSnapshotItem> safeBase = baseSnapshot == null ? Map.of() : baseSnapshot;
        Map<String, ProjectSnapshotItem> safeSource = sourceSnapshot == null ? Map.of() : sourceSnapshot;
        Map<String, ProjectSnapshotItem> safeTarget = targetSnapshot == null ? Map.of() : targetSnapshot;

        List<ChangeEntry> sourceChanges = ProjectSnapshotDiffSupport.buildChangeEntries(
                safeBase,
                safeSource,
                baseCommitId,
                sourceCommitId,
                targetCommitId,
                binaryResolver
        );
        List<ChangeEntry> targetChanges = ProjectSnapshotDiffSupport.buildChangeEntries(
                safeBase,
                safeTarget,
                baseCommitId,
                sourceCommitId,
                targetCommitId,
                binaryResolver
        );

        Map<String, ChangeEntry> sourceChangesByOldPath = indexByOldPath(sourceChanges);
        Map<String, ChangeEntry> sourceChangesByNewPath = indexByNewPath(sourceChanges);
        Map<String, ChangeEntry> targetChangesByOldPath = indexByOldPath(targetChanges);
        Map<String, ChangeEntry> targetChangesByNewPath = indexByNewPath(targetChanges);

        Map<String, ProjectSnapshotItem> acceptedBefore = new LinkedHashMap<>();
        Map<String, ProjectSnapshotItem> acceptedAfter = new LinkedHashMap<>();
        List<ConflictDetail> conflicts = new ArrayList<>();
        if (baseCommitId == null) {
            conflicts.add(ConflictDetail.builder()
                    .conflictId(buildConflictId(ConflictType.MISSING_BASE, null, null, null))
                    .conflictType(ConflictType.MISSING_BASE)
                    .path(null)
                    .binaryFile(false)
                    .baseCommitId(null)
                    .sourceCommitId(sourceCommitId)
                    .targetCommitId(targetCommitId)
                    .summary("No common merge base was found.")
                    .suggestedAction(suggestedAction(ConflictType.MISSING_BASE))
                    .severity(severity(ConflictType.MISSING_BASE))
                    .metadata(buildNonFileConflictMetadata(ConflictType.MISSING_BASE, false, false))
                    .build());
        }

        Set<String> relocationConflictPaths = collectRelocationConflictPaths(
                sourceChanges,
                targetChanges,
                conflicts,
                baseCommitId,
                sourceCommitId,
                targetCommitId
        );

        Set<String> paths = new TreeSet<>();
        paths.addAll(safeBase.keySet());
        paths.addAll(safeSource.keySet());
        paths.addAll(safeTarget.keySet());

        for (String path : paths) {
            if (relocationConflictPaths.contains(path)) {
                continue;
            }
            ProjectSnapshotItem baseItem = safeBase.get(path);
            ProjectSnapshotItem sourceItem = safeSource.get(path);
            ProjectSnapshotItem targetItem = safeTarget.get(path);

            if (sameSnapshotItem(sourceItem, targetItem)) {
                continue;
            }
            if (sameSnapshotItem(baseItem, targetItem)) {
                if (targetItem != null) {
                    acceptedBefore.put(path, targetItem);
                }
                if (sourceItem != null) {
                    acceptedAfter.put(path, sourceItem);
                }
                continue;
            }
            if (sameSnapshotItem(baseItem, sourceItem)) {
                continue;
            }

            ChangeEntry sourceChange = resolvePathChange(path, sourceChangesByOldPath, sourceChangesByNewPath);
            ChangeEntry targetChange = resolvePathChange(path, targetChangesByOldPath, targetChangesByNewPath);
            conflicts.add(buildConflictDetail(
                    path,
                    baseItem,
                    sourceItem,
                    targetItem,
                    sourceChange,
                    targetChange,
                    baseCommitId,
                    sourceCommitId,
                    targetCommitId
            ));
        }

        List<ChangeEntry> changes = ProjectSnapshotDiffSupport.buildChangeEntries(
                acceptedBefore,
                acceptedAfter,
                baseCommitId,
                sourceCommitId,
                targetCommitId,
                binaryResolver
        );

        boolean mergeable = conflicts.isEmpty();
        List<String> blockingReasons = mergeable ? List.of() : List.of("UNRESOLVED_CONFLICTS");
        return MergeCheckResult.builder()
                .mergeRequestId(mergeRequestId)
                .repositoryId(repositoryId)
                .sourceBranchId(sourceBranchId)
                .targetBranchId(targetBranchId)
                .baseCommitId(baseCommitId)
                .sourceCommitId(sourceCommitId)
                .targetCommitId(targetCommitId)
                .mergeable(mergeable)
                .totalConflicts(conflicts.size())
                .requiresBranchUpdate(false)
                .requiresRecheck(false)
                .changes(changes)
                .conflicts(conflicts)
                .blockingReasons(blockingReasons)
                .summary(buildSummary(mergeable, changes.size(), conflicts.size()))
                .suggestedAction(mergeable ? "Merge can proceed." : "Resolve conflicts and re-run merge check.")
                .severity(mergeable ? "INFO" : "ERROR")
                .metadata(buildMetadata(safeBase.size(), safeSource.size(), safeTarget.size(), sourceChanges.size(), targetChanges.size()))
                .build();
    }

    private static Set<String> collectRelocationConflictPaths(List<ChangeEntry> sourceChanges,
                                                              List<ChangeEntry> targetChanges,
                                                              List<ConflictDetail> conflicts,
                                                              Long baseCommitId,
                                                              Long sourceCommitId,
                                                              Long targetCommitId) {
        Map<String, ChangeEntry> sourceRelocations = relocationByOldPath(sourceChanges);
        Map<String, ChangeEntry> targetRelocations = relocationByOldPath(targetChanges);
        Set<String> conflictPaths = new LinkedHashSet<>();
        for (Map.Entry<String, ChangeEntry> entry : sourceRelocations.entrySet()) {
            String oldPath = entry.getKey();
            ChangeEntry sourceRelocation = entry.getValue();
            ChangeEntry targetRelocation = targetRelocations.get(oldPath);
            if (targetRelocation == null) {
                continue;
            }
            if (Objects.equals(normalizePath(sourceRelocation.getNewPath()), normalizePath(targetRelocation.getNewPath()))) {
                continue;
            }
            ConflictType conflictType = resolveRelocationConflictType(sourceRelocation, targetRelocation);
            String sourcePath = normalizePath(sourceRelocation.getNewPath());
            String targetPath = normalizePath(targetRelocation.getNewPath());
            String displayPath = sourcePath == null ? targetPath : sourcePath;
            conflicts.add(ConflictDetail.builder()
                    .conflictId(buildConflictId(conflictType, oldPath, sourcePath, targetPath))
                    .conflictType(conflictType)
                    .sourceChangeType(sourceRelocation.getChangeType())
                    .targetChangeType(targetRelocation.getChangeType())
                    .path(displayPath)
                    .oldPath(oldPath)
                    .newPath(displayPath)
                    .fileName(displayPath == null ? null : ProjectPathUtils.extractFileName(displayPath))
                    .basePath(oldPath)
                    .sourcePath(sourcePath)
                    .targetPath(targetPath)
                    .binaryFile(resolveConflictBinaryFile(sourceRelocation, targetRelocation))
                    .baseCommitId(baseCommitId)
                    .sourceCommitId(sourceCommitId)
                    .targetCommitId(targetCommitId)
                    .contentHashBefore(sourceRelocation.getContentHashBefore())
                    .contentHashAfter(sourceRelocation.getContentHashAfter())
                    .baseContentHash(sourceRelocation.getContentHashBefore())
                    .sourceContentHash(sourceRelocation.getContentHashAfter())
                    .targetContentHash(targetRelocation.getContentHashAfter())
                    .summary(buildRelocationSummary(conflictType, oldPath, sourcePath, targetPath))
                    .suggestedAction(suggestedAction(conflictType))
                    .severity(severity(conflictType))
                    .relatedChanges(List.of(sourceRelocation, targetRelocation))
                    .metadata(buildConflictMetadata(conflictType, oldPath, sourcePath, targetPath, sourceRelocation, targetRelocation))
                    .build());
            if (oldPath != null) {
                conflictPaths.add(oldPath);
            }
            if (sourcePath != null) {
                conflictPaths.add(sourcePath);
            }
            if (targetPath != null) {
                conflictPaths.add(targetPath);
            }
        }
        return conflictPaths;
    }

    private static ConflictDetail buildConflictDetail(String path,
                                                      ProjectSnapshotItem baseItem,
                                                      ProjectSnapshotItem sourceItem,
                                                      ProjectSnapshotItem targetItem,
                                                      ChangeEntry sourceChange,
                                                      ChangeEntry targetChange,
                                                      Long baseCommitId,
                                                      Long sourceCommitId,
                                                      Long targetCommitId) {
        ConflictType conflictType = resolveConflictType(path, baseItem, sourceItem, targetItem, sourceChange, targetChange);
        String sourcePath = resolveChangedPath(path, sourceItem, sourceChange);
        String targetPath = resolveChangedPath(path, targetItem, targetChange);
        String displayPath = sourcePath != null ? sourcePath : (targetPath != null ? targetPath : path);
        ChangeType sourceChangeType = resolveChangeType(baseItem, sourceItem, sourceChange);
        ChangeType targetChangeType = resolveChangeType(baseItem, targetItem, targetChange);
        return ConflictDetail.builder()
                .conflictId(buildConflictId(conflictType, path, sourcePath, targetPath))
                .conflictType(conflictType)
                .sourceChangeType(sourceChangeType)
                .targetChangeType(targetChangeType)
                .path(displayPath)
                .oldPath(path)
                .newPath(displayPath)
                .fileName(displayPath == null ? null : ProjectPathUtils.extractFileName(displayPath))
                .basePath(baseItem == null ? null : path)
                .sourcePath(sourcePath)
                .targetPath(targetPath)
                .binaryFile(resolveConflictBinaryFile(sourceChange, targetChange))
                .baseCommitId(baseCommitId)
                .sourceCommitId(sourceCommitId)
                .targetCommitId(targetCommitId)
                .contentHashBefore(baseItem == null ? null : baseItem.getContentHash())
                .contentHashAfter(sourceItem == null ? null : sourceItem.getContentHash())
                .baseContentHash(baseItem == null ? null : baseItem.getContentHash())
                .sourceContentHash(sourceItem == null ? null : sourceItem.getContentHash())
                .targetContentHash(targetItem == null ? null : targetItem.getContentHash())
                .summary(buildSummary(conflictType, path, sourcePath, targetPath))
                .suggestedAction(suggestedAction(conflictType))
                .severity(severity(conflictType))
                .relatedChanges(compactRelatedChanges(sourceChange, targetChange))
                .metadata(buildConflictMetadata(conflictType, baseItem == null ? null : path, sourcePath, targetPath, sourceChange, targetChange))
                .build();
    }

    private static Map<String, ChangeEntry> indexByOldPath(List<ChangeEntry> changes) {
        Map<String, ChangeEntry> index = new LinkedHashMap<>();
        if (changes == null) {
            return index;
        }
        for (ChangeEntry change : changes) {
            if (change == null || change.getOldPath() == null) {
                continue;
            }
            index.putIfAbsent(change.getOldPath(), change);
        }
        return index;
    }

    private static Map<String, ChangeEntry> indexByNewPath(List<ChangeEntry> changes) {
        Map<String, ChangeEntry> index = new LinkedHashMap<>();
        if (changes == null) {
            return index;
        }
        for (ChangeEntry change : changes) {
            if (change == null || change.getNewPath() == null) {
                continue;
            }
            index.putIfAbsent(change.getNewPath(), change);
        }
        return index;
    }

    private static Map<String, ChangeEntry> relocationByOldPath(List<ChangeEntry> changes) {
        Map<String, ChangeEntry> relocation = new LinkedHashMap<>();
        if (changes == null) {
            return relocation;
        }
        for (ChangeEntry change : changes) {
            if (change == null || !isRelocationChange(change.getChangeType()) || change.getOldPath() == null) {
                continue;
            }
            relocation.putIfAbsent(change.getOldPath(), change);
        }
        return relocation;
    }

    private static ChangeEntry resolvePathChange(String path,
                                                 Map<String, ChangeEntry> byOldPath,
                                                 Map<String, ChangeEntry> byNewPath) {
        ChangeEntry oldMatched = byOldPath.get(path);
        if (oldMatched != null) {
            return oldMatched;
        }
        return byNewPath.get(path);
    }

    private static ChangeType resolveChangeType(ProjectSnapshotItem before,
                                                ProjectSnapshotItem after,
                                                ChangeEntry change) {
        if (change != null && change.getChangeType() != null) {
            return change.getChangeType();
        }
        if (before == null && after == null) {
            return null;
        }
        if (before == null) {
            return ChangeType.ADD;
        }
        if (after == null) {
            return ChangeType.DELETE;
        }
        return ChangeType.MODIFY;
    }

    private static String resolveChangedPath(String path,
                                             ProjectSnapshotItem item,
                                             ChangeEntry change) {
        if (item != null) {
            return path;
        }
        if (change == null) {
            return null;
        }
        if (Objects.equals(path, normalizePath(change.getOldPath()))) {
            return normalizePath(change.getNewPath());
        }
        if (Objects.equals(path, normalizePath(change.getNewPath()))) {
            return normalizePath(change.getNewPath());
        }
        return null;
    }

    private static ConflictType resolveConflictType(String path,
                                                    ProjectSnapshotItem baseItem,
                                                    ProjectSnapshotItem sourceItem,
                                                    ProjectSnapshotItem targetItem,
                                                    ChangeEntry sourceChange,
                                                    ChangeEntry targetChange) {
        boolean sourceRelocation = isRelocationFromPath(path, sourceChange);
        boolean targetRelocation = isRelocationFromPath(path, targetChange);
        if (sourceRelocation || targetRelocation) {
            return resolveRelocationConflictType(sourceChange, targetChange);
        }
        if (baseItem == null && sourceItem != null && targetItem != null) {
            return ConflictType.TARGET_PATH_OCCUPIED;
        }
        if ((sourceItem == null && targetItem != null) || (sourceItem != null && targetItem == null)) {
            return ConflictType.DELETE_MODIFY_CONFLICT;
        }
        return ConflictType.CONTENT_CONFLICT;
    }

    private static boolean isRelocationFromPath(String path, ChangeEntry change) {
        if (change == null || !isRelocationChange(change.getChangeType())) {
            return false;
        }
        return Objects.equals(normalizePath(change.getOldPath()), normalizePath(path));
    }

    private static boolean isRelocationChange(ChangeType changeType) {
        return ChangeType.RENAME.equals(changeType) || ChangeType.MOVE.equals(changeType);
    }

    private static ConflictType resolveRelocationConflictType(ChangeEntry sourceChange, ChangeEntry targetChange) {
        if (ChangeType.MOVE.equals(sourceChange == null ? null : sourceChange.getChangeType())
                || ChangeType.MOVE.equals(targetChange == null ? null : targetChange.getChangeType())) {
            return ConflictType.MOVE_CONFLICT;
        }
        return ConflictType.RENAME_CONFLICT;
    }

    private static List<ChangeEntry> compactRelatedChanges(ChangeEntry sourceChange, ChangeEntry targetChange) {
        if (sourceChange == null && targetChange == null) {
            return List.of();
        }
        if (sourceChange == null) {
            return List.of(targetChange);
        }
        if (targetChange == null || sourceChange == targetChange) {
            return List.of(sourceChange);
        }
        return List.of(sourceChange, targetChange);
    }

    private static Map<String, Object> buildConflictMetadata(ConflictType conflictType,
                                                             String basePath,
                                                             String sourcePath,
                                                             String targetPath,
                                                             ChangeEntry sourceChange,
                                                             ChangeEntry targetChange) {
        String displayPath = resolveDisplayPath(basePath, sourcePath, targetPath);
        Boolean binaryFile = resolveConflictBinaryFile(sourceChange, targetChange);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("path", displayPath);
        metadata.put("basePath", basePath);
        metadata.put("sourcePath", sourcePath);
        metadata.put("targetPath", targetPath);
        metadata.put("fileName", displayPath == null ? null : ProjectPathUtils.extractFileName(displayPath));
        metadata.put("binaryFile", binaryFile);
        metadata.put("hasPathChange", hasPathChange(basePath, sourcePath, targetPath));
        metadata.put("sourceChangeType", sourceChange == null || sourceChange.getChangeType() == null ? null : sourceChange.getChangeType().name());
        metadata.put("targetChangeType", targetChange == null || targetChange.getChangeType() == null ? null : targetChange.getChangeType().name());
        metadata.put("baseContentHash", sourceChange == null ? null : sourceChange.getContentHashBefore());
        metadata.put("sourceContentHash", sourceChange == null ? null : sourceChange.getContentHashAfter());
        metadata.put("targetContentHash", targetChange == null ? null : targetChange.getContentHashAfter());
        metadata.put("fileExtension", resolveFileExtension(displayPath));
        metadata.put("language", inferEditorLanguage(displayPath));
        metadata.put("onlineEditable", ConflictType.CONTENT_CONFLICT.equals(conflictType) && !Boolean.TRUE.equals(binaryFile));
        metadata.put("allowOnlineEdit", ConflictType.CONTENT_CONFLICT.equals(conflictType) && !Boolean.TRUE.equals(binaryFile));
        metadata.put("readOnly", !ConflictType.CONTENT_CONFLICT.equals(conflictType) || Boolean.TRUE.equals(binaryFile));
        metadata.put("resolutionStrategies", resolutionStrategies(conflictType));
        return metadata;
    }

    private static Map<String, Object> buildNonFileConflictMetadata(ConflictType conflictType,
                                                                    boolean requiresBranchUpdate,
                                                                    boolean requiresRecheck) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("nonFileConflict", true);
        metadata.put("binaryFile", false);
        metadata.put("requiresBranchUpdate", requiresBranchUpdate);
        metadata.put("requiresRecheck", requiresRecheck);
        metadata.put("onlineEditable", false);
        metadata.put("allowOnlineEdit", false);
        metadata.put("readOnly", true);
        metadata.put("resolutionStrategies", resolutionStrategies(conflictType));
        return metadata;
    }

    private static Boolean resolveConflictBinaryFile(ChangeEntry sourceChange, ChangeEntry targetChange) {
        if (Boolean.TRUE.equals(sourceChange == null ? null : sourceChange.getBinaryFile())
                || Boolean.TRUE.equals(targetChange == null ? null : targetChange.getBinaryFile())) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private static boolean hasPathChange(String basePath, String sourcePath, String targetPath) {
        String base = normalizePath(basePath);
        String source = normalizePath(sourcePath);
        String target = normalizePath(targetPath);
        return (base != null && source != null && !Objects.equals(base, source))
                || (base != null && target != null && !Objects.equals(base, target))
                || (source != null && target != null && !Objects.equals(source, target));
    }

    private static String resolveDisplayPath(String basePath, String sourcePath, String targetPath) {
        if (sourcePath != null) {
            return sourcePath;
        }
        if (targetPath != null) {
            return targetPath;
        }
        return basePath;
    }

    private static String resolveFileExtension(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String fileName = ProjectPathUtils.extractFileName(path);
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(index + 1).toLowerCase(java.util.Locale.ROOT);
    }

    private static String inferEditorLanguage(String path) {
        String extension = resolveFileExtension(path);
        if (extension == null) {
            return "plaintext";
        }
        return switch (extension) {
            case "java" -> "java";
            case "js", "mjs", "cjs" -> "javascript";
            case "ts", "tsx" -> "typescript";
            case "vue" -> "vue";
            case "json" -> "json";
            case "xml", "pom" -> "xml";
            case "html", "htm" -> "html";
            case "css" -> "css";
            case "scss" -> "scss";
            case "md", "markdown" -> "markdown";
            case "yml", "yaml" -> "yaml";
            case "sql" -> "sql";
            case "py" -> "python";
            case "go" -> "go";
            case "kt", "kts" -> "kotlin";
            case "sh", "bash" -> "shell";
            case "bat", "cmd" -> "bat";
            case "ps1" -> "powershell";
            default -> "plaintext";
        };
    }

    private static List<String> resolutionStrategies(ConflictType type) {
        if (type == null) {
            return List.of();
        }
        return switch (type) {
            case CONTENT_CONFLICT -> List.of("MANUAL_CONTENT");
            case DELETE_MODIFY_CONFLICT -> List.of("KEEP_SOURCE", "KEEP_TARGET");
            case RENAME_CONFLICT, MOVE_CONFLICT -> List.of("USE_SOURCE_PATH", "USE_TARGET_PATH", "SET_TARGET_PATH");
            case TARGET_PATH_OCCUPIED -> List.of("KEEP_SOURCE", "KEEP_TARGET", "SET_TARGET_PATH");
            case STALE_BRANCH -> List.of("SYNC_SOURCE_WITH_TARGET");
            case MISSING_BASE -> List.of();
        };
    }

    private static String buildConflictId(ConflictType type, String basePath, String sourcePath, String targetPath) {
        StringBuilder builder = new StringBuilder();
        builder.append(type == null ? "UNKNOWN" : type.name());
        builder.append('|').append(basePath == null ? "-" : basePath);
        builder.append('|').append(sourcePath == null ? "-" : sourcePath);
        builder.append('|').append(targetPath == null ? "-" : targetPath);
        return Integer.toHexString(builder.toString().hashCode());
    }

    private static String buildSummary(ConflictType type, String basePath, String sourcePath, String targetPath) {
        return switch (type) {
            case DELETE_MODIFY_CONFLICT -> "Delete/modify conflict at " + safePath(basePath, sourcePath, targetPath) + ".";
            case RENAME_CONFLICT -> "Rename conflict at " + safePath(basePath, sourcePath, targetPath) + ".";
            case MOVE_CONFLICT -> "Move conflict at " + safePath(basePath, sourcePath, targetPath) + ".";
            case TARGET_PATH_OCCUPIED -> "Target path occupied at " + safePath(basePath, sourcePath, targetPath) + ".";
            case MISSING_BASE -> "No common merge base was found.";
            case STALE_BRANCH -> "Branch is stale and must be updated.";
            default -> "Content conflict at " + safePath(basePath, sourcePath, targetPath) + ".";
        };
    }

    private static String buildRelocationSummary(ConflictType type, String oldPath, String sourcePath, String targetPath) {
        String label = type == ConflictType.RENAME_CONFLICT ? "Rename conflict" : "Move conflict";
        return label + ": " + oldPath + " -> [" + safePath(sourcePath, null, null) + "] / [" + safePath(targetPath, null, null) + "].";
    }

    private static String suggestedAction(ConflictType type) {
        return switch (type) {
            case CONTENT_CONFLICT -> "Review both branch contents and commit a manual merge resolution.";
            case DELETE_MODIFY_CONFLICT -> "Choose whether to keep deletion or keep modifications, then re-run merge check.";
            case RENAME_CONFLICT -> "Unify to one rename target path and re-run merge check.";
            case MOVE_CONFLICT -> "Unify to one move destination path and re-run merge check.";
            case TARGET_PATH_OCCUPIED -> "Resolve path collision (rename/delete one side) and re-run merge check.";
            case MISSING_BASE -> "Rebase source branch on target branch (or recreate branch) and re-run merge check.";
            case STALE_BRANCH -> "Sync source branch with latest target branch and re-run merge check.";
        };
    }

    private static String severity(ConflictType type) {
        if (ConflictType.STALE_BRANCH.equals(type)) {
            return "WARNING";
        }
        return "ERROR";
    }

    private static String safePath(String first, String second, String third) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        if (third != null) {
            return third;
        }
        return "/";
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return path.trim();
    }

    private static boolean sameSnapshotItem(ProjectSnapshotItem left, ProjectSnapshotItem right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        if (Objects.equals(left.getBlobId(), right.getBlobId())) {
            return true;
        }
        return Objects.equals(left.getContentHash(), right.getContentHash());
    }

    private static String buildSummary(boolean mergeable, int changeCount, int conflictCount) {
        if (mergeable) {
            return "Merge check passed with " + changeCount + " accepted change(s).";
        }
        return "Merge check found " + conflictCount + " conflict(s).";
    }

    private static Map<String, Object> buildMetadata(int baseFileCount,
                                                     int sourceFileCount,
                                                     int targetFileCount,
                                                     int sourceChangeCount,
                                                     int targetChangeCount) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("baseFileCount", baseFileCount);
        metadata.put("sourceFileCount", sourceFileCount);
        metadata.put("targetFileCount", targetFileCount);
        metadata.put("sourceChangeCount", sourceChangeCount);
        metadata.put("targetChangeCount", targetChangeCount);
        return metadata;
    }
}
