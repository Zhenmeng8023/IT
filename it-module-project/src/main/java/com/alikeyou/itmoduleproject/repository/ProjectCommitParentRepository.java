package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectCommitParent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectCommitParentRepository extends JpaRepository<ProjectCommitParent, Long> {
    List<ProjectCommitParent> findByCommitIdOrderByParentOrderAsc(Long commitId);
}
