package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectBlob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectBlobRepository extends JpaRepository<ProjectBlob, Long> {
    Optional<ProjectBlob> findBySha256AndSizeBytes(String sha256, Long sizeBytes);
}
