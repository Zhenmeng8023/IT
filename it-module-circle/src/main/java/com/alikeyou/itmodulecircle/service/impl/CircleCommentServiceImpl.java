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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CircleCommentServiceImpl implements CircleCommentService {

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

        if (request.getParentCommentId() == null) {
            // 这是主题帖，先设置临时值，保存后再更新
            comment.setPostId(0L); // 先设置为 0，避免 NOT NULL 约束失败
            comment = circleCommentRepository.save(comment);
            // 现在有了 ID，将 postId 更新为自己的 ID
            comment.setPostId(comment.getId());
            comment = circleCommentRepository.save(comment);
        } else {
            // 这是回复，需要找到父评论并获取其 postId
            CircleComment parentComment = circleCommentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new CircleException("父评论不存在，ID: " + request.getParentCommentId()));
            comment.setPostId(parentComment.getPostId() != null ? parentComment.getPostId() : parentComment.getId());
            comment.setLikes(0);
            comment.setStatus("normal");
            comment.setCreatedAt(Instant.now());
            comment = circleCommentRepository.save(comment);
        }

        return comment;
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
    public List<CircleComment> getRepliesByPostId(Long postId) {
        if (postId == null) {
            throw new CircleException("帖子 ID 不能为空");
        }
        return circleCommentRepository.findByPostIdAndParentCommentIdIsNotNullOrderByCreatedAtAsc(postId);
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

        CircleComment comment = circleCommentRepository.findById(id)
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
        response.setStatus(comment.getStatus());

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
    public List<CircleCommentResponse> convertToResponseList(List<CircleComment> comments) {
        return comments.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
}
