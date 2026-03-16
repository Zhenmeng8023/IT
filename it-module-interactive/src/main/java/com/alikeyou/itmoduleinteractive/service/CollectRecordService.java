package com.alikeyou.itmoduleinteractive.service;

import com.alikeyou.itmoduleinteractive.entity.CollectRecord;
import com.alikeyou.itmodulecommon.entity.UserInfo;

import java.util.List;
import java.util.Optional;

public interface CollectRecordService {

    /**
     * 获取所有收藏记录
     */
    List<CollectRecord> getAllCollectRecords();

    /**
     * 创建收藏记录
     */
    CollectRecord createCollectRecord(CollectRecord collectRecord);

    /**
     * 删除收藏记录
     */
    void deleteCollectRecord(Long id);

    /**
     * 根据ID获取收藏记录
     */
    Optional<CollectRecord> getCollectRecordById(Long id);

    /**
     * 根据用户ID、目标类型和目标ID获取收藏记录
     */
    Optional<CollectRecord> getCollectRecordByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);

    /**
     * 根据目标类型和目标ID获取收藏记录列表
     */
    List<CollectRecord> getCollectRecordsByTargetTypeAndTargetId(String targetType, Long targetId);

    /**
     * 根据用户ID获取收藏记录列表
     */
    List<CollectRecord> getCollectRecordsByUserId(Long userId);

    /**
     * 添加收藏
     */
    CollectRecord addCollect(UserInfo user, String targetType, Long targetId);

    /**
     * 取消收藏
     */
    void removeCollect(UserInfo user, String targetType, Long targetId);

    /**
     * 检查是否已收藏
     */
    boolean isCollected(UserInfo user, String targetType, Long targetId);

    /**
     * 获取用户的收藏列表
     */
    List<CollectRecord> getCollections(UserInfo user, String targetType);
}