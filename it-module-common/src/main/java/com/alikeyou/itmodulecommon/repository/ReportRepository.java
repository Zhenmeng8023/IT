package com.alikeyou.itmodulecommon.repository;

import com.alikeyou.itmodulecommon.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @EntityGraph(attributePaths = {"reporter", "processor"})
    List<Report> findByTargetTypeAndTargetIdAndStatus(@Param("targetType") String targetType,
                                                      @Param("targetId") Long targetId,
                                                      @Param("status") String status);

    @EntityGraph(attributePaths = {"reporter"})
    List<Report> findByStatus(@Param("status") String status);

    @EntityGraph(attributePaths = {"reporter", "processor"})
    List<Report> findAll();

    boolean existsByReporter_IdAndTargetTypeAndTargetId(Long reporterId, String targetType, Long targetId);

    boolean existsByReporter_IdAndTargetTypeAndTargetIdAndStatus(Long reporterId, String targetType, Long targetId, String status);

    boolean existsByReporter_IdAndTargetTypeAndTargetIdAndStatusNot(Long reporterId, String targetType, Long targetId, String status);

    long countByTargetTypeAndTargetId(String targetType, Long targetId);

    long countByTargetTypeAndTargetIdAndStatus(String targetType, Long targetId, String status);

    long countByTargetTypeAndTargetIdAndStatusNot(String targetType, Long targetId, String status);

    @EntityGraph(attributePaths = {"reporter", "processor"})
    @Query("""
            SELECT r
            FROM Report r
            WHERE (:targetType IS NULL OR r.targetType = :targetType)
              AND (:targetId IS NULL OR r.targetId = :targetId)
              AND (:status IS NULL OR r.status = :status)
            """)
    Page<Report> findPageByConditions(
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("status") String status,
            Pageable pageable
    );

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
              AND r.targetId IN :targetIds
              AND r.status = :status
            GROUP BY r.targetId
            """)
    List<TargetReportStatsProjection> findTargetReportStatsByTargetTypeAndTargetIdsAndStatus(
            @Param("targetType") String targetType,
            @Param("targetIds") List<Long> targetIds,
            @Param("status") String status
    );

    @Query("""
            SELECT r.targetId AS targetId,
                   COUNT(r.id) AS reportCount,
                   MAX(r.createdAt) AS latestReportedAt
            FROM Report r
            WHERE r.targetType = :targetType
              AND r.targetId IN :targetIds
              AND (r.status IS NULL OR r.status <> :excludedStatus)
            GROUP BY r.targetId
            """)
    List<TargetReportStatsProjection> findTargetReportStatsByTargetTypeAndTargetIdsExcludingStatus(
            @Param("targetType") String targetType,
            @Param("targetIds") List<Long> targetIds,
            @Param("excludedStatus") String excludedStatus
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

    @Query("""
            SELECT r.targetId AS targetId,
                   COUNT(r.id) AS reportCount,
                   MAX(r.createdAt) AS latestReportedAt
            FROM Report r
            WHERE r.targetType = :targetType
              AND r.status = :status
            GROUP BY r.targetId
            HAVING COUNT(r.id) >= :minCount
            ORDER BY COUNT(r.id) DESC, MAX(r.createdAt) DESC
            """)
    List<TargetReportStatsProjection> findTargetReportStatsByTargetTypeAndMinCountAndStatus(
            @Param("targetType") String targetType,
            @Param("minCount") long minCount,
            @Param("status") String status
    );

    @Query("""
            SELECT r.targetId AS targetId,
                   COUNT(r.id) AS reportCount,
                   MAX(r.createdAt) AS latestReportedAt
            FROM Report r
            WHERE r.targetType = :targetType
              AND (r.status IS NULL OR r.status <> :excludedStatus)
            GROUP BY r.targetId
            HAVING COUNT(r.id) >= :minCount
            ORDER BY COUNT(r.id) DESC, MAX(r.createdAt) DESC
            """)
    List<TargetReportStatsProjection> findTargetReportStatsByTargetTypeAndMinCountExcludingStatus(
            @Param("targetType") String targetType,
            @Param("minCount") long minCount,
            @Param("excludedStatus") String excludedStatus
    );
}
