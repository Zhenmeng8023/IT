package com.alikeyou.itmoduleinteractive.repository;

import com.alikeyou.itmoduleinteractive.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            SELECT n FROM Notification n
            WHERE n.receiverId = :receiverId
              AND n.deletedAt IS NULL
              AND (:category IS NULL OR :category = '' OR n.category = :category)
              AND (:type IS NULL OR :type = '' OR n.type = :type)
              AND (:readStatus IS NULL OR n.readStatus = :readStatus)
            ORDER BY n.createdAt DESC
            """)
    Page<Notification> pageMine(@Param("receiverId") Long receiverId,
                                @Param("category") String category,
                                @Param("type") String type,
                                @Param("readStatus") Boolean readStatus,
                                Pageable pageable);

    List<Notification> findByReceiverIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long receiverId);

    List<Notification> findByReceiverIdAndReadStatusFalseAndDeletedAtIsNullOrderByCreatedAtDesc(Long receiverId);

    long countByReceiverIdAndReadStatusFalseAndDeletedAtIsNull(Long receiverId);

    @Query("""
            SELECT n.category, COUNT(n)
            FROM Notification n
            WHERE n.receiverId = :receiverId
              AND n.readStatus = false
              AND n.deletedAt IS NULL
            GROUP BY n.category
            """)
    List<Object[]> countUnreadByCategory(@Param("receiverId") Long receiverId);

    boolean existsByEventKey(String eventKey);

    Optional<Notification> findByEventKey(String eventKey);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.readStatus = true,
                n.readAt = CURRENT_TIMESTAMP,
                n.updatedAt = CURRENT_TIMESTAMP
            WHERE n.receiverId = :receiverId
              AND n.deletedAt IS NULL
              AND n.readStatus = false
              AND (:category IS NULL OR :category = '' OR n.category = :category)
            """)
    int markAllAsRead(@Param("receiverId") Long receiverId, @Param("category") String category);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.deletedAt = CURRENT_TIMESTAMP,
                n.updatedAt = CURRENT_TIMESTAMP
            WHERE n.id = :id
              AND n.receiverId = :receiverId
              AND n.deletedAt IS NULL
            """)
    int softDeleteByIdAndReceiverId(@Param("id") Long id, @Param("receiverId") Long receiverId);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.deletedAt = CURRENT_TIMESTAMP,
                n.updatedAt = CURRENT_TIMESTAMP
            WHERE n.receiverId = :receiverId
              AND n.deletedAt IS NULL
            """)
    int softDeleteByReceiverId(@Param("receiverId") Long receiverId);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.businessStatus = :businessStatus,
                n.updatedAt = CURRENT_TIMESTAMP
            WHERE n.sourceType = :sourceType
              AND n.sourceId = :sourceId
              AND n.deletedAt IS NULL
            """)
    int updateBusinessStatusBySource(@Param("sourceType") String sourceType,
                                     @Param("sourceId") Long sourceId,
                                     @Param("businessStatus") String businessStatus);

    @Query("""
            SELECT n FROM Notification n
            WHERE n.deletedAt IS NULL
              AND (:receiverId IS NULL OR n.receiverId = :receiverId)
              AND (:senderId IS NULL OR n.senderId = :senderId)
              AND (:category IS NULL OR :category = '' OR n.category = :category)
              AND (:type IS NULL OR :type = '' OR n.type = :type)
              AND (:readStatus IS NULL OR n.readStatus = :readStatus)
              AND (:businessStatus IS NULL OR :businessStatus = '' OR n.businessStatus = :businessStatus)
              AND (:startTime IS NULL OR n.createdAt >= :startTime)
              AND (:endTime IS NULL OR n.createdAt <= :endTime)
            ORDER BY n.createdAt DESC
            """)
    Page<Notification> pageAdmin(@Param("receiverId") Long receiverId,
                                 @Param("senderId") Long senderId,
                                 @Param("category") String category,
                                 @Param("type") String type,
                                 @Param("readStatus") Boolean readStatus,
                                 @Param("businessStatus") String businessStatus,
                                 @Param("startTime") Instant startTime,
                                 @Param("endTime") Instant endTime,
                                 Pageable pageable);

    long countByDeletedAtIsNull();

    long countByReadStatusFalseAndDeletedAtIsNull();

    long countByCreatedAtGreaterThanEqualAndDeletedAtIsNull(Instant startTime);

    long countByCategoryAndDeletedAtIsNull(String category);

    long countByCategoryInAndDeletedAtIsNull(List<String> categories);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.deletedAt = CURRENT_TIMESTAMP,
                n.updatedAt = CURRENT_TIMESTAMP
            WHERE n.id = :id AND n.deletedAt IS NULL
            """)
    int adminSoftDeleteById(@Param("id") Long id);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.deletedAt = CURRENT_TIMESTAMP,
                n.updatedAt = CURRENT_TIMESTAMP
            WHERE n.id IN :ids AND n.deletedAt IS NULL
            """)
    int adminSoftDeleteByIds(@Param("ids") List<Long> ids);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.readStatus = true,
                n.readAt = CURRENT_TIMESTAMP,
                n.updatedAt = CURRENT_TIMESTAMP
            WHERE n.id = :id AND n.deletedAt IS NULL
            """)
    int adminMarkAsRead(@Param("id") Long id);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.readStatus = true,
                n.readAt = CURRENT_TIMESTAMP,
                n.updatedAt = CURRENT_TIMESTAMP
            WHERE n.id IN :ids AND n.deletedAt IS NULL
            """)
    int adminMarkAsReadByIds(@Param("ids") List<Long> ids);
}
