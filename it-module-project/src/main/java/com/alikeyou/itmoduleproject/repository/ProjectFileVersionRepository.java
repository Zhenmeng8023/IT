package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectFileVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectFileVersionRepository extends JpaRepository<ProjectFileVersion, Long> {
    List<ProjectFileVersion> findByFileIdOrderByUploadedAtDesc(Long fileId);
}
