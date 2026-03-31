package com.alikeyou.itmodulecommon.repository;

import com.alikeyou.itmodulecommon.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    @EntityGraph(attributePaths = {"reporter"})
    List<Report> findByTargetTypeAndTargetId(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    @EntityGraph(attributePaths = {"reporter"})
    List<Report> findByStatus(@Param("status") String status);

    @EntityGraph(attributePaths = {"reporter", "processor"})
    List<Report> findAll();
}
