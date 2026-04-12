package com.alikeyou.itmoduleproject.support;

import com.alikeyou.itmoduleproject.entity.ProjectSnapshotItem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectSnapshotDiffSupportTest {

    @Test
    void buildComparePayload_shouldCountAddModifyDeleteAndIgnoreUnchanged() {
        Map<String, ProjectSnapshotItem> fromMap = Map.of(
                "README.md", snapshotItem(1L, "README.md"),
                "legacy/old.txt", snapshotItem(2L, "legacy/old.txt"),
                "src/App.java", snapshotItem(3L, "src/App.java")
        );
        Map<String, ProjectSnapshotItem> toMap = Map.of(
                "README.md", snapshotItem(1L, "README.md"),
                "docs/guide.md", snapshotItem(4L, "docs/guide.md"),
                "src/App.java", snapshotItem(5L, "src/App.java")
        );

        Map<String, Object> payload = ProjectSnapshotDiffSupport.buildComparePayload(fromMap, toMap);

        assertEquals(1, payload.get("addCount"));
        assertEquals(1, payload.get("modifyCount"));
        assertEquals(1, payload.get("deleteCount"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> files = (List<Map<String, Object>>) payload.get("files");
        assertEquals(3, files.size());
        assertEquals("docs/guide.md", files.get(0).get("path"));
        assertEquals("ADD", files.get(0).get("changeType"));
        assertEquals("legacy/old.txt", files.get(1).get("path"));
        assertEquals("DELETE", files.get(1).get("changeType"));
        assertEquals("src/App.java", files.get(2).get("path"));
        assertEquals("MODIFY", files.get(2).get("changeType"));
    }

    private ProjectSnapshotItem snapshotItem(Long blobId, String path) {
        return ProjectSnapshotItem.builder()
                .blobId(blobId)
                .canonicalPath(path)
                .contentHash("hash-" + blobId)
                .projectFileId(blobId)
                .projectFileVersionId(blobId)
                .build();
    }
}
