package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ProjectActivityLogRepository extends JpaRepository<ProjectActivityLog, Long>, JpaSpecificationExecutor<ProjectActivityLog> {

    List<ProjectActivityLog> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
