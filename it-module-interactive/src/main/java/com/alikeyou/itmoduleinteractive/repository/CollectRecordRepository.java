package com.alikeyou.itmoduleinteractive.repository;

import com.alikeyou.itmoduleinteractive.entity.CollectRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollectRecordRepository extends JpaRepository<CollectRecord, Long> {
    
    // 根据用户ID、目标类型和目标ID获取收藏记录
    @Query("SELECT c FROM CollectRecord c WHERE c.userId = :userId AND c.targetType = :targetType AND c.targetId = :targetId")
    Optional<CollectRecord> findByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);
    
    // 根据用户ID和目标类型获取收藏记录列表
    @Query("SELECT c FROM CollectRecord c WHERE c.userId = :userId AND c.targetType = :targetType")
    List<CollectRecord> findByUserIdAndTargetType(Long userId, String targetType);
    
    // 根据用户ID获取收藏记录列表
    @Query("SELECT c FROM CollectRecord c WHERE c.userId = :userId")
    List<CollectRecord> findByUserId(Long userId);

    long countByUserId(Long userId);

    List<CollectRecord> findByUserIdAndCreatedAtBetween(Long userId, java.time.Instant start, java.time.Instant end);
    
    // 根据目标类型和目标ID获取收藏记录列表
    @Query("SELECT c FROM CollectRecord c WHERE c.targetType = :targetType AND c.targetId = :targetId")
    List<CollectRecord> findByTargetTypeAndTargetId(String targetType, Long targetId);

    long countByTargetTypeAndTargetId(String targetType, Long targetId);
    
    // 根据用户ID、目标类型和目标ID删除收藏记录
    void deleteByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);
}
