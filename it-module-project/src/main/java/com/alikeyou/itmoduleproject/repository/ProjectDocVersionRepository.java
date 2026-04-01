package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectDocVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectDocVersionRepository extends JpaRepository<ProjectDocVersion, Long> {

    List<ProjectDocVersion> findByDocIdOrderByVersionNoDescCreatedAtDesc(Long docId);

    Optional<ProjectDocVersion> findByDocIdAndVersionNo(Long docId, Integer versionNo);

    void deleteByDocId(Long docId);
}
