package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectTaskLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectTaskLogRepository extends JpaRepository<ProjectTaskLog, Long> {
    List<ProjectTaskLog> findByTaskIdOrderByCreatedAtDesc(Long taskId);
}
