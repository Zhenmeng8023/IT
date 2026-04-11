package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectSnapshotRepository extends JpaRepository<ProjectSnapshot, Long> {
}
