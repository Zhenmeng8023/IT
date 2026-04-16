package com.alikeyou.itmoduleproject.support.diff;

import com.alikeyou.itmoduleproject.entity.ProjectSnapshotItem;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectMergeDiffSupportTest {

    @Test
    void buildMergeCheck_shouldDetectRenameConflict() {
        Map<String, ProjectSnapshotItem> base = Map.of(
                "/a.txt", item(1L, "/a.txt", "hash-1")
        );
        Map<String, ProjectSnapshotItem> source = Map.of(
                "/b.txt", item(1L, "/b.txt", "hash-1")
        );
        Map<String, ProjectSnapshotItem> target = Map.of(
                "/c.txt", item(1L, "/c.txt", "hash-1")
        );

        MergeCheckResult result = ProjectMergeDiffSupport.buildMergeCheck(
                10L, 20L, 30L, 40L, 50L, 60L, 70L, base, source, target
        );

        assertFalse(Boolean.TRUE.equals(result.getMergeable()));
        assertTrue(result.getConflicts().stream().anyMatch(detail -> detail.getConflictType() == ConflictType.RENAME_CONFLICT));
        ConflictDetail detail = findConflict(result, ConflictType.RENAME_CONFLICT);
        assertEquals("/b.txt", detail.getPath());
        assertEquals("b.txt", detail.getFileName());
        assertEquals("/a.txt", detail.getBasePath());
        assertEquals("/b.txt", detail.getSourcePath());
        assertEquals("/c.txt", detail.getTargetPath());
        assertEquals(Boolean.FALSE, detail.getBinaryFile());
        assertEquals(Boolean.TRUE, detail.getMetadata().get("hasPathChange"));
        assertEquals("plaintext", detail.getMetadata().get("language"));
    }

    @Test
    void buildMergeCheck_shouldDetectMoveConflict() {
        Map<String, ProjectSnapshotItem> base = Map.of(
                "/src/a.txt", item(1L, "/src/a.txt", "hash-1")
        );
        Map<String, ProjectSnapshotItem> source = Map.of(
                "/module/a.txt", item(1L, "/module/a.txt", "hash-1")
        );
        Map<String, ProjectSnapshotItem> target = Map.of(
                "/docs/a.txt", item(1L, "/docs/a.txt", "hash-1")
        );

        MergeCheckResult result = ProjectMergeDiffSupport.buildMergeCheck(
                10L, 20L, 30L, 40L, 50L, 60L, 70L, base, source, target
        );

        assertFalse(Boolean.TRUE.equals(result.getMergeable()));
        assertTrue(result.getConflicts().stream().anyMatch(detail -> detail.getConflictType() == ConflictType.MOVE_CONFLICT));
        ConflictDetail detail = findConflict(result, ConflictType.MOVE_CONFLICT);
        assertEquals("/module/a.txt", detail.getPath());
        assertEquals("a.txt", detail.getFileName());
        assertEquals("/src/a.txt", detail.getBasePath());
        assertEquals("/module/a.txt", detail.getSourcePath());
        assertEquals("/docs/a.txt", detail.getTargetPath());
        assertEquals(Boolean.FALSE, detail.getBinaryFile());
        assertEquals(Boolean.TRUE, detail.getMetadata().get("hasPathChange"));
    }

    @Test
    void buildMergeCheck_shouldDetectTargetPathOccupied() {
        Map<String, ProjectSnapshotItem> source = Map.of(
                "/same.txt", item(1L, "/same.txt", "hash-source")
        );
        Map<String, ProjectSnapshotItem> target = Map.of(
                "/same.txt", item(2L, "/same.txt", "hash-target")
        );

        MergeCheckResult result = ProjectMergeDiffSupport.buildMergeCheck(
                10L, 20L, 30L, 40L, 50L, 60L, 70L, Map.of(), source, target
        );

        assertFalse(Boolean.TRUE.equals(result.getMergeable()));
        assertTrue(result.getConflicts().stream().anyMatch(detail -> detail.getConflictType() == ConflictType.TARGET_PATH_OCCUPIED));
        ConflictDetail detail = findConflict(result, ConflictType.TARGET_PATH_OCCUPIED);
        assertEquals("/same.txt", detail.getPath());
        assertEquals("same.txt", detail.getFileName());
        assertEquals("/same.txt", detail.getSourcePath());
        assertEquals("/same.txt", detail.getTargetPath());
        assertEquals(Boolean.FALSE, detail.getBinaryFile());
        assertNotNull(detail.getMetadata().get("resolutionStrategies"));
    }

    @Test
    void buildMergeCheck_shouldReportMissingBaseConflict() {
        MergeCheckResult result = ProjectMergeDiffSupport.buildMergeCheck(
                10L, 20L, 30L, 40L, null, 60L, 70L, Map.of(), Map.of(), Map.of()
        );

        assertFalse(Boolean.TRUE.equals(result.getMergeable()));
        assertTrue(result.getConflicts().stream().anyMatch(detail -> detail.getConflictType() == ConflictType.MISSING_BASE));
    }

    @Test
    void buildMergeCheck_shouldDetectContentConflict() {
        Map<String, ProjectSnapshotItem> base = Map.of(
                "/src/App.java", item(1L, "/src/App.java", "base-hash")
        );
        Map<String, ProjectSnapshotItem> source = Map.of(
                "/src/App.java", item(2L, "/src/App.java", "source-hash")
        );
        Map<String, ProjectSnapshotItem> target = Map.of(
                "/src/App.java", item(3L, "/src/App.java", "target-hash")
        );

        MergeCheckResult result = ProjectMergeDiffSupport.buildMergeCheck(
                10L, 20L, 30L, 40L, 50L, 60L, 70L, base, source, target
        );

        assertFalse(Boolean.TRUE.equals(result.getMergeable()));
        assertTrue(result.getConflicts().stream().anyMatch(detail -> detail.getConflictType() == ConflictType.CONTENT_CONFLICT));
        ConflictDetail detail = findConflict(result, ConflictType.CONTENT_CONFLICT);
        assertEquals("/src/App.java", detail.getPath());
        assertEquals("App.java", detail.getFileName());
        assertEquals("/src/App.java", detail.getBasePath());
        assertEquals("/src/App.java", detail.getSourcePath());
        assertEquals("/src/App.java", detail.getTargetPath());
        assertEquals(Boolean.FALSE, detail.getBinaryFile());
        assertEquals("java", detail.getMetadata().get("language"));
        assertEquals(Boolean.TRUE, detail.getMetadata().get("onlineEditable"));
        assertEquals(Boolean.FALSE, detail.getMetadata().get("readOnly"));
    }

    @Test
    void buildMergeCheck_shouldDetectMixedPathAndContentConflicts() {
        Map<String, ProjectSnapshotItem> base = Map.of(
                "/docs/guide.md", item(1L, "/docs/guide.md", "guide-base"),
                "/src/App.java", item(2L, "/src/App.java", "app-base")
        );
        Map<String, ProjectSnapshotItem> source = Map.of(
                "/guide.md", item(1L, "/guide.md", "guide-base"),
                "/src/App.java", item(3L, "/src/App.java", "app-source")
        );
        Map<String, ProjectSnapshotItem> target = Map.of(
                "/manual.md", item(1L, "/manual.md", "guide-base"),
                "/src/App.java", item(4L, "/src/App.java", "app-target")
        );

        MergeCheckResult result = ProjectMergeDiffSupport.buildMergeCheck(
                10L, 20L, 30L, 40L, 50L, 60L, 70L, base, source, target
        );

        assertFalse(Boolean.TRUE.equals(result.getMergeable()));
        assertEquals(2, result.getTotalConflicts());
        assertNotNull(findConflict(result, ConflictType.CONTENT_CONFLICT));
        assertNotNull(findConflict(result, ConflictType.MOVE_CONFLICT));
    }

    @Test
    void buildMergeCheck_shouldPassAfterRecheckWithUpdatedTarget() {
        Map<String, ProjectSnapshotItem> base = Map.of(
                "/src/App.java", item(1L, "/src/App.java", "base-hash")
        );
        Map<String, ProjectSnapshotItem> source = Map.of(
                "/src/App.java", item(2L, "/src/App.java", "source-hash")
        );
        Map<String, ProjectSnapshotItem> staleTarget = Map.of(
                "/src/App.java", item(3L, "/src/App.java", "target-old")
        );
        Map<String, ProjectSnapshotItem> refreshedTarget = Map.of(
                "/src/App.java", item(1L, "/src/App.java", "base-hash")
        );

        MergeCheckResult firstResult = ProjectMergeDiffSupport.buildMergeCheck(
                10L, 20L, 30L, 40L, 50L, 60L, 70L, base, source, staleTarget
        );
        MergeCheckResult recheckedResult = ProjectMergeDiffSupport.buildMergeCheck(
                10L, 20L, 30L, 40L, 50L, 60L, 71L, base, source, refreshedTarget
        );

        assertFalse(Boolean.TRUE.equals(firstResult.getMergeable()));
        assertTrue(Boolean.TRUE.equals(recheckedResult.getMergeable()));
        assertEquals(0, recheckedResult.getTotalConflicts());
        assertEquals(1, recheckedResult.getChanges().size());
        ChangeEntry acceptedChange = recheckedResult.getChanges().get(0);
        assertNotNull(acceptedChange);
        assertEquals(ChangeType.MODIFY, acceptedChange.getChangeType());
    }

    private ProjectSnapshotItem item(Long blobId, String path, String hash) {
        return ProjectSnapshotItem.builder()
                .projectFileId(blobId)
                .projectFileVersionId(blobId)
                .blobId(blobId)
                .canonicalPath(path)
                .contentHash(hash)
                .build();
    }

    private ConflictDetail findConflict(MergeCheckResult result, ConflictType type) {
        return result.getConflicts().stream()
                .filter(detail -> detail.getConflictType() == type)
                .findFirst()
                .orElseThrow();
    }
}
