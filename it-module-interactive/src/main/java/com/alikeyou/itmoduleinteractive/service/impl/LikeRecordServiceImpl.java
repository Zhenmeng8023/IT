package com.alikeyou.itmoduleinteractive.service.impl;

import com.alikeyou.itmodulecommon.notification.NotificationCreateCommand;
import com.alikeyou.itmodulecommon.notification.NotificationPublisher;
import com.alikeyou.itmoduleinteractive.entity.Comment;
import com.alikeyou.itmoduleinteractive.entity.LikeRecord;
import com.alikeyou.itmoduleinteractive.repository.CommentRepository;
import com.alikeyou.itmoduleinteractive.repository.LikeRecordRepository;
import com.alikeyou.itmoduleinteractive.service.LikeRecordService;
import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.repository.BlogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class LikeRecordServiceImpl implements LikeRecordService {

    @Autowired
    private LikeRecordRepository likeRecordRepository;

    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private NotificationPublisher notificationPublisher;

    @Override
    public List<LikeRecord> getAllLikeRecords() {
        return likeRecordRepository.findAll();
    }

    @Override
    public Optional<LikeRecord> getLikeRecordById(Long id) {
        return likeRecordRepository.findById(id);
    }

    @Override
    public LikeRecord createLikeRecord(LikeRecord likeRecord) {
        Optional<LikeRecord> existingRecord = likeRecordRepository.findByUserIdAndTargetTypeAndTargetId(
                likeRecord.getUserId(),
                likeRecord.getTargetType(),
                likeRecord.getTargetId()
        );
        if (existingRecord.isPresent()) {
            return existingRecord.get();
        }

        LikeRecord savedLikeRecord = likeRecordRepository.save(likeRecord);
        // 根据目标类型更新对应表的点赞数
        if ("comment".equals(likeRecord.getTargetType())) {
            updateCommentLikes(likeRecord.getTargetId());
        } else if ("blog".equals(likeRecord.getTargetType())) {
            updateBlogLikes(likeRecord.getTargetId());
        }
        publishLikeNotification(savedLikeRecord);
        return savedLikeRecord;
    }

    @Override
    public void deleteLikeRecord(Long id) {
        Optional<LikeRecord> likeRecordOptional = likeRecordRepository.findById(id);
        if (likeRecordOptional.isPresent()) {
            LikeRecord likeRecord = likeRecordOptional.get();
            likeRecordRepository.deleteById(id);
            // 根据目标类型更新对应表的点赞数
            if ("comment".equals(likeRecord.getTargetType())) {
                updateCommentLikes(likeRecord.getTargetId());
            } else if ("blog".equals(likeRecord.getTargetType())) {
                updateBlogLikes(likeRecord.getTargetId());
            }
        }
    }

    @Override
    public Optional<LikeRecord> getLikeRecordByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId) {
        return likeRecordRepository.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId);
    }

    @Override
    public List<LikeRecord> getLikeRecordsByTargetTypeAndTargetId(String targetType, Long targetId) {
        return likeRecordRepository.findByTargetTypeAndTargetId(targetType, targetId);
    }

    @Override
    public List<LikeRecord> getLikeRecordsByUserId(Long userId) {
        return likeRecordRepository.findByUserId(userId);
    }
    
    @Override
    public List<LikeRecord> getLikeRecordsReceivedByAuthor(Long authorId) {
        return likeRecordRepository.findByTargetBlogAuthorId(authorId);
    }

    /**
     * 更新评论的点赞数
     * @param commentId 评论ID
     */
    private void updateCommentLikes(Long commentId) {
        Optional<Comment> commentOptional = commentRepository.findById(commentId);
        if (commentOptional.isPresent()) {
            Comment comment = commentOptional.get();
            long likeCount = likeRecordRepository.countByTargetTypeAndTargetId("comment", commentId);
            comment.setLikes((int) likeCount);
            commentRepository.save(comment);
        }
    }
    
    /**
     * 更新博客的点赞数
     * @param blogId 博客ID
     */
    private void updateBlogLikes(Long blogId) {
        Optional<Blog> blogOptional = blogRepository.findById(blogId);
        if (blogOptional.isPresent()) {
            Blog blog = blogOptional.get();
            long likeCount = likeRecordRepository.countByTargetTypeAndTargetId("blog", blogId);
            blog.setLikeCount((int) likeCount);
            blogRepository.save(blog);
        }
    }

    private void publishLikeNotification(LikeRecord likeRecord) {
        if (likeRecord == null || likeRecord.getId() == null || likeRecord.getUserId() == null) {
            return;
        }

        String targetType = likeRecord.getTargetType();
        Long targetId = likeRecord.getTargetId();
        if ("blog".equalsIgnoreCase(targetType)) {
            blogRepository.findWithAssociationsById(targetId).ifPresent(blog -> {
                Long receiverId = blog.getAuthor() == null ? null : blog.getAuthor().getId();
                if (receiverId == null || Objects.equals(receiverId, likeRecord.getUserId())) {
                    return;
                }
                notificationPublisher.publish(NotificationCreateCommand.builder()
                        .receiverId(receiverId)
                        .senderId(likeRecord.getUserId())
                        .category("interaction")
                        .type("like")
                        .title("新的点赞")
                        .content("点赞了你的博客《" + safeBlogTitle(blog) + "》")
                        .targetType("blog")
                        .targetId(blog.getId())
                        .sourceType("like_record")
                        .sourceId(likeRecord.getId())
                        .eventKey("like:blog:" + blog.getId() + ":user:" + likeRecord.getUserId())
                        .actionUrl("/blog/" + blog.getId())
                        .payload(Map.of("blogId", blog.getId(), "targetTitle", safeBlogTitle(blog)))
                        .build());
            });
            return;
        }

        if ("comment".equalsIgnoreCase(targetType)) {
            commentRepository.findById(targetId).ifPresent(comment -> {
                Long receiverId = comment.getAuthorId();
                if (receiverId == null || Objects.equals(receiverId, likeRecord.getUserId())) {
                    return;
                }
                Long blogId = comment.getPostId();
                notificationPublisher.publish(NotificationCreateCommand.builder()
                        .receiverId(receiverId)
                        .senderId(likeRecord.getUserId())
                        .category("interaction")
                        .type("like")
                        .title("新的点赞")
                        .content("点赞了你的评论：" + preview(comment.getContent()))
                        .targetType("blog")
                        .targetId(blogId)
                        .sourceType("like_record")
                        .sourceId(likeRecord.getId())
                        .eventKey("like:comment:" + comment.getId() + ":user:" + likeRecord.getUserId())
                        .actionUrl("/blog/" + blogId + "?commentId=" + comment.getId() + "&highlight=true")
                        .payload(Map.of("blogId", blogId, "commentId", comment.getId()))
                        .build());
            });
        }
    }

    private String safeBlogTitle(Blog blog) {
        if (blog == null || blog.getTitle() == null || blog.getTitle().isBlank()) {
            return "相关博客";
        }
        return blog.getTitle();
    }

    private String preview(String content) {
        String normalized = content == null ? "" : content.trim().replaceAll("\\s+", " ");
        return normalized.length() > 60 ? normalized.substring(0, 60) + "..." : normalized;
    }
}
