package com.alikeyou.itmoduleinteractive.repository;

import com.alikeyou.itmoduleinteractive.entity.CollectRecord;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollectRecordRepository extends JpaRepository<CollectRecord, Long> {

    /**
     * 检查用户是否已收藏指定目标
     */
    Optional<CollectRecord> findByUserAndTargetTypeAndTargetId(UserInfo user, String targetType, Long targetId);

    /**
     * 获取用户的所有收藏记录
     */
    List<CollectRecord> findByUserAndTargetType(UserInfo user, String targetType);

    /**
     * 删除用户对指定目标的收藏
     */
    void deleteByUserAndTargetTypeAndTargetId(UserInfo user, String targetType, Long targetId);
}