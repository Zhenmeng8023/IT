package com.alikeyou.itmoduleinteractive.support;

import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.repository.BlogRepository;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import com.alikeyou.itmoduleinteractive.entity.Comment;
import com.alikeyou.itmoduleinteractive.entity.Notification;
import com.alikeyou.itmoduleinteractive.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NotificationViewAssembler {

    private final UserInfoRepository userInfoRepository;
    private final BlogRepository blogRepository;
    private final CommentRepository commentRepository;

    public Notification enrich(Notification notification) {
        if (notification == null) {
            return null;
        }
        enrichAll(List.of(notification));
        return notification;
    }

    public List<Notification> enrichAll(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return notifications;
        }

        Map<Long, UserInfo> senderMap = loadSenders(notifications.stream()
                .map(Notification::getSenderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        Map<Long, Comment> commentMap = loadComments(notifications.stream()
                .filter(notification -> "comment".equalsIgnoreCase(notification.getTargetType()))
                .map(Notification::getTargetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        Set<Long> blogIds = notifications.stream()
                .filter(notification -> "blog".equalsIgnoreCase(notification.getTargetType()))
                .map(Notification::getTargetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        blogIds.addAll(commentMap.values().stream()
                .map(Comment::getPostId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        Map<Long, Blog> blogMap = loadBlogs(blogIds);

        notifications.forEach(notification -> {
            UserInfo sender = senderMap.get(notification.getSenderId());
            notification.setSenderName(getDisplayName(sender));
            notification.setSenderAvatar(sender != null ? sender.getAvatarUrl() : null);
            notification.setPreview(notification.getContent());
            notification.setActionText(resolveActionText(notification.getType()));

            if ("comment".equalsIgnoreCase(notification.getTargetType()) && notification.getTargetId() != null) {
                Comment comment = commentMap.get(notification.getTargetId());
                if (comment != null) {
                    notification.setCommentId(comment.getId());
                    notification.setBlogId(comment.getPostId());
                    Blog blog = blogMap.get(comment.getPostId());
                    notification.setTargetTitle(blog != null ? blog.getTitle() : "相关博客");
                } else {
                    notification.setCommentId(notification.getTargetId());
                }
            } else if ("blog".equalsIgnoreCase(notification.getTargetType()) && notification.getTargetId() != null) {
                notification.setBlogId(notification.getTargetId());
                Blog blog = blogMap.get(notification.getTargetId());
                notification.setTargetTitle(blog != null ? blog.getTitle() : "相关博客");
            } else if ("conversation".equalsIgnoreCase(notification.getTargetType())) {
                notification.setTargetTitle(notification.getTargetTitle() == null ? "私信会话" : notification.getTargetTitle());
            }
        });

        return notifications;
    }

    private Map<Long, UserInfo> loadSenders(Collection<Long> senderIds) {
        if (senderIds == null || senderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, UserInfo> senderMap = new HashMap<>();
        userInfoRepository.findAllById(senderIds).forEach(sender -> senderMap.put(sender.getId(), sender));
        return senderMap;
    }

    private Map<Long, Comment> loadComments(Collection<Long> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Comment> commentMap = new HashMap<>();
        commentRepository.findAllById(commentIds).forEach(comment -> commentMap.put(comment.getId(), comment));
        return commentMap;
    }

    private Map<Long, Blog> loadBlogs(Collection<Long> blogIds) {
        if (blogIds == null || blogIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return blogRepository.findByIdIn(blogIds.stream().toList()).stream()
                .collect(Collectors.toMap(Blog::getId, blog -> blog));
    }

    private String getDisplayName(UserInfo sender) {
        if (sender == null) {
            return "系统";
        }
        if (sender.getNickname() != null && !sender.getNickname().isBlank()) {
            return sender.getNickname();
        }
        if (sender.getUsername() != null && !sender.getUsername().isBlank()) {
            return sender.getUsername();
        }
        return "匿名用户";
    }

    private String resolveActionText(String type) {
        if (type == null) {
            return "给你带来了新动态";
        }
        return switch (type.toLowerCase()) {
            case "reply" -> "回复了你的评论";
            case "comment" -> "评论了你的博客";
            case "like" -> "点赞了你的内容";
            case "message" -> "发来了一条新消息";
            case "system" -> "发送了一条系统通知";
            default -> "给你带来了新动态";
        };
    }
}
