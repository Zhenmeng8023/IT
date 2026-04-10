package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectDownloadRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProjectDownloadRecordRepository extends JpaRepository<ProjectDownloadRecord, Long> {

    Page<ProjectDownloadRecord> findByProjectIdOrderByDownloadedAtDesc(Long projectId, Pageable pageable);

    List<ProjectDownloadRecord> findByProjectIdAndDownloadedAtBetweenOrderByDownloadedAtAsc(Long projectId, LocalDateTime startTime, LocalDateTime endTime);

    long countByProjectId(Long projectId);

    @Query("select count(distinct r.userId) from ProjectDownloadRecord r where r.projectId = :projectId and r.userId is not null")
    long countDistinctUserIdByProjectId(@Param("projectId") Long projectId);
}
