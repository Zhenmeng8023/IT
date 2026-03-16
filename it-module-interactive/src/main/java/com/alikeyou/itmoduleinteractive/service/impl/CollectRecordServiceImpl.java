package com.alikeyou.itmoduleinteractive.service.impl;

import com.alikeyou.itmoduleinteractive.entity.CollectRecord;
import com.alikeyou.itmoduleinteractive.repository.CollectRecordRepository;
import com.alikeyou.itmoduleinteractive.service.CollectRecordService;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmoduleblog.repository.BlogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CollectRecordServiceImpl implements CollectRecordService {
    
    @Autowired
    private CollectRecordRepository collectRecordRepository;
    
    @Autowired
    private BlogRepository blogRepository;
    
    @Override
    public List<CollectRecord> getAllCollectRecords() {
        return collectRecordRepository.findAll();
    }

    @Override
    public CollectRecord createCollectRecord(CollectRecord collectRecord) {
        // 检查是否已收藏
        if (collectRecordRepository.findByUserIdAndTargetTypeAndTargetId(
                collectRecord.getUserId(), 
                collectRecord.getTargetType(), 
                collectRecord.getTargetId()).isPresent()) {
            throw new RuntimeException("Already collected");
        }
        
        collectRecord.setCreatedAt(Instant.now());
        CollectRecord savedRecord = collectRecordRepository.save(collectRecord);
        
        // 如果是收藏博客，更新博客的收藏数
        if ("blog".equals(collectRecord.getTargetType())) {
            blogRepository.incrementCollectCount(collectRecord.getTargetId());
        }
        
        return savedRecord;
    }

    @Override
    public void deleteCollectRecord(Long id) {
        Optional<CollectRecord> collectRecord = collectRecordRepository.findById(id);
        if (collectRecord.isPresent()) {
            CollectRecord record = collectRecord.get();
            // 如果是取消收藏博客，更新博客的收藏数
            if ("blog".equals(record.getTargetType())) {
                blogRepository.decrementCollectCount(record.getTargetId());
            }
            collectRecordRepository.deleteById(id);
        }
    }

    @Override
    public Optional<CollectRecord> getCollectRecordById(Long id) {
        return collectRecordRepository.findById(id);
    }

    @Override
    public Optional<CollectRecord> getCollectRecordByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId) {
        return collectRecordRepository.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId);
    }

    @Override
    public List<CollectRecord> getCollectRecordsByTargetTypeAndTargetId(String targetType, Long targetId) {
        return collectRecordRepository.findByTargetTypeAndTargetId(targetType, targetId);
    }

    @Override
    public List<CollectRecord> getCollectRecordsByUserId(Long userId) {
        return collectRecordRepository.findByUserId(userId);
    }
    
    @Override
    public CollectRecord addCollect(UserInfo user, String targetType, Long targetId) {
        // 检查是否已收藏
        if (isCollected(user, targetType, targetId)) {
            throw new RuntimeException("Already collected");
        }
        
        // 创建收藏记录
        CollectRecord collectRecord = new CollectRecord();
        collectRecord.setUserId(user.getId());
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
        collectRecordRepository.deleteByUserIdAndTargetTypeAndTargetId(user.getId(), targetType, targetId);
        
        // 如果是取消收藏博客，更新博客的收藏数
        if ("blog".equals(targetType)) {
            blogRepository.decrementCollectCount(targetId);
        }
    }
    
    @Override
    public boolean isCollected(UserInfo user, String targetType, Long targetId) {
        return collectRecordRepository.findByUserIdAndTargetTypeAndTargetId(user.getId(), targetType, targetId).isPresent();
    }
    
    @Override
    public List<CollectRecord> getCollections(UserInfo user, String targetType) {
        return collectRecordRepository.findByUserIdAndTargetType(user.getId(), targetType);
    }
}