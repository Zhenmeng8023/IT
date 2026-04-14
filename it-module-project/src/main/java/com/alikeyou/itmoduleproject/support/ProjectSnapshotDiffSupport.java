package com.alikeyou.itmoduleproject.support;

import com.alikeyou.itmoduleproject.entity.ProjectSnapshotItem;
import com.alikeyou.itmoduleproject.support.diff.ChangeEntry;
import com.alikeyou.itmoduleproject.support.diff.ChangeType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

public final class ProjectSnapshotDiffSupport {

    private ProjectSnapshotDiffSupport() {
    }

    public static Map<String, Object> buildComparePayload(Map<String, ProjectSnapshotItem> fromMap,
                                                          Map<String, ProjectSnapshotItem> toMap) {
        return buildComparePayload(fromMap, toMap, null, null, null, null);
    }

    public static Map<String, Object> buildComparePayload(Map<String, ProjectSnapshotItem> fromMap,
                                                          Map<String, ProjectSnapshotItem> toMap,
                                                          Function<Long, Boolean> binaryResolver) {
        return buildComparePayload(fromMap, toMap, binaryResolver, null, null, null);
    }

    public static Map<String, Object> buildComparePayload(Map<String, ProjectSnapshotItem> fromMap,
                                                          Map<String, ProjectSnapshotItem> toMap,
                                                          Function<Long, Boolean> binaryResolver,
                                                          Long baseCommitId,
                                                          Long sourceCommitId,
                                                          Long targetCommitId) {
        int add = 0;
        int modify = 0;
        int delete = 0;
        int rename = 0;
        int move = 0;
        List<Map<String, Object>> files = new ArrayList<>();
        List<ChangeEntry> changes = buildChangeEntries(fromMap, toMap, baseCommitId, sourceCommitId, targetCommitId, binaryResolver);
        for (ChangeEntry change : changes) {
            ChangeType type = change.getChangeType();
            if (ChangeType.ADD.equals(type)) {
                add++;
            } else if (ChangeType.DELETE.equals(type)) {
                delete++;
            } else if (ChangeType.RENAME.equals(type)) {
                rename++;
                modify++;
            } else if (ChangeType.MOVE.equals(type)) {
                move++;
                modify++;
            } else if (ChangeType.MODIFY.equals(type)) {
                modify++;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("path", change.getNewPath() == null ? change.getOldPath() : change.getNewPath());
            row.put("oldPath", change.getOldPath());
            row.put("newPath", change.getNewPath());
            row.put("fileName", change.getFileName());
            row.put("changeType", type == null ? null : type.name());
            row.put("binaryFile", change.getBinaryFile());
            row.put("fromBlobId", change.getOldBlobId());
            row.put("toBlobId", change.getNewBlobId());
            row.put("contentHashBefore", change.getContentHashBefore());
            row.put("contentHashAfter", change.getContentHashAfter());
            files.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("addCount", add);
        result.put("modifyCount", modify);
        result.put("deleteCount", delete);
        result.put("renameCount", rename);
        result.put("moveCount", move);
        result.put("files", files);
        result.put("changes", changes);
        return result;
    }

    public static List<ChangeEntry> buildChangeEntries(Map<String, ProjectSnapshotItem> fromMap,
                                                       Map<String, ProjectSnapshotItem> toMap) {
        return buildChangeEntries(fromMap, toMap, null, null, null, null);
    }

    public static List<ChangeEntry> buildChangeEntries(Map<String, ProjectSnapshotItem> fromMap,
                                                       Map<String, ProjectSnapshotItem> toMap,
                                                       Function<Long, Boolean> binaryResolver) {
        return buildChangeEntries(fromMap, toMap, null, null, null, binaryResolver);
    }

    public static List<ChangeEntry> buildChangeEntries(Map<String, ProjectSnapshotItem> fromMap,
                                                       Map<String, ProjectSnapshotItem> toMap,
                                                       Long baseCommitId,
                                                       Long sourceCommitId,
                                                       Long targetCommitId) {
        return buildChangeEntries(fromMap, toMap, baseCommitId, sourceCommitId, targetCommitId, null);
    }

    public static List<ChangeEntry> buildChangeEntries(Map<String, ProjectSnapshotItem> fromMap,
                                                       Map<String, ProjectSnapshotItem> toMap,
                                                       Long baseCommitId,
                                                       Long sourceCommitId,
                                                       Long targetCommitId,
                                                       Function<Long, Boolean> binaryResolver) {
        List<ChangeEntry> changes = new ArrayList<>();
        List<PendingPathChange> addCandidates = new ArrayList<>();
        List<PendingPathChange> deleteCandidates = new ArrayList<>();
        Set<String> paths = new TreeSet<>();
        if (fromMap != null) {
            paths.addAll(fromMap.keySet());
        }
        if (toMap != null) {
            paths.addAll(toMap.keySet());
        }
        for (String path : paths) {
            ProjectSnapshotItem from = fromMap == null ? null : fromMap.get(path);
            ProjectSnapshotItem to = toMap == null ? null : toMap.get(path);
            if (from == null && to == null) {
                continue;
            }
            if (from != null && to != null) {
                if (sameSnapshotItem(from, to)) {
                    continue;
                }
                changes.add(buildChangeEntry(
                        ChangeType.MODIFY,
                        path,
                        path,
                        from,
                        to,
                        baseCommitId,
                        sourceCommitId,
                        targetCommitId,
                        binaryResolver
                ));
                continue;
            }
            if (from != null) {
                deleteCandidates.add(new PendingPathChange(path, from));
            } else {
                addCandidates.add(new PendingPathChange(path, to));
            }
        }

        Map<String, List<PendingPathChange>> addByFingerprint = new LinkedHashMap<>();
        for (PendingPathChange add : addCandidates) {
            String fingerprint = fingerprint(add.item());
            if (!hasText(fingerprint)) {
                continue;
            }
            addByFingerprint.computeIfAbsent(fingerprint, key -> new ArrayList<>()).add(add);
        }

        List<PendingPathChange> unmatchedAdds = new ArrayList<>(addCandidates);
        List<PendingPathChange> unmatchedDeletes = new ArrayList<>();
        for (PendingPathChange delete : deleteCandidates) {
            String fingerprint = fingerprint(delete.item());
            if (!hasText(fingerprint)) {
                unmatchedDeletes.add(delete);
                continue;
            }
            List<PendingPathChange> maybeAdds = addByFingerprint.get(fingerprint);
            if (maybeAdds == null || maybeAdds.isEmpty()) {
                unmatchedDeletes.add(delete);
                continue;
            }
            PendingPathChange matchedAdd = chooseBestRelocationTarget(delete.path(), maybeAdds);
            if (matchedAdd == null) {
                unmatchedDeletes.add(delete);
                continue;
            }
            maybeAdds.remove(matchedAdd);
            unmatchedAdds.remove(matchedAdd);
            ChangeType relocationType = resolveRelocationType(delete.path(), matchedAdd.path());
            changes.add(buildChangeEntry(
                    relocationType,
                    delete.path(),
                    matchedAdd.path(),
                    delete.item(),
                    matchedAdd.item(),
                    baseCommitId,
                    sourceCommitId,
                    targetCommitId,
                    binaryResolver
            ));
        }

        unmatchedDeletes.stream()
                .sorted(Comparator.comparing(PendingPathChange::path))
                .forEach(delete -> changes.add(buildChangeEntry(
                        ChangeType.DELETE,
                        delete.path(),
                        null,
                        delete.item(),
                        null,
                        baseCommitId,
                        sourceCommitId,
                        targetCommitId,
                        binaryResolver
                )));

        unmatchedAdds.stream()
                .sorted(Comparator.comparing(PendingPathChange::path))
                .forEach(add -> changes.add(buildChangeEntry(
                        ChangeType.ADD,
                        null,
                        add.path(),
                        null,
                        add.item(),
                        baseCommitId,
                        sourceCommitId,
                        targetCommitId,
                        binaryResolver
                )));

        changes.sort(Comparator
                .comparing(ProjectSnapshotDiffSupport::changeSortPrimaryKey)
                .thenComparing(ProjectSnapshotDiffSupport::changeSortSecondaryKey)
                .thenComparing(change -> change.getChangeType() == null ? "" : change.getChangeType().name()));
        return changes;
    }

    private static ChangeEntry buildChangeEntry(ChangeType type,
                                                String oldPath,
                                                String newPath,
                                                ProjectSnapshotItem before,
                                                ProjectSnapshotItem after,
                                                Long baseCommitId,
                                                Long sourceCommitId,
                                                Long targetCommitId,
                                                Function<Long, Boolean> binaryResolver) {
        String displayPath = newPath == null ? oldPath : newPath;
        return ChangeEntry.builder()
                .changeType(type)
                .oldPath(oldPath)
                .newPath(newPath)
                .fileName(displayPath == null ? null : ProjectPathUtils.extractFileName(displayPath))
                .binaryFile(resolveBinaryFlag(before, after, binaryResolver))
                .contentHashBefore(before == null ? null : before.getContentHash())
                .contentHashAfter(after == null ? null : after.getContentHash())
                .oldBlobId(before == null ? null : before.getBlobId())
                .newBlobId(after == null ? null : after.getBlobId())
                .baseCommitId(baseCommitId)
                .sourceCommitId(sourceCommitId)
                .targetCommitId(targetCommitId)
                .summary(buildSummary(type, oldPath, newPath))
                .severity("INFO")
                .build();
    }

    private static PendingPathChange chooseBestRelocationTarget(String oldPath,
                                                                List<PendingPathChange> candidates) {
        PendingPathChange best = null;
        int bestScore = Integer.MAX_VALUE;
        for (PendingPathChange candidate : candidates) {
            int score = relocationScore(oldPath, candidate.path());
            if (best == null || score < bestScore
                    || (score == bestScore && candidate.path().compareTo(best.path()) < 0)) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private static int relocationScore(String oldPath, String newPath) {
        ChangeType relocationType = resolveRelocationType(oldPath, newPath);
        if (ChangeType.RENAME.equals(relocationType)) {
            return 0;
        }
        if (Objects.equals(ProjectPathUtils.extractFileName(oldPath), ProjectPathUtils.extractFileName(newPath))) {
            return 1;
        }
        return 2;
    }

    private static ChangeType resolveRelocationType(String oldPath, String newPath) {
        String oldDir = extractDirectory(oldPath);
        String newDir = extractDirectory(newPath);
        String oldName = ProjectPathUtils.extractFileName(oldPath);
        String newName = ProjectPathUtils.extractFileName(newPath);
        if (Objects.equals(oldDir, newDir) && !Objects.equals(oldName, newName)) {
            return ChangeType.RENAME;
        }
        if (!Objects.equals(oldDir, newDir) && Objects.equals(oldName, newName)) {
            return ChangeType.MOVE;
        }
        return ChangeType.MOVE;
    }

    private static String extractDirectory(String path) {
        if (!hasText(path)) {
            return "";
        }
        int index = path.lastIndexOf('/');
        if (index < 0) {
            return "";
        }
        if (index == 0) {
            return "/";
        }
        return path.substring(0, index);
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
        return hasText(left.getContentHash()) && Objects.equals(left.getContentHash(), right.getContentHash());
    }

    private static String fingerprint(ProjectSnapshotItem item) {
        if (item == null) {
            return null;
        }
        if (hasText(item.getContentHash())) {
            return "hash:" + item.getContentHash();
        }
        if (item.getBlobId() != null) {
            return "blob:" + item.getBlobId();
        }
        return null;
    }

    private static Boolean resolveBinaryFlag(ProjectSnapshotItem before,
                                             ProjectSnapshotItem after,
                                             Function<Long, Boolean> binaryResolver) {
        Boolean beforeBinary = resolveBinaryByBlob(before == null ? null : before.getBlobId(), binaryResolver);
        Boolean afterBinary = resolveBinaryByBlob(after == null ? null : after.getBlobId(), binaryResolver);
        if (Boolean.TRUE.equals(beforeBinary) || Boolean.TRUE.equals(afterBinary)) {
            return Boolean.TRUE;
        }
        if (Boolean.FALSE.equals(beforeBinary) || Boolean.FALSE.equals(afterBinary)) {
            return Boolean.FALSE;
        }
        return Boolean.FALSE;
    }

    private static Boolean resolveBinaryByBlob(Long blobId, Function<Long, Boolean> binaryResolver) {
        if (blobId == null || binaryResolver == null) {
            return null;
        }
        return binaryResolver.apply(blobId);
    }

    private static String buildSummary(ChangeType type, String oldPath, String newPath) {
        if (type == null) {
            return newPath != null ? newPath : oldPath;
        }
        if (ChangeType.RENAME.equals(type) || ChangeType.MOVE.equals(type)) {
            return type.name() + " " + oldPath + " -> " + newPath;
        }
        String path = newPath != null ? newPath : oldPath;
        return type.name() + " " + path;
    }

    private static String changeSortPrimaryKey(ChangeEntry entry) {
        if (entry == null) {
            return "";
        }
        if (hasText(entry.getNewPath())) {
            return entry.getNewPath();
        }
        return entry.getOldPath() == null ? "" : entry.getOldPath();
    }

    private static String changeSortSecondaryKey(ChangeEntry entry) {
        if (entry == null || entry.getOldPath() == null) {
            return "";
        }
        return entry.getOldPath();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record PendingPathChange(String path, ProjectSnapshotItem item) {
    }
}
