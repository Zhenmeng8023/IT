package com.alikeyou.itmodulecircle.service.impl;

import com.alikeyou.itmodulecircle.dto.CircleCommentRequest;
import com.alikeyou.itmodulecircle.dto.CircleCommentResponse;
import com.alikeyou.itmodulecircle.entity.CircleComment;
import com.alikeyou.itmodulecircle.exception.CircleException;
import com.alikeyou.itmodulecircle.repository.CircleCommentRepository;
import com.alikeyou.itmodulecircle.service.CircleCommentService;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CircleCommentServiceImpl implements CircleCommentService {

    private static final String POST_STATUS_PENDING = "pending";
    private static final String POST_STATUS_PUBLISHED = "published";
    private static final String POST_STATUS_DELETED = "deleted";

    @Autowired
    private CircleCommentRepository circleCommentRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public CircleComment createComment(CircleCommentRequest request) {
        if (request.getCircleId() == null) {
            throw new CircleException("圈子 ID 不能为空");
        }

        if (request.getAuthorId() == null) {
            throw new CircleException("作者 ID 不能为空");
        }

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new CircleException("评论内容不能为空");
        }

        UserInfo author = userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new CircleException("用户不存在，ID: " + request.getAuthorId()));

        CircleComment comment = new CircleComment();
        comment.setContent(request.getContent());
        comment.setAuthor(author);
        comment.setCircleId(request.getCircleId());
        comment.setParentCommentId(request.getParentCommentId());
        comment.setCreatedAt(Instant.now());

        if (request.getParentCommentId() == null) {
            // 这是主题帖，需要先保存获取 ID，然后更新 postId 为自身 ID
            comment.setPostId(null); // 先置空，落库后再回填为主题帖 ID
            comment.setStatus(POST_STATUS_PENDING);
            CircleComment savedComment = circleCommentRepository.save(comment);
            // 现在更新 postId 为已分配的 ID
            savedComment.setPostId(savedComment.getId());
            return circleCommentRepository.save(savedComment);
        } else {
            // 这是回复，需要使用父评论的 postId
            CircleComment parentComment = circleCommentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new CircleException("父评论不存在，ID: " + request.getParentCommentId()));

            // 确保父评论有 postId（主题帖的 postId 为 NULL，需要用 id 代替）
            Long postId = parentComment.getPostId() != null ? parentComment.getPostId() : parentComment.getId();
            comment.setPostId(postId);
            comment.setStatus(POST_STATUS_PUBLISHED);
            return circleCommentRepository.save(comment);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CircleComment> getDirectRepliesByPostId(Long postId) {
        if (postId == null) {
            throw new CircleException("帖子 ID 不能为空");
        }
        // 查询一级回复：post_id = postId AND parent_comment_id = postId
        return circleCommentRepository.findByPostIdAndParentCommentIdOrderByCreatedAtAsc(postId, postId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CircleComment> getAllCommentsByPostId(Long postId) {
        if (postId == null) {
            throw new CircleException("帖子 ID 不能为空");
        }
        // 查询该主题帖的所有评论（包括一级和次级），按创建时间升序排列
        return circleCommentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CircleComment> getRepliesByCommentId(Long commentId) {
        if (commentId == null) {
            throw new CircleException("评论 ID 不能为空");
        }
        // 查询子回复：parent_comment_id = commentId
        return circleCommentRepository.findByParentCommentIdOrderByCreatedAtAsc(commentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CircleComment> getAllPosts() {
        return circleCommentRepository.findAllByParentCommentIdIsNullOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CircleComment> getPostsByCircleId(Long circleId) {
        if (circleId == null) {
            throw new CircleException("圈子 ID 不能为空");
        }
        return circleCommentRepository.findByCircleIdAndParentCommentIdIsNullOrderByCreatedAtDesc(circleId);
    }


    @Override
    @Transactional(readOnly = true)
    public Optional<CircleComment> getCommentById(Long id) {
        if (id == null) {
            throw new CircleException("评论 ID 不能为空");
        }
        return circleCommentRepository.findById(id);
    }

    @Override
    @Transactional
    public void deleteComment(Long id) {
        if (id == null) {
            throw new CircleException("评论 ID 不能为空");
        }

        circleCommentRepository.findById(id)
                .orElseThrow(() -> new CircleException("评论不存在，ID: " + id));

        circleCommentRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void incrementLikes(Long id) {
        if (id == null) {
            throw new CircleException("评论 ID 不能为空");
        }

        CircleComment comment = circleCommentRepository.findById(id)
                .orElseThrow(() -> new CircleException("评论不存在，ID: " + id));

        // Deleted:comment.setLikes(comment.getLikes() + 1);
        // 处理 null 值：如果 likes 为 null，初始化为 0
        Integer currentLikes = comment.getLikes();
        comment.setLikes((currentLikes != null ? currentLikes : 0) + 1);
        circleCommentRepository.save(comment);
    }

    @Override
    @Transactional
    public CircleComment approvePost(Long postId) {
        if (postId == null) {
            throw new CircleException("帖子 ID 不能为空");
        }

        CircleComment post = circleCommentRepository.findById(postId)
                .orElseThrow(() -> new CircleException("帖子不存在，ID: " + postId));

        if (post.getParentCommentId() != null) {
            throw new CircleException("仅支持审核主题帖，当前 ID 不是主题帖");
        }

        String normalizedStatus = normalizePostStatus(post.getStatus());
        if (POST_STATUS_DELETED.equals(normalizedStatus)) {
            throw new CircleException("已删除帖子不能审核");
        }

        post.setStatus(POST_STATUS_PUBLISHED);
        return circleCommentRepository.save(post);
    }

    @Override
    @Transactional
    public void deletePostWithReplies(Long postId) {
        if (postId == null) {
            throw new CircleException("帖子 ID 不能为空");
        }

        CircleComment post = circleCommentRepository.findById(postId)
                .orElseThrow(() -> new CircleException("帖子不存在，ID: " + postId));

        if (post.getParentCommentId() != null) {
            throw new CircleException("仅支持删除主题帖，当前 ID 不是主题帖");
        }

        Long rootPostId = post.getPostId() != null ? post.getPostId() : post.getId();
        circleCommentRepository.deleteByPostId(rootPostId);

        if (!Objects.equals(rootPostId, post.getId())) {
            circleCommentRepository.deleteById(post.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countRepliesByPostId(Long postId) {
        if (postId == null) {
            return 0L;
        }
        return circleCommentRepository.countByPostIdAndParentCommentIdIsNotNull(postId);
    }

    @Override
    public CircleCommentResponse convertToResponse(CircleComment comment) {
        if (comment == null) {
            return null;
        }

        CircleCommentResponse response = new CircleCommentResponse();
        response.setId(comment.getId());
        response.setContent(comment.getContent());
        response.setParentCommentId(comment.getParentCommentId());
        response.setPostId(comment.getPostId());
        response.setCircleId(comment.getCircleId());
        response.setLikes(comment.getLikes());
        response.setCreatedAt(comment.getCreatedAt());
        String normalizedStatus = normalizePostStatus(comment.getStatus());
        if (comment.getParentCommentId() != null && POST_STATUS_PENDING.equals(normalizedStatus)) {
            normalizedStatus = POST_STATUS_PUBLISHED;
        }
        response.setStatus(normalizedStatus);

        if (comment.getAuthor() != null) {
            CircleCommentResponse.AuthorInfo authorInfo = new CircleCommentResponse.AuthorInfo();
            authorInfo.setId(comment.getAuthor().getId());
            authorInfo.setUsername(comment.getAuthor().getUsername());
            authorInfo.setNickname(comment.getAuthor().getNickname());
            authorInfo.setAvatarUrl(comment.getAuthor().getAvatarUrl());
            response.setAuthor(authorInfo);
        }

        if (comment.getParentCommentId() == null) {
            long replyCount = circleCommentRepository.countByPostIdAndParentCommentIdIsNotNull(comment.getId());
            response.setReplyCount(replyCount);
        } else {
            response.setReplyCount(0L);
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CircleComment> getPostsByAuthorId(Long authorId) {
        if (authorId == null) {
            throw new CircleException("作者 ID 不能为空");
        }
        return circleCommentRepository.findByAuthorIdAndParentCommentIdIsNullOrderByCreatedAtDesc(authorId);
    }

    @Override
    @Transactional
    public void deletePostsByAuthorId(Long authorId) {
        if (authorId == null) {
            throw new CircleException("作者 ID 不能为空");
        }

        List<CircleComment> posts = getPostsByAuthorId(authorId);
        if (!posts.isEmpty()) {
            circleCommentRepository.deleteAll(posts);
        }
    }

    @Override
    public List<CircleCommentResponse> convertToResponseList(List<CircleComment> comments) {
        return comments.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private String normalizePostStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return POST_STATUS_PENDING;
        }

        String normalized = status.trim().toLowerCase();
        if ("approved".equals(normalized) || "normal".equals(normalized)) {
            return POST_STATUS_PUBLISHED;
        }
        if ("close".equals(normalized) || "closed".equals(normalized)) {
            return POST_STATUS_DELETED;
        }
        return normalized;
    }
}
