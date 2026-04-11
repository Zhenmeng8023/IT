package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectCheckRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectCheckRunRepository extends JpaRepository<ProjectCheckRun, Long> {
    List<ProjectCheckRun> findByCommitIdOrderByCreatedAtDesc(Long commitId);
    List<ProjectCheckRun> findByMergeRequestIdOrderByCreatedAtDesc(Long mergeRequestId);
}
