package com.alikeyou.itmoduleinteractive.service.impl;

import com.alikeyou.itmoduleinteractive.entiey.CollectRecord;
import com.alikeyou.itmoduleinteractive.repository.CollectRecordRepository;
import com.alikeyou.itmoduleinteractive.service.CollectRecordService;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class CollectRecordServiceImpl implements CollectRecordService {
    
    @Autowired
    private CollectRecordRepository collectRecordRepository;
    
    @Override
    public CollectRecord addCollect(UserInfo user, String targetType, Long targetId) {
        // 检查是否已收藏
        if (isCollected(user, targetType, targetId)) {
            throw new RuntimeException("Already collected");
        }
        
        // 创建收藏记录
        CollectRecord collectRecord = new CollectRecord();
        collectRecord.setUser(user);
        collectRecord.setTargetType(targetType);
        collectRecord.setTargetId(targetId);
        collectRecord.setCreatedAt(Instant.now());
        
        return collectRecordRepository.save(collectRecord);
    }
    
    @Override
    public void removeCollect(UserInfo user, String targetType, Long targetId) {
        collectRecordRepository.deleteByUserAndTargetTypeAndTargetId(user, targetType, targetId);
    }
    
    @Override
    public boolean isCollected(UserInfo user, String targetType, Long targetId) {
        return collectRecordRepository.findByUserAndTargetTypeAndTargetId(user, targetType, targetId).isPresent();
    }
    
    @Override
    public List<CollectRecord> getCollections(UserInfo user, String targetType) {
        return collectRecordRepository.findByUserAndTargetType(user, targetType);
    }
}