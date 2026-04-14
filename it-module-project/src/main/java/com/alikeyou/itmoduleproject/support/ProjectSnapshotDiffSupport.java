package com.alikeyou.itmoduleproject.support;

import com.alikeyou.itmoduleproject.entity.ProjectSnapshotItem;
import com.alikeyou.itmoduleproject.support.diff.ChangeEntry;
import com.alikeyou.itmoduleproject.support.diff.ChangeType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public final class ProjectSnapshotDiffSupport {

    private ProjectSnapshotDiffSupport() {
    }

    public static Map<String, Object> buildComparePayload(Map<String, ProjectSnapshotItem> fromMap,
                                                          Map<String, ProjectSnapshotItem> toMap) {
        int add = 0;
        int modify = 0;
        int delete = 0;
        List<Map<String, Object>> files = new ArrayList<>();
        List<ChangeEntry> changes = buildChangeEntries(fromMap, toMap);
        for (ChangeEntry change : changes) {
            ChangeType type = change.getChangeType();
            if (ChangeType.ADD.equals(type)) {
                add++;
            } else if (ChangeType.DELETE.equals(type)) {
                delete++;
            } else if (ChangeType.MODIFY.equals(type)) {
                modify++;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("path", change.getNewPath() == null ? change.getOldPath() : change.getNewPath());
            row.put("oldPath", change.getOldPath());
            row.put("newPath", change.getNewPath());
            row.put("fileName", change.getFileName());
            row.put("changeType", type == null ? null : type.name());
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
        result.put("files", files);
        return result;
    }

    public static List<ChangeEntry> buildChangeEntries(Map<String, ProjectSnapshotItem> fromMap,
                                                       Map<String, ProjectSnapshotItem> toMap) {
        return buildChangeEntries(fromMap, toMap, null, null, null);
    }

    public static List<ChangeEntry> buildChangeEntries(Map<String, ProjectSnapshotItem> fromMap,
                                                       Map<String, ProjectSnapshotItem> toMap,
                                                       Long baseCommitId,
                                                       Long sourceCommitId,
                                                       Long targetCommitId) {
        List<ChangeEntry> changes = new ArrayList<>();
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
            ChangeType type;
            if (from == null) {
                type = ChangeType.ADD;
            } else if (to == null) {
                type = ChangeType.DELETE;
            } else if (Objects.equals(from.getBlobId(), to.getBlobId())) {
                continue;
            } else {
                type = ChangeType.MODIFY;
            }
            changes.add(ChangeEntry.builder()
                    .changeType(type)
                    .oldPath(ChangeType.ADD.equals(type) ? null : path)
                    .newPath(ChangeType.DELETE.equals(type) ? null : path)
                    .fileName(ProjectPathUtils.extractFileName(path))
                    .contentHashBefore(from == null ? null : from.getContentHash())
                    .contentHashAfter(to == null ? null : to.getContentHash())
                    .oldBlobId(from == null ? null : from.getBlobId())
                    .newBlobId(to == null ? null : to.getBlobId())
                    .baseCommitId(baseCommitId)
                    .sourceCommitId(sourceCommitId)
                    .targetCommitId(targetCommitId)
                    .summary(buildSummary(type, path))
                    .severity("INFO")
                    .build());
        }
        return changes;
    }

    private static String buildSummary(ChangeType type, String path) {
        if (type == null) {
            return path;
        }
        return type.name() + " " + path;
    }
}
