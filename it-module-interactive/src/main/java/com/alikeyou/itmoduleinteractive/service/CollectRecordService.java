package com.alikeyou.itmoduleinteractive.service;

import com.alikeyou.itmoduleinteractive.entity.CollectRecord;
import com.alikeyou.itmodulecommon.entity.UserInfo;

import java.util.List;

public interface CollectRecordService {

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