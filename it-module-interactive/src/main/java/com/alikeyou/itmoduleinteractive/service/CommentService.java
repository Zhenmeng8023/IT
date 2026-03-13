package com.alikeyou.itmoduleinteractive.service;

import com.alikeyou.itmoduleinteractive.entiey.Comment;
import java.util.List;
import java.util.Optional;

public interface CommentService {
    List<Comment> getAllComments();
    Optional<Comment> getCommentById(Long id);
    Comment saveComment(Comment comment);
    List<Comment> getCommentsByPostId(Long postId);
}