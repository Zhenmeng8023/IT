package com.alikeyou.itmoduleproject.support;

import com.alikeyou.itmoduleproject.entity.ProjectSnapshotItem;

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
            String type;
            if (from == null) {
                type = "ADD";
                add++;
            } else if (to == null) {
                type = "DELETE";
                delete++;
            } else if (Objects.equals(from.getBlobId(), to.getBlobId())) {
                continue;
            } else {
                type = "MODIFY";
                modify++;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("path", path);
            row.put("changeType", type);
            row.put("fromBlobId", from == null ? null : from.getBlobId());
            row.put("toBlobId", to == null ? null : to.getBlobId());
            files.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("addCount", add);
        result.put("modifyCount", modify);
        result.put("deleteCount", delete);
        result.put("files", files);
        return result;
    }
}
