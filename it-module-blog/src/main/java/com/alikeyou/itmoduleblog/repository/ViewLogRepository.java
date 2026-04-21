package com.alikeyou.itmoduleblog.repository;

import com.alikeyou.itmoduleblog.entity.ViewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface ViewLogRepository extends JpaRepository<ViewLog, Long> {
    interface DailyCountProjection {
        String getStatDate();
        Long getTotal();
    }

    long countByUserId(Long userId);

    long countByTargetTypeAndCreatedAtGreaterThanEqual(String targetType, Instant createdAt);

    java.util.List<ViewLog> findByUserIdAndCreatedAtBetween(Long userId, java.time.Instant start, java.time.Instant end);

    java.util.Optional<ViewLog> findTopByUserIdAndTargetTypeAndTargetIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            Long userId,
            String targetType,
            Long targetId,
            java.time.Instant createdAt
    );

    java.util.Optional<ViewLog> findTopByUserIdIsNullAndTargetTypeAndTargetIdAndIpAddressAndUserAgentAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            String targetType,
            Long targetId,
            String ipAddress,
            String userAgent,
            java.time.Instant createdAt
    );

    @org.springframework.data.jpa.repository.Query(value = "SELECT DATE(v.created_at) AS statDate, COUNT(*) AS total FROM view_log v WHERE v.target_type = :targetType AND v.created_at >= :start GROUP BY DATE(v.created_at) ORDER BY DATE(v.created_at)", nativeQuery = true)
    java.util.List<DailyCountProjection> countByTargetTypeDailySince(
            @org.springframework.data.repository.query.Param("targetType") String targetType,
            @org.springframework.data.repository.query.Param("start") java.time.Instant start
    );
}
