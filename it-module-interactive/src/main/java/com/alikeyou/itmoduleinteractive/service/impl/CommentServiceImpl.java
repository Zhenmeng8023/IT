package com.alikeyou.itmoduleinteractive.service.impl;

import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.repository.BlogRepository;
import com.alikeyou.itmodulecommon.constant.LoginConstant;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.notification.NotificationCreateCommand;
import com.alikeyou.itmodulecommon.notification.NotificationPublisher;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import com.alikeyou.itmoduleinteractive.entity.Comment;
import com.alikeyou.itmoduleinteractive.entity.LikeRecord;
import com.alikeyou.itmoduleinteractive.repository.CommentRepository;
import com.alikeyou.itmoduleinteractive.repository.LikeRecordRepository;
import com.alikeyou.itmoduleinteractive.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {

    private static final String STATUS_NORMAL = "normal";
    private static final String STATUS_DELETED = "deleted";
    private static final String DELETED_CONTENT = "该评论已删除";
    private static final String POST_TYPE_BLOG = "blog";
    private static final Set<Integer> ADMIN_ROLE_IDS = Set.of(1, 2);

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private LikeRecordRepository likeRecordRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private NotificationPublisher notificationPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<Comment> getAllComments() {
        List<Comment> comments = commentRepository.findAllByOrderByCreatedAtDesc();
        enrichComments(comments, getCurrentUserIdOrNull());
        return comments;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Comment> getCommentById(Long id) {
        Optional<Comment> commentOptional = commentRepository.findById(id);
        commentOptional.ifPresent(comment -> enrichComments(List.of(comment), getCurrentUserIdOrNull()));
        return commentOptional;
    }

    @Override
    @Transactional
    public Comment saveComment(Comment comment) {
        if (comment == null) {
            throw badRequest("评论参数不能为空");
        }

        Long actorId = resolveActorId(comment.getAuthorId());
        requireText(comment.getContent(), "评论内容不能为空");
        Blog blog = requireBlog(comment.getPostId());

        comment.setAuthorId(actorId);
        comment.setContent(comment.getContent().trim());
        comment.setPostType(resolvePostType(comment.getPostType()));
        comment.setLikes(0);
        comment.setStatus(STATUS_NORMAL);
        if (comment.getCreatedAt() == null) {
            comment.setCreatedAt(Instant.now());
        }

        if (comment.getParentComment() != null && comment.getParentComment().getId() != null) {
            Comment parentComment = commentRepository.findById(comment.getParentComment().getId())
                    .orElseThrow(() -> notFound("父评论不存在，ID: " + comment.getParentComment().getId()));
            if (!Objects.equals(parentComment.getPostId(), blog.getId())) {
                throw badRequest("父评论不属于当前博客");
            }
            if (isDeleted(parentComment)) {
                throw badRequest("已删除的评论不能继续回复");
            }
            comment.setParentComment(parentComment);
        } else {
            comment.setParentComment(null);
        }

        Comment savedComment = commentRepository.save(comment);
        createNotificationsForComment(savedComment, blog, actorId);
        enrichComments(List.of(savedComment), actorId);
        return savedComment;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Comment> getCommentsByPostId(Long postId) {
        requireBlog(postId);
        List<Comment> comments = commentRepository.findByPostIdAndPostTypeOrderByCreatedAtAsc(postId, POST_TYPE_BLOG);
        enrichComments(comments, getCurrentUserIdOrNull());
        return comments;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Comment> getRepliesByCommentId(Long commentId) {
        getRequiredComment(commentId);
        List<Comment> comments = commentRepository.findByParentComment_IdOrderByCreatedAtAsc(commentId);
        enrichComments(comments, getCurrentUserIdOrNull());
        return comments;
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        Long currentUserId = requireCurrentUserId();
        Comment comment = getRequiredComment(commentId);
        Blog blog = requireBlog(comment.getPostId());
        if (!canDelete(comment.getAuthorId(), blog, currentUserId)) {
            throw forbidden("无权删除该评论");
        }
        if (isDeleted(comment)) {
            return;
        }

        comment.setStatus(STATUS_DELETED);
        comment.setContent(DELETED_CONTENT);
        commentRepository.save(comment);
    }

    private void updateCommentLikes(Comment comment) {
        List<LikeRecord> likeRecords = likeRecordRepository.findByTargetTypeAndTargetId("comment", comment.getId());
        comment.setLikes(likeRecords.size());
    }

    private void createNotificationsForComment(Comment savedComment, Blog blog, Long actorId) {
        if (savedComment == null || actorId == null) {
            return;
        }

        Long blogAuthorId = blog != null && blog.getAuthor() != null ? blog.getAuthor().getId() : null;
        Comment parentComment = savedComment.getParentComment();
        String preview = buildNotificationPreview(savedComment.getContent());

        if (parentComment == null || parentComment.getId() == null) {
            sendCommentNotification(blogAuthorId, actorId, "comment", "新的评论",
                    "评论了你的博客《" + safeBlogTitle(blog) + "》：" + preview,
                    savedComment, blog);
            return;
        }

        sendCommentNotification(parentComment.getAuthorId(), actorId, "reply", "新的回复",
                "回复了你的评论：" + preview, savedComment, blog);

        if (blogAuthorId != null && !Objects.equals(blogAuthorId, parentComment.getAuthorId())) {
            sendCommentNotification(blogAuthorId, actorId, "comment", "博客收到新回复",
                    "在你的博客《" + safeBlogTitle(blog) + "》下回复了评论：" + preview,
                    savedComment, blog);
        }
    }

    private void sendCommentNotification(Long receiverId, Long senderId, String type, String title,
                                         String content, Comment comment, Blog blog) {
        if (receiverId == null || senderId == null || comment == null || comment.getId() == null || Objects.equals(receiverId, senderId)) {
            return;
        }

        Long blogId = blog == null ? comment.getPostId() : blog.getId();
        notificationPublisher.publish(NotificationCreateCommand.builder()
                .receiverId(receiverId)
                .senderId(senderId)
                .category("interaction")
                .type(type)
                .title(title)
                .content(content)
                .targetType("blog")
                .targetId(blogId)
                .sourceType("comment")
                .sourceId(comment.getId())
                .eventKey("comment:" + comment.getId() + ":" + type + ":receiver:" + receiverId)
                .actionUrl("/blog/" + blogId + "?commentId=" + comment.getId() + "&highlight=true")
                .payload(Map.of(
                        "blogId", blogId,
                        "commentId", comment.getId(),
                        "targetTitle", safeBlogTitle(blog)
                ))
                .build());
    }

    private String buildNotificationPreview(String content) {
        String normalized = content == null ? "" : content.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= 60) {
            return normalized;
        }
        return normalized.substring(0, 60) + "...";
    }

    private String safeBlogTitle(Blog blog) {
        if (blog == null || blog.getTitle() == null || blog.getTitle().isBlank()) {
            return "相关博客";
        }
        return blog.getTitle();
    }

    private void enrichComments(List<Comment> comments, Long currentUserId) {
        if (comments == null || comments.isEmpty()) {
            return;
        }

        Map<Long, UserInfo> authorMap = loadAuthors(comments.stream()
                .map(Comment::getAuthorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        Map<Long, Blog> blogMap = loadBlogs(comments.stream()
                .map(Comment::getPostId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());

        comments.forEach(comment -> {
            updateCommentLikes(comment);

            String status = defaultStatus(comment.getStatus());
            comment.setStatus(status);

            boolean deleted = isDeleted(comment);
            comment.setDeleted(deleted);

            UserInfo author = authorMap.get(comment.getAuthorId());
            if (author != null) {
                comment.setUsername(author.getUsername());
                comment.setNickname(getDisplayName(author));
                comment.setAvatar(author.getAvatarUrl());
            } else {
                comment.setNickname("匿名用户");
                comment.setAvatar(null);
            }

            Blog blog = blogMap.get(comment.getPostId());
            comment.setCanDelete(!deleted && canDelete(comment.getAuthorId(), blog, currentUserId));
        });
    }

    private Map<Long, UserInfo> loadAuthors(Collection<Long> authorIds) {
        if (authorIds == null || authorIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, UserInfo> authorMap = new LinkedHashMap<>();
        userInfoRepository.findAllById(authorIds).forEach(user -> authorMap.put(user.getId(), user));
        return authorMap;
    }

    private Map<Long, Blog> loadBlogs(Collection<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return blogRepository.findByIdIn(postIds.stream().toList()).stream()
                .collect(Collectors.toMap(Blog::getId, blog -> blog));
    }

    private Blog requireBlog(Long postId) {
        if (postId == null) {
            throw badRequest("postId 不能为空");
        }
        return blogRepository.findWithAssociationsById(postId)
                .orElseThrow(() -> notFound("博客不存在，ID: " + postId));
    }

    private Comment getRequiredComment(Long commentId) {
        if (commentId == null) {
            throw badRequest("评论ID不能为空");
        }
        return commentRepository.findById(commentId)
                .orElseThrow(() -> notFound("评论不存在，ID: " + commentId));
    }

    private Long resolveActorId(Long fallbackAuthorId) {
        Long currentUserId = getCurrentUserIdOrNull();
        if (currentUserId != null) {
            return currentUserId;
        }
        if (fallbackAuthorId != null) {
            return fallbackAuthorId;
        }
        throw unauthorized("请先登录后再评论");
    }

    private Long requireCurrentUserId() {
        Long currentUserId = getCurrentUserIdOrNull();
        if (currentUserId == null) {
            throw unauthorized("请先登录后再操作");
        }
        return currentUserId;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw badRequest(message);
        }
        return value.trim();
    }

    private String resolvePostType(String postType) {
        if (!StringUtils.hasText(postType)) {
            return POST_TYPE_BLOG;
        }
        return postType.trim().toLowerCase();
    }

    private boolean isDeleted(Comment comment) {
        return STATUS_DELETED.equalsIgnoreCase(defaultStatus(comment.getStatus()));
    }

    private String defaultStatus(String status) {
        return (status == null || status.isBlank()) ? STATUS_NORMAL : status.trim();
    }

    private boolean canDelete(Long commentAuthorId, Blog blog, Long currentUserId) {
        if (currentUserId == null) {
            return false;
        }
        Long blogAuthorId = blog != null && blog.getAuthor() != null ? blog.getAuthor().getId() : null;
        return Objects.equals(commentAuthorId, currentUserId)
                || Objects.equals(blogAuthorId, currentUserId)
                || isAdmin();
    }

    private boolean isAdmin() {
        Long currentUserId = getCurrentUserIdOrNull();
        Integer roleId = LoginConstant.getRoleId();
        if (roleId == null && currentUserId != null) {
            roleId = userInfoRepository.findById(currentUserId)
                    .map(UserInfo::getRoleId)
                    .orElse(null);
        }
        return roleId != null && ADMIN_ROLE_IDS.contains(roleId);
    }

    private Long getCurrentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return LoginConstant.getUserId();
        }

        Long fromPrincipal = tryExtractUserId(authentication.getPrincipal());
        if (fromPrincipal != null) {
            return fromPrincipal;
        }

        Long fromDetails = tryExtractUserId(authentication.getDetails());
        if (fromDetails != null) {
            return fromDetails;
        }

        Long fromName = tryExtractUserId(authentication.getName());
        return fromName != null ? fromName : LoginConstant.getUserId();
    }

    private Long tryExtractUserId(Object source) {
        if (source == null) {
            return null;
        }
        if (source instanceof Number number) {
            return number.longValue();
        }
        if (source instanceof CharSequence text) {
            String value = text.toString().trim();
            if (!StringUtils.hasText(value) || "anonymousUser".equalsIgnoreCase(value)) {
                return null;
            }
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                return userInfoRepository.findByUsername(value)
                        .map(UserInfo::getId)
                        .orElse(null);
            }
        }
        return null;
    }

    private String getDisplayName(UserInfo user) {
        if (user == null) {
            return "匿名用户";
        }
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return "匿名用户";
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }

    private ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
