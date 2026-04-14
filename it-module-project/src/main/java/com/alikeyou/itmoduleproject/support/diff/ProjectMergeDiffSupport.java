package com.alikeyou.itmoduleproject.support.diff;

import com.alikeyou.itmoduleproject.entity.ProjectSnapshotItem;
import com.alikeyou.itmoduleproject.support.ProjectPathUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

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
        Map<String, ProjectSnapshotItem> safeBase = baseSnapshot == null ? Map.of() : baseSnapshot;
        Map<String, ProjectSnapshotItem> safeSource = sourceSnapshot == null ? Map.of() : sourceSnapshot;
        Map<String, ProjectSnapshotItem> safeTarget = targetSnapshot == null ? Map.of() : targetSnapshot;

        List<ChangeEntry> changes = new ArrayList<>();
        List<ConflictDetail> conflicts = new ArrayList<>();
        if (baseCommitId == null) {
            conflicts.add(ConflictDetail.builder()
                    .conflictType(ConflictType.MISSING_BASE)
                    .baseCommitId(null)
                    .sourceCommitId(sourceCommitId)
                    .targetCommitId(targetCommitId)
                    .summary("No common merge base was found.")
                    .suggestedAction("Rebase the source branch or choose an explicit base commit before merging.")
                    .severity("ERROR")
                    .build());
        }

        Set<String> paths = new TreeSet<>();
        paths.addAll(safeBase.keySet());
        paths.addAll(safeSource.keySet());
        paths.addAll(safeTarget.keySet());

        for (String path : paths) {
            ProjectSnapshotItem baseItem = safeBase.get(path);
            ProjectSnapshotItem sourceItem = safeSource.get(path);
            ProjectSnapshotItem targetItem = safeTarget.get(path);

            if (sameSnapshotItem(sourceItem, targetItem)) {
                continue;
            }
            if (sameSnapshotItem(baseItem, targetItem)) {
                changes.add(buildChangeEntry(path, targetItem, sourceItem, baseCommitId, sourceCommitId, targetCommitId));
                continue;
            }
            if (sameSnapshotItem(baseItem, sourceItem)) {
                continue;
            }
            conflicts.add(buildConflictDetail(path, baseItem, sourceItem, targetItem, baseCommitId, sourceCommitId, targetCommitId));
        }

        boolean mergeable = conflicts.isEmpty();
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
                .changes(changes)
                .conflicts(conflicts)
                .summary(buildSummary(mergeable, changes.size(), conflicts.size()))
                .suggestedAction(mergeable ? "Merge can proceed." : "Resolve conflicts before merging.")
                .severity(mergeable ? "INFO" : "ERROR")
                .metadata(buildMetadata(safeBase.size(), safeSource.size(), safeTarget.size()))
                .build();
    }

    private static ChangeEntry buildChangeEntry(String path,
                                                ProjectSnapshotItem before,
                                                ProjectSnapshotItem after,
                                                Long baseCommitId,
                                                Long sourceCommitId,
                                                Long targetCommitId) {
        ChangeType changeType = resolveChangeType(before, after);
        return ChangeEntry.builder()
                .changeType(changeType)
                .oldPath(ChangeType.ADD.equals(changeType) ? null : path)
                .newPath(ChangeType.DELETE.equals(changeType) ? null : path)
                .fileName(ProjectPathUtils.extractFileName(path))
                .contentHashBefore(before == null ? null : before.getContentHash())
                .contentHashAfter(after == null ? null : after.getContentHash())
                .oldBlobId(before == null ? null : before.getBlobId())
                .newBlobId(after == null ? null : after.getBlobId())
                .baseCommitId(baseCommitId)
                .sourceCommitId(sourceCommitId)
                .targetCommitId(targetCommitId)
                .summary(changeType.name() + " " + path)
                .suggestedAction("Apply source branch change to target branch.")
                .severity("INFO")
                .build();
    }

    private static ConflictDetail buildConflictDetail(String path,
                                                      ProjectSnapshotItem baseItem,
                                                      ProjectSnapshotItem sourceItem,
                                                      ProjectSnapshotItem targetItem,
                                                      Long baseCommitId,
                                                      Long sourceCommitId,
                                                      Long targetCommitId) {
        ConflictType conflictType = resolveConflictType(baseItem, sourceItem, targetItem);
        return ConflictDetail.builder()
                .conflictType(conflictType)
                .sourceChangeType(resolveChangeType(baseItem, sourceItem))
                .targetChangeType(resolveChangeType(baseItem, targetItem))
                .oldPath(path)
                .newPath(path)
                .fileName(ProjectPathUtils.extractFileName(path))
                .basePath(baseItem == null ? null : path)
                .sourcePath(sourceItem == null ? null : path)
                .targetPath(targetItem == null ? null : path)
                .baseCommitId(baseCommitId)
                .sourceCommitId(sourceCommitId)
                .targetCommitId(targetCommitId)
                .contentHashBefore(baseItem == null ? null : baseItem.getContentHash())
                .contentHashAfter(sourceItem == null ? null : sourceItem.getContentHash())
                .baseContentHash(baseItem == null ? null : baseItem.getContentHash())
                .sourceContentHash(sourceItem == null ? null : sourceItem.getContentHash())
                .targetContentHash(targetItem == null ? null : targetItem.getContentHash())
                .summary(conflictType.name() + " at " + path)
                .suggestedAction("Choose source, choose target, or provide a manual resolution.")
                .severity("ERROR")
                .build();
    }

    private static ChangeType resolveChangeType(ProjectSnapshotItem before, ProjectSnapshotItem after) {
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

    private static ConflictType resolveConflictType(ProjectSnapshotItem baseItem,
                                                    ProjectSnapshotItem sourceItem,
                                                    ProjectSnapshotItem targetItem) {
        if (baseItem == null && sourceItem != null && targetItem != null) {
            return ConflictType.TARGET_PATH_OCCUPIED;
        }
        if ((sourceItem == null && targetItem != null) || (sourceItem != null && targetItem == null)) {
            return ConflictType.DELETE_MODIFY_CONFLICT;
        }
        return ConflictType.CONTENT_CONFLICT;
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

    private static Map<String, Object> buildMetadata(int baseFileCount, int sourceFileCount, int targetFileCount) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("baseFileCount", baseFileCount);
        metadata.put("sourceFileCount", sourceFileCount);
        metadata.put("targetFileCount", targetFileCount);
        return metadata;
    }
}
