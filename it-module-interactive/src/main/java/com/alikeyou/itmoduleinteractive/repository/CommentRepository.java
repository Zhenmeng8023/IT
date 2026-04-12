package com.alikeyou.itmoduleinteractive.repository;

import com.alikeyou.itmoduleinteractive.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByOrderByCreatedAtDesc();

    List<Comment> findByPostIdAndPostTypeOrderByCreatedAtAsc(Long postId, String postType);

    List<Comment> findByParentComment_IdOrderByCreatedAtAsc(Long parentCommentId);

    List<Comment> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    List<Comment> findByAuthorIdAndCreatedAtBetweenOrderByCreatedAtAsc(Long authorId, java.time.Instant start, java.time.Instant end);
}
