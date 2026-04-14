package com.alikeyou.itmoduleinteractive.service.impl;

import com.alikeyou.itmoduleinteractive.entity.CollectRecord;
import com.alikeyou.itmoduleinteractive.repository.CollectRecordRepository;
import com.alikeyou.itmoduleinteractive.service.CollectRecordService;
import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmodulecommon.notification.NotificationCreateCommand;
import com.alikeyou.itmodulecommon.notification.NotificationPublisher;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmoduleblog.repository.BlogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class CollectRecordServiceImpl implements CollectRecordService {
    
    @Autowired
    private CollectRecordRepository collectRecordRepository;
    
    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private NotificationPublisher notificationPublisher;
    
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
            updateBlogCollects(collectRecord.getTargetId());
            publishBlogCollectNotification(savedRecord);
        }
        
        return savedRecord;
    }

    @Override
    public void deleteCollectRecord(Long id) {
        Optional<CollectRecord> collectRecord = collectRecordRepository.findById(id);
        if (collectRecord.isPresent()) {
            CollectRecord record = collectRecord.get();
            Long targetId = record.getTargetId();
            String targetType = record.getTargetType();
            collectRecordRepository.deleteById(id);
            // 如果是取消收藏博客，更新博客的收藏数
            if ("blog".equals(targetType)) {
                updateBlogCollects(targetId);
            }
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
            updateBlogCollects(targetId);
            publishBlogCollectNotification(savedRecord);
        }
        
        return savedRecord;
    }
    
    @Override
    public void removeCollect(UserInfo user, String targetType, Long targetId) {
        boolean existed = collectRecordRepository.findByUserIdAndTargetTypeAndTargetId(user.getId(), targetType, targetId).isPresent();
        if (!existed) {
            return;
        }

        collectRecordRepository.deleteByUserIdAndTargetTypeAndTargetId(user.getId(), targetType, targetId);
        
        // 如果是取消收藏博客，更新博客的收藏数
        if ("blog".equals(targetType)) {
            updateBlogCollects(targetId);
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
    
    @Override
    public List<CollectRecord> getCollectRecordsReceivedByAuthor(Long authorId) {
        return collectRecordRepository.findByTargetBlogAuthorId(authorId);
    }

    private void updateBlogCollects(Long blogId) {
        blogRepository.findById(blogId).ifPresent(blog -> {
            long collectCount = collectRecordRepository.countByTargetTypeAndTargetId("blog", blogId);
            blog.setCollectCount((int) collectCount);
            blogRepository.save(blog);
        });
    }

    private void publishBlogCollectNotification(CollectRecord collectRecord) {
        if (collectRecord == null || collectRecord.getId() == null || collectRecord.getUserId() == null) {
            return;
        }
        blogRepository.findWithAssociationsById(collectRecord.getTargetId()).ifPresent(blog -> {
            Long receiverId = blog.getAuthor() == null ? null : blog.getAuthor().getId();
            if (receiverId == null || Objects.equals(receiverId, collectRecord.getUserId())) {
                return;
            }
            notificationPublisher.publish(NotificationCreateCommand.builder()
                    .receiverId(receiverId)
                    .senderId(collectRecord.getUserId())
                    .category("interaction")
                    .type("collect")
                    .title("新的收藏")
                    .content("收藏了你的博客《" + safeBlogTitle(blog) + "》")
                    .targetType("blog")
                    .targetId(blog.getId())
                    .sourceType("collect_record")
                    .sourceId(collectRecord.getId())
                    .eventKey("collect:blog:" + blog.getId() + ":user:" + collectRecord.getUserId())
                    .actionUrl("/blog/" + blog.getId())
                    .payload(Map.of("blogId", blog.getId(), "targetTitle", safeBlogTitle(blog)))
                    .build());
        });
    }

    private String safeBlogTitle(Blog blog) {
        if (blog == null || blog.getTitle() == null || blog.getTitle().isBlank()) {
            return "相关博客";
        }
        return blog.getTitle();
    }
}
