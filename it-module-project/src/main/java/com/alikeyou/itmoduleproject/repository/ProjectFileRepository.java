package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectFileRepository extends JpaRepository<ProjectFile, Long> {
    List<ProjectFile> findByProjectIdOrderByUploadTimeDesc(Long projectId);
    Optional<ProjectFile> findByProjectIdAndFileName(Long projectId, String fileName);
    List<ProjectFile> findByProjectIdAndIsMainTrue(Long projectId);
    List<ProjectFile> findByProjectIdAndIdInOrderByUploadTimeDesc(Long projectId, List<Long> ids);
    Optional<ProjectFile> findByProjectIdAndCanonicalPathAndDeletedFlagFalse(Long projectId, String canonicalPath);
    List<ProjectFile> findByProjectIdAndDeletedFlagFalseOrderByUploadTimeDesc(Long projectId);
    boolean existsByLatestBlobId(Long latestBlobId);
}
