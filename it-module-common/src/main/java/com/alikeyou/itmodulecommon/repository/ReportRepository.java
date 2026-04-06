package com.alikeyou.itmodulecommon.repository;

import com.alikeyou.itmodulecommon.entity.Report;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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

    boolean existsByReporter_IdAndTargetTypeAndTargetIdAndStatus(Long reporterId, String targetType, Long targetId, String status);

    long countByTargetTypeAndTargetId(String targetType, Long targetId);

    long countByTargetTypeAndTargetIdAndStatus(String targetType, Long targetId, String status);

    long countByTargetTypeAndStatus(String targetType, String status);

    long countByTargetTypeAndCreatedAtGreaterThanEqual(String targetType, Instant createdAt);

    long countByTargetTypeAndStatusAndCreatedAtGreaterThanEqual(String targetType, String status, Instant createdAt);

    @EntityGraph(attributePaths = {"reporter", "processor"})
    List<Report> findByTargetTypeAndTargetIdAndStatus(@Param("targetType") String targetType, @Param("targetId") Long targetId, @Param("status") String status);

    @EntityGraph(attributePaths = {"reporter", "processor"})
    Optional<Report> findTopByReporter_IdAndTargetTypeAndTargetIdOrderByCreatedAtDesc(Long reporterId, String targetType, Long targetId);

    @Query("SELECT r FROM Report r WHERE (:targetType IS NULL OR r.targetType = :targetType) AND (:targetId IS NULL OR r.targetId = :targetId) AND (:status IS NULL OR r.status = :status)")
    org.springframework.data.domain.Page<Report> findPageByConditions(
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("status") String status,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("SELECT r.targetId AS targetId, COUNT(r.id) AS reportCount, MAX(r.createdAt) AS latestReportedAt FROM Report r WHERE r.targetType = :targetType AND r.targetId IN :targetIds AND (:status IS NULL OR r.status = :status) GROUP BY r.targetId")
    List<TargetReportStatsProjection> findTargetReportStatsByTargetTypeAndTargetIdsAndStatus(
            @Param("targetType") String targetType,
            @Param("targetIds") List<Long> targetIds,
            @Param("status") String status
    );

    @Query("SELECT r.targetId AS targetId, COUNT(r.id) AS reportCount, MAX(r.createdAt) AS latestReportedAt FROM Report r WHERE r.targetType = :targetType AND (:status IS NULL OR r.status = :status) GROUP BY r.targetId HAVING COUNT(r.id) >= :minCount ORDER BY COUNT(r.id) DESC, MAX(r.createdAt) DESC")
    List<TargetReportStatsProjection> findTargetReportStatsByTargetTypeAndMinCountAndStatus(
            @Param("targetType") String targetType,
            @Param("minCount") long minCount,
            @Param("status") String status
    );

    @Query("SELECT r.targetId AS targetId, COUNT(r.id) AS reportCount, MAX(r.createdAt) AS latestReportedAt FROM Report r WHERE r.targetType = :targetType GROUP BY r.targetId HAVING COUNT(r.id) >= :minCount ORDER BY COUNT(r.id) DESC, MAX(r.createdAt) DESC")
    List<TargetReportStatsProjection> findTargetReportStatsByTargetTypeAndMinCount(
            @Param("targetType") String targetType,
            @Param("minCount") long minCount
    );
}
