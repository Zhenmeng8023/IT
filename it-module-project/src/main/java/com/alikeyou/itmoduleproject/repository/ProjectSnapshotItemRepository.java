package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectSnapshotItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectSnapshotItemRepository extends JpaRepository<ProjectSnapshotItem, Long> {
    List<ProjectSnapshotItem> findBySnapshotIdOrderByCanonicalPathAsc(Long snapshotId);

    Optional<ProjectSnapshotItem> findBySnapshotIdAndProjectFileId(Long snapshotId, Long projectFileId);

    List<ProjectSnapshotItem> findBySnapshotIdAndProjectFileIdInOrderByCanonicalPathAsc(Long snapshotId, List<Long> projectFileIds);
}
