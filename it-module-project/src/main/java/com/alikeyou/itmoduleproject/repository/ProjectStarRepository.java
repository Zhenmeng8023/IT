package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectStar;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectStarRepository extends JpaRepository<ProjectStar, Long> {

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    Optional<ProjectStar> findByProjectIdAndUserId(Long projectId, Long userId);

    void deleteByProjectIdAndUserId(Long projectId, Long userId);

    long countByProjectId(Long projectId);

    Page<ProjectStar> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<ProjectStar> findByUserIdAndProjectIdIn(Long userId, Collection<Long> projectIds);
}
