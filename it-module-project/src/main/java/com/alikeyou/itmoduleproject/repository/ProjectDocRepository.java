package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectDoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectDocRepository extends JpaRepository<ProjectDoc, Long> {

    List<ProjectDoc> findByProjectId(Long projectId);

    List<ProjectDoc> findByProjectIdAndIsMainReadmeTrueOrderByUpdatedAtDesc(Long projectId);

    List<ProjectDoc> findByProjectIdAndIsPinnedHomeTrue(Long projectId);

    Optional<ProjectDoc> findByIdAndProjectId(Long id, Long projectId);
}
