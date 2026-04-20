package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectCommitChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectCommitChangeRepository extends JpaRepository<ProjectCommitChange, Long> {
    List<ProjectCommitChange> findByCommitIdOrderByIdAsc(Long commitId);
    boolean existsByOldBlobIdOrNewBlobId(Long oldBlobId, Long newBlobId);
}
