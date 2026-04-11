package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectCodeRepositoryRepository extends JpaRepository<ProjectCodeRepository, Long> {
    Optional<ProjectCodeRepository> findByProjectId(Long projectId);
}
