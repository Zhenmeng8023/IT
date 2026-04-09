package com.alikeyou.itmoduleinteractive.service.impl;

import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.repository.BlogRepository;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import com.alikeyou.itmoduleinteractive.entity.Comment;
import com.alikeyou.itmoduleinteractive.entity.Notification;
import com.alikeyou.itmoduleinteractive.repository.CommentRepository;
import com.alikeyou.itmoduleinteractive.repository.LikeRecordRepository;
import com.alikeyou.itmoduleinteractive.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private LikeRecordRepository likeRecordRepository;

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private BlogRepository blogRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CommentServiceImpl commentService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void saveTopLevelCommentShouldNotifyBlogAuthor() {
        Blog blog = buildBlog(1L, 7L);
        Comment input = new Comment();
        input.setPostId(1L);
        input.setAuthorId(99L);
        input.setContent("这篇文章很有帮助");

        when(blogRepository.findWithAssociationsById(1L)).thenReturn(Optional.of(blog));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            saved.setId(101L);
            saved.setCreatedAt(Instant.now());
            return saved;
        });
        when(userInfoRepository.findAllById(any())).thenReturn(List.of(buildUser(99L, "评论者")));
        when(blogRepository.findByIdIn(anyList())).thenReturn(List.of(blog));
        when(likeRecordRepository.findByTargetTypeAndTargetId("comment", 101L)).thenReturn(List.of());

        commentService.saveComment(input);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService).createNotification(captor.capture());
        Notification notification = captor.getValue();
        assertEquals(7L, notification.getReceiverId());
        assertEquals(99L, notification.getSenderId());
        assertEquals("comment", notification.getType());
        assertEquals("comment", notification.getTargetType());
        assertEquals(101L, notification.getTargetId());
    }

    @Test
    void saveReplyShouldNotifyParentAuthorAndBlogAuthor() {
        Blog blog = buildBlog(1L, 7L);
        Comment parent = new Comment();
        parent.setId(10L);
        parent.setPostId(1L);
        parent.setAuthorId(5L);
        parent.setStatus("normal");

        Comment input = new Comment();
        input.setPostId(1L);
        input.setAuthorId(99L);
        input.setContent("我来补充一下");
        Comment parentRef = new Comment();
        parentRef.setId(10L);
        input.setParentComment(parentRef);

        when(blogRepository.findWithAssociationsById(1L)).thenReturn(Optional.of(blog));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            saved.setId(102L);
            saved.setCreatedAt(Instant.now());
            return saved;
        });
        when(userInfoRepository.findAllById(any())).thenReturn(List.of(buildUser(99L, "回复者")));
        when(blogRepository.findByIdIn(anyList())).thenReturn(List.of(blog));
        when(likeRecordRepository.findByTargetTypeAndTargetId("comment", 102L)).thenReturn(List.of());

        commentService.saveComment(input);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService, times(2)).createNotification(captor.capture());
        List<Notification> notifications = captor.getAllValues();

        assertEquals(5L, notifications.get(0).getReceiverId());
        assertEquals("reply", notifications.get(0).getType());
        assertEquals(7L, notifications.get(1).getReceiverId());
        assertEquals("comment", notifications.get(1).getType());
    }

    private Blog buildBlog(Long blogId, Long authorId) {
        UserInfo author = buildUser(authorId, "博客作者");
        Blog blog = new Blog();
        blog.setId(blogId);
        blog.setTitle("测试博客");
        blog.setAuthor(author);
        return blog;
    }

    private UserInfo buildUser(Long id, String username) {
        UserInfo user = new UserInfo();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(username);
        return user;
    }
}
