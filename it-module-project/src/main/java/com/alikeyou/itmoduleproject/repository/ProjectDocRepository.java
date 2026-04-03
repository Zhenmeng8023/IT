package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectDoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectDocRepository extends JpaRepository<ProjectDoc, Long> {

    List<ProjectDoc> findByProjectIdOrderByUpdatedAtDesc(Long projectId);

    List<ProjectDoc> findByProjectIdAndStatusOrderByUpdatedAtDesc(Long projectId, String status);

    List<ProjectDoc> findByProjectIdAndIsPrimaryTrue(Long projectId);

    Optional<ProjectDoc> findFirstByProjectIdAndIsPrimaryTrueOrderByUpdatedAtDesc(Long projectId);
}
