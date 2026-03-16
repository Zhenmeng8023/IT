package com.alikeyou.itmodulecircle.repository;

import com.alikeyou.itmodulecircle.entity.CircleComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CircleCommentRepository extends JpaRepository<CircleComment, Long> {

    /**
     * 根据圈子 ID 查询评论/帖子列表（只查询主题帖，即 parentCommentId 为 NULL 的记录）
     */
    @EntityGraph(attributePaths = {"author", "circle"})
    List<CircleComment> findByCircleIdAndParentCommentIdIsNullOrderByCreatedAtDesc(Long circleId);


    /**
     * 根据帖子 ID 查询一级回复列表（不包括楼中楼）
     */
    @EntityGraph(attributePaths = {"author"})
    List<CircleComment> findByPostIdAndParentCommentIdOrderByCreatedAtAsc(Long postId, Long parentCommentId);

    /**
     * 根据帖子 ID 查询所有回复（包括一级和次级）
     */
    @EntityGraph(attributePaths = {"author"})
    List<CircleComment> findByPostIdOrderByCreatedAtAsc(Long postId);

    /**
     * 根据父评论 ID 查询子评论
     */
    @EntityGraph(attributePaths = {"author"})
    List<CircleComment> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId);

    /**
     * 统计帖子的回复数
     */
    long countByPostIdAndParentCommentIdIsNotNull(Long postId);
}
