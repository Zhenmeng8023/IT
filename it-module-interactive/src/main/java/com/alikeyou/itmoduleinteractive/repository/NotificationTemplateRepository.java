package com.alikeyou.itmoduleinteractive.repository;

import com.alikeyou.itmoduleinteractive.entity.NotificationTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByCodeAndDeletedAtIsNull(String code);

    @Query("""
            SELECT t FROM NotificationTemplate t
            WHERE t.deletedAt IS NULL
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(t.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(t.titleTemplate) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:category IS NULL OR :category = '' OR t.category = :category)
              AND (:type IS NULL OR :type = '' OR t.type = :type)
              AND (:enabled IS NULL OR t.enabled = :enabled)
            ORDER BY t.updatedAt DESC, t.id DESC
            """)
    Page<NotificationTemplate> pageTemplates(@Param("keyword") String keyword,
                                             @Param("category") String category,
                                             @Param("type") String type,
                                             @Param("enabled") Boolean enabled,
                                             Pageable pageable);

    @Modifying
    @Query("""
            UPDATE NotificationTemplate t
            SET t.deletedAt = CURRENT_TIMESTAMP,
                t.updatedAt = CURRENT_TIMESTAMP
            WHERE t.id = :id AND t.deletedAt IS NULL
            """)
    int softDeleteById(@Param("id") Long id);
}
