package com.alikeyou.itmoduleinteractive.service;

import com.alikeyou.itmoduleinteractive.entity.LikeRecord;
import java.util.List;
import java.util.Optional;

public interface LikeRecordService {
    // 获取所有点赞记录
    List<LikeRecord> getAllLikeRecords();
    
    // 根据ID获取点赞记录
    Optional<LikeRecord> getLikeRecordById(Long id);
    
    // 创建点赞记录
    LikeRecord createLikeRecord(LikeRecord likeRecord);
    
    // 删除点赞记录
    void deleteLikeRecord(Long id);
    
    // 根据用户ID、目标类型和目标ID获取点赞记录
    Optional<LikeRecord> getLikeRecordByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);
    
    // 根据目标类型和目标ID获取点赞记录列表
    List<LikeRecord> getLikeRecordsByTargetTypeAndTargetId(String targetType, Long targetId);
    
    // 根据用户ID获取点赞记录列表
    List<LikeRecord> getLikeRecordsByUserId(Long userId);
    
    // 根据作者ID获取其博客收到的点赞记录列表
    List<LikeRecord> getLikeRecordsReceivedByAuthor(Long authorId);
}