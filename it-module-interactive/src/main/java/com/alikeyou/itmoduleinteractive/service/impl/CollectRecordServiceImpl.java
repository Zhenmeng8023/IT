package com.alikeyou.itmoduleinteractive.service.impl;

import com.alikeyou.itmoduleinteractive.entiey.CollectRecord;
import com.alikeyou.itmoduleinteractive.repository.CollectRecordRepository;
import com.alikeyou.itmoduleinteractive.service.CollectRecordService;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmoduleblog.repository.BlogRepository;
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
    
    @Autowired
    private BlogRepository blogRepository;
    
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
        
        // 保存收藏记录
        CollectRecord savedRecord = collectRecordRepository.save(collectRecord);
        
        // 如果是收藏博客，更新博客的收藏数
        if ("blog".equals(targetType)) {
            blogRepository.incrementCollectCount(targetId);
        }
        
        return savedRecord;
    }
    
    @Override
    public void removeCollect(UserInfo user, String targetType, Long targetId) {
        // 删除收藏记录
        collectRecordRepository.deleteByUserAndTargetTypeAndTargetId(user, targetType, targetId);
        
        // 如果是取消收藏博客，更新博客的收藏数
        if ("blog".equals(targetType)) {
            blogRepository.decrementCollectCount(targetId);
        }
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