package com.alikeyou.itmoduleinteractive.repository;

import com.alikeyou.itmoduleinteractive.entity.LikeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRecordRepository extends JpaRepository<LikeRecord, Long> {
    // 根据用户ID、目标类型和目标ID查询点赞记录
    Optional<LikeRecord> findByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);

    // 根据目标类型和目标ID查询点赞记录列表
    List<LikeRecord> findByTargetTypeAndTargetId(String targetType, Long targetId);

    long countByTargetTypeAndTargetId(String targetType, Long targetId);

    long countByUserId(Long userId);

    // 根据用户ID查询点赞记录列表
    List<LikeRecord> findByUserId(Long userId);

    List<LikeRecord> findByUserIdAndCreatedAtBetween(Long userId, java.time.Instant start, java.time.Instant end);
    
    // 根据作者ID查询其所有博客收到的点赞记录（通过关联blog表）
    @Query("SELECT lr FROM LikeRecord lr WHERE lr.targetType = 'blog' AND lr.targetId IN (SELECT b.id FROM Blog b WHERE b.author.id = :authorId)")
    List<LikeRecord> findByTargetBlogAuthorId(@org.springframework.data.repository.query.Param("authorId") Long authorId);
}
