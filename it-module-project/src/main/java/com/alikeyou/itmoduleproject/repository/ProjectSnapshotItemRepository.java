package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectSnapshotItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectSnapshotItemRepository extends JpaRepository<ProjectSnapshotItem, Long> {
    List<ProjectSnapshotItem> findBySnapshotIdOrderByCanonicalPathAsc(Long snapshotId);
}
