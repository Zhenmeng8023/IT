package com.alikeyou.itmoduleinteractive.repository;

import com.alikeyou.itmoduleinteractive.entity.LikeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRecordRepository extends JpaRepository<LikeRecord, Long> {
    // 根据用户ID、目标类型和目标ID查询点赞记录
    Optional<LikeRecord> findByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);

    // 根据目标类型和目标ID查询点赞记录列表
    List<LikeRecord> findByTargetTypeAndTargetId(String targetType, Long targetId);

    // 根据用户ID查询点赞记录列表
    List<LikeRecord> findByUserId(Long userId);
}
