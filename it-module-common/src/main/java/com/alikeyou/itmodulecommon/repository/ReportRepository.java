package com.alikeyou.itmodulecommon.repository;

import com.alikeyou.itmodulecommon.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    interface TargetReportStatsProjection {
        Long getTargetId();
        Long getReportCount();
        Instant getLatestReportedAt();
    }

    @EntityGraph(attributePaths = {"reporter"})
    List<Report> findByTargetTypeAndTargetId(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    @EntityGraph(attributePaths = {"reporter"})
    List<Report> findByStatus(@Param("status") String status);

    @EntityGraph(attributePaths = {"reporter", "processor"})
    List<Report> findAll();

    boolean existsByReporter_IdAndTargetTypeAndTargetId(Long reporterId, String targetType, Long targetId);

    long countByTargetTypeAndTargetId(String targetType, Long targetId);

    @Query("""
            SELECT r.targetId AS targetId,
                   COUNT(r.id) AS reportCount,
                   MAX(r.createdAt) AS latestReportedAt
            FROM Report r
            WHERE r.targetType = :targetType
              AND r.targetId IN :targetIds
            GROUP BY r.targetId
            """)
    List<TargetReportStatsProjection> findTargetReportStatsByTargetTypeAndTargetIds(
            @Param("targetType") String targetType,
            @Param("targetIds") List<Long> targetIds
    );

    @Query("""
            SELECT r.targetId AS targetId,
                   COUNT(r.id) AS reportCount,
                   MAX(r.createdAt) AS latestReportedAt
            FROM Report r
            WHERE r.targetType = :targetType
            GROUP BY r.targetId
            HAVING COUNT(r.id) >= :minCount
            ORDER BY COUNT(r.id) DESC, MAX(r.createdAt) DESC
            """)
    List<TargetReportStatsProjection> findTargetReportStatsByTargetTypeAndMinCount(
            @Param("targetType") String targetType,
            @Param("minCount") long minCount
    );
}
