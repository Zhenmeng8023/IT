package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectFileVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectFileVersionRepository extends JpaRepository<ProjectFileVersion, Long> {
    List<ProjectFileVersion> findByFileIdOrderByUploadedAtDesc(Long fileId);

    List<ProjectFileVersion> findByFileIdInOrderByUploadedAtDesc(List<Long> fileIds);

    boolean existsByFileIdAndVersion(Long fileId, String version);
    boolean existsByBlobId(Long blobId);

    Optional<ProjectFileVersion> findTopByFileIdOrderByUploadedAtDesc(Long fileId);

    long countByFileId(Long fileId);
}
