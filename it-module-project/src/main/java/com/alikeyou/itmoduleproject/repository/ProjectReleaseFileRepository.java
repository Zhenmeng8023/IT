package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectReleaseFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectReleaseFileRepository extends JpaRepository<ProjectReleaseFile, Long> {

    List<ProjectReleaseFile> findByReleaseIdOrderBySortOrderAscIdAsc(Long releaseId);

    boolean existsByReleaseIdAndProjectFileId(Long releaseId, Long projectFileId);

    long countByReleaseId(Long releaseId);

    void deleteByReleaseIdAndId(Long releaseId, Long id);
}
