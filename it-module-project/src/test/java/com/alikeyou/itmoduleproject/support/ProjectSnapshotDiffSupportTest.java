package com.alikeyou.itmoduleproject.support;

import com.alikeyou.itmoduleproject.entity.ProjectSnapshotItem;
import com.alikeyou.itmoduleproject.support.diff.ChangeEntry;
import com.alikeyou.itmoduleproject.support.diff.ChangeType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProjectSnapshotDiffSupportTest {

    @Test
    void buildComparePayload_shouldExposeUnifiedCountsAndChanges() {
        Map<String, ProjectSnapshotItem> fromMap = Map.of(
                "/docs/old-name.md", snapshotItem(1L, "/docs/old-name.md", "hash-1"),
                "/src/App.java", snapshotItem(2L, "/src/App.java", "hash-2"),
                "/legacy/dead.txt", snapshotItem(3L, "/legacy/dead.txt", "hash-3"),
                "/assets/logo.png", snapshotItem(4L, "/assets/logo.png", "hash-4")
        );
        Map<String, ProjectSnapshotItem> toMap = Map.of(
                "/docs/new-name.md", snapshotItem(11L, "/docs/new-name.md", "hash-1"),
                "/src/App.java", snapshotItem(12L, "/src/App.java", "hash-12"),
                "/public/logo.png", snapshotItem(14L, "/public/logo.png", "hash-4"),
                "/README.md", snapshotItem(5L, "/README.md", "hash-5")
        );
        Function<Long, Boolean> binaryResolver = blobId -> blobId != null && (blobId == 4L || blobId == 14L);

        Map<String, Object> payload = ProjectSnapshotDiffSupport.buildComparePayload(
                fromMap,
                toMap,
                binaryResolver,
                100L,
                101L,
                102L
        );

        assertEquals(1, payload.get("addCount"));
        assertEquals(3, payload.get("modifyCount"));
        assertEquals(1, payload.get("deleteCount"));
        assertEquals(1, payload.get("renameCount"));
        assertEquals(1, payload.get("moveCount"));

        @SuppressWarnings("unchecked")
        List<ChangeEntry> changes = (List<ChangeEntry>) payload.get("changes");
        assertEquals(5, changes.size());

        ChangeEntry rename = findByType(changes, ChangeType.RENAME);
        assertNotNull(rename);
        assertEquals("/docs/old-name.md", rename.getOldPath());
        assertEquals("/docs/new-name.md", rename.getNewPath());

        ChangeEntry move = findByType(changes, ChangeType.MOVE);
        assertNotNull(move);
        assertEquals("/assets/logo.png", move.getOldPath());
        assertEquals("/public/logo.png", move.getNewPath());
        assertEquals(Boolean.TRUE, move.getBinaryFile());
    }

    @Test
    void buildChangeEntries_shouldApplyRenameMoveRulesInPriorityOrder() {
        Map<String, ProjectSnapshotItem> fromMap = Map.of(
                "/same-dir/old.txt", snapshotItem(1L, "/same-dir/old.txt", "h1"),
                "/old-dir/same.txt", snapshotItem(2L, "/old-dir/same.txt", "h2"),
                "/old-dir/old.java", snapshotItem(3L, "/old-dir/old.java", "h3")
        );
        Map<String, ProjectSnapshotItem> toMap = Map.of(
                "/same-dir/new.txt", snapshotItem(11L, "/same-dir/new.txt", "h1"),
                "/new-dir/same.txt", snapshotItem(12L, "/new-dir/same.txt", "h2"),
                "/new-dir/new.java", snapshotItem(13L, "/new-dir/new.java", "h3")
        );

        List<ChangeEntry> changes = ProjectSnapshotDiffSupport.buildChangeEntries(fromMap, toMap);
        assertEquals(3, changes.size());

        ChangeEntry rename = changes.stream()
                .filter(change -> ChangeType.RENAME.equals(change.getChangeType()))
                .findFirst()
                .orElse(null);
        assertNotNull(rename);
        assertEquals("/same-dir/old.txt", rename.getOldPath());
        assertEquals("/same-dir/new.txt", rename.getNewPath());

        List<ChangeEntry> moves = changes.stream()
                .filter(change -> ChangeType.MOVE.equals(change.getChangeType()))
                .toList();
        assertEquals(2, moves.size());
    }

    @Test
    void buildChangeEntries_shouldExposeAddModifyDelete() {
        Map<String, ProjectSnapshotItem> fromMap = Map.of(
                "/src/keep-and-modify.java", snapshotItem(1L, "/src/keep-and-modify.java", "hash-old"),
                "/src/remove.txt", snapshotItem(2L, "/src/remove.txt", "hash-delete")
        );
        Map<String, ProjectSnapshotItem> toMap = Map.of(
                "/src/keep-and-modify.java", snapshotItem(11L, "/src/keep-and-modify.java", "hash-new"),
                "/src/add.txt", snapshotItem(12L, "/src/add.txt", "hash-add")
        );

        List<ChangeEntry> changes = ProjectSnapshotDiffSupport.buildChangeEntries(fromMap, toMap);

        assertEquals(3, changes.size());
        assertNotNull(findByType(changes, ChangeType.ADD));
        assertNotNull(findByType(changes, ChangeType.MODIFY));
        assertNotNull(findByType(changes, ChangeType.DELETE));
    }

    private ChangeEntry findByType(List<ChangeEntry> changes, ChangeType type) {
        return changes.stream()
                .filter(change -> type.equals(change.getChangeType()))
                .findFirst()
                .orElse(null);
    }

    private ProjectSnapshotItem snapshotItem(Long blobId, String path, String hash) {
        return ProjectSnapshotItem.builder()
                .blobId(blobId)
                .canonicalPath(path)
                .contentHash(hash)
                .projectFileId(blobId)
                .projectFileVersionId(blobId)
                .build();
    }
}
