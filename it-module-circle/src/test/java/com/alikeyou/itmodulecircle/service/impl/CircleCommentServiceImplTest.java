package com.alikeyou.itmodulecircle.service.impl;

import com.alikeyou.itmodulecircle.dto.CircleCommentRequest;
import com.alikeyou.itmodulecircle.dto.CircleCommentResponse;
import com.alikeyou.itmodulecircle.entity.CircleComment;
import com.alikeyou.itmodulecircle.repository.CircleCommentRepository;
import com.alikeyou.itmodulecircle.support.CircleCommentVisibilitySupport;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CircleCommentServiceImplTest {

    @Mock
    private CircleCommentRepository circleCommentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CircleCommentServiceImpl service;

    @Test
    void createCommentShouldPersistRootPostAsPendingAndBackfillPostId() {
        CircleCommentRequest request = new CircleCommentRequest();
        request.setCircleId(8L);
        request.setAuthorId(1L);
        request.setContent("主题帖");

        UserInfo author = buildAuthor(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(circleCommentRepository.save(any(CircleComment.class))).thenAnswer(invocation -> {
            CircleComment comment = invocation.getArgument(0);
            if (comment.getId() == null) {
                comment.setId(101L);
            }
            return comment;
        });

        CircleComment saved = service.createComment(request);

        assertNotNull(saved);
        assertEquals(101L, saved.getId());
        assertEquals(101L, saved.getPostId());
        assertEquals(CircleCommentVisibilitySupport.STATUS_PENDING, saved.getStatus());
    }

    @Test
    void createCommentShouldAutoPublishReplyWhenRepliesAreExemptFromModeration() {
        CircleCommentRequest request = new CircleCommentRequest();
        request.setCircleId(8L);
        request.setAuthorId(2L);
        request.setParentCommentId(101L);
        request.setContent("回复");

        UserInfo author = buildAuthor(2L);
        CircleComment parent = new CircleComment();
        parent.setId(101L);
        parent.setCircleId(8L);
        parent.setPostId(101L);
        parent.setParentCommentId(null);

        when(userRepository.findById(2L)).thenReturn(Optional.of(author));
        when(circleCommentRepository.findById(101L)).thenReturn(Optional.of(parent));
        when(circleCommentRepository.save(any(CircleComment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CircleComment saved = service.createComment(request);

        assertEquals(101L, saved.getPostId());
        assertEquals(CircleCommentVisibilitySupport.STATUS_PUBLISHED, saved.getStatus());
    }

    @Test
    void getPostsByCircleIdShouldOnlyExposePublicVisibleRootPosts() {
        when(circleCommentRepository.findByCircleIdAndParentCommentIdIsNullOrderByCreatedAtDesc(8L)).thenReturn(List.of(
                buildComment(1L, 8L, null, null, "published"),
                buildComment(2L, 8L, null, null, "normal"),
                buildComment(3L, 8L, null, null, "pending"),
                buildComment(4L, 8L, null, null, "hidden"),
                buildComment(5L, 8L, null, null, "deleted")
        ));

        List<CircleComment> posts = service.getPostsByCircleId(8L);

        assertIterableEquals(List.of(1L, 2L), posts.stream().map(CircleComment::getId).toList());
    }

    @Test
    void getManagePostsByCircleIdShouldKeepPendingAndDeletedPosts() {
        List<CircleComment> rootPosts = List.of(
                buildComment(1L, 8L, null, null, "published"),
                buildComment(2L, 8L, null, null, "hidden"),
                buildComment(3L, 8L, null, null, "deleted")
        );
        when(circleCommentRepository.findByCircleIdAndParentCommentIdIsNullOrderByCreatedAtDesc(8L)).thenReturn(rootPosts);

        List<CircleComment> posts = service.getManagePostsByCircleId(8L);

        assertEquals(3, posts.size());
        assertIterableEquals(List.of(1L, 2L, 3L), posts.stream().map(CircleComment::getId).toList());
    }

    @Test
    void getDirectRepliesByPostIdShouldOnlyExposePublishedReplies() {
        when(circleCommentRepository.findByPostIdAndParentCommentIdOrderByCreatedAtAsc(101L, 101L)).thenReturn(List.of(
                buildComment(11L, 8L, 101L, 101L, "published"),
                buildComment(12L, 8L, 101L, 101L, "normal"),
                buildComment(13L, 8L, 101L, 101L, "hidden"),
                buildComment(14L, 8L, 101L, 101L, "deleted")
        ));

        List<CircleComment> replies = service.getDirectRepliesByPostId(101L);

        assertIterableEquals(List.of(11L, 12L), replies.stream().map(CircleComment::getId).toList());
    }

    @Test
    void convertToResponseShouldNormalizeLegacyStatusAndCountOnlyVisibleReplies() {
        CircleComment post = buildComment(101L, 8L, 101L, null, "normal");
        post.setAuthor(buildAuthor(5L));

        when(circleCommentRepository.findByPostIdOrderByCreatedAtAsc(101L)).thenReturn(List.of(
                post,
                buildComment(201L, 8L, 101L, 101L, "published"),
                buildComment(202L, 8L, 101L, 101L, "normal"),
                buildComment(203L, 8L, 101L, 101L, "hidden"),
                buildComment(204L, 8L, 101L, 101L, "deleted")
        ));

        CircleCommentResponse response = service.convertToResponse(post);

        assertEquals(CircleCommentVisibilitySupport.STATUS_PUBLISHED, response.getStatus());
        assertEquals(2L, response.getReplyCount());
        assertEquals(5L, response.getAuthor().getId());
    }

    @Test
    void convertToResponseShouldNotRewritePendingReplyToPublished() {
        CircleComment reply = buildComment(301L, 8L, 101L, 101L, "hidden");

        CircleCommentResponse response = service.convertToResponse(reply);

        assertEquals(CircleCommentVisibilitySupport.STATUS_PENDING, response.getStatus());
        assertEquals(0L, response.getReplyCount());
    }

    @Test
    void createCommentShouldRejectReplyAcrossCircles() {
        CircleCommentRequest request = new CircleCommentRequest();
        request.setCircleId(8L);
        request.setAuthorId(1L);
        request.setParentCommentId(101L);
        request.setContent("跨圈回复");

        UserInfo author = buildAuthor(1L);
        CircleComment parent = buildComment(101L, 9L, 101L, null, "published");

        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(circleCommentRepository.findById(101L)).thenReturn(Optional.of(parent));

        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> service.createComment(request)
        );
    }

    @Test
    void approvePostWorkflowShouldExposeApprovedRootPostAndKeepReplyCountConsistent() {
        CircleComment pendingPost = buildComment(401L, 8L, 401L, null, "pending");
        pendingPost.setAuthor(buildAuthor(7L));
        CircleComment publishedReply = buildComment(402L, 8L, 401L, 401L, "published");
        CircleComment hiddenReply = buildComment(403L, 8L, 401L, 401L, "hidden");

        when(circleCommentRepository.findById(401L)).thenReturn(Optional.of(pendingPost));
        when(circleCommentRepository.save(any(CircleComment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(circleCommentRepository.findByCircleIdAndParentCommentIdIsNullOrderByCreatedAtDesc(8L)).thenReturn(List.of(pendingPost));
        when(circleCommentRepository.findByPostIdOrderByCreatedAtAsc(401L)).thenReturn(List.of(pendingPost, publishedReply, hiddenReply));

        CircleComment approved = service.approvePost(401L);
        List<CircleComment> posts = service.getPostsByCircleId(8L);
        CircleCommentResponse response = service.convertToResponse(approved);

        assertSame(pendingPost, approved);
        assertEquals(CircleCommentVisibilitySupport.STATUS_PUBLISHED, approved.getStatus());
        assertIterableEquals(List.of(401L), posts.stream().map(CircleComment::getId).toList());
        assertEquals(1L, response.getReplyCount());
        assertEquals(CircleCommentVisibilitySupport.STATUS_PUBLISHED, response.getStatus());
    }

    private UserInfo buildAuthor(Long id) {
        UserInfo author = new UserInfo();
        author.setId(id);
        author.setUsername("user-" + id);
        author.setNickname("昵称-" + id);
        author.setAvatarUrl("avatar-" + id + ".png");
        return author;
    }

    private CircleComment buildComment(Long id, Long circleId, Long postId, Long parentCommentId, String status) {
        CircleComment comment = new CircleComment();
        comment.setId(id);
        comment.setCircleId(circleId);
        comment.setPostId(postId);
        comment.setParentCommentId(parentCommentId);
        comment.setStatus(status);
        comment.setContent("content-" + id);
        comment.setCreatedAt(Instant.parse("2026-04-18T00:00:00Z"));
        comment.setLikes(0);
        return comment;
    }
}
