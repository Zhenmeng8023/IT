package com.alikeyou.itmodulecircle.service;

import com.alikeyou.itmodulecircle.dto.CircleCommentRequest;
import com.alikeyou.itmodulecircle.dto.CircleCommentResponse;
import com.alikeyou.itmodulecircle.entity.CircleComment;

import java.util.List;
import java.util.Optional;

public interface CircleCommentService {

    /**
     * 创建主题帖或评论
     */
    CircleComment createComment(CircleCommentRequest request);

    /**
     * 获取主题帖的所有评论（包括一级回复和楼中楼）
     */
    List<CircleComment> getAllCommentsByPostId(Long postId);

    /**
     * 获取圈子的主题帖列表
     * 前台公开口径：仅返回用户真正可见的主题帖
     */
    List<CircleComment> getPostsByCircleId(Long circleId);

    /**
     * 获取圈子的主题帖列表（管理端）
     * 后台管理口径：返回全部主题帖，含 pending / published / deleted 等状态
     */
    List<CircleComment> getManagePostsByCircleId(Long circleId);

    /**
     * 获取帖子的一级回复列表（直接回复主题帖的评论）
     * 前台公开口径：仅返回用户真正可见的回复
     */
    List<CircleComment> getDirectRepliesByPostId(Long postId);

    /**
     * 获取某条评论的子回复（楼中楼）
     */
    List<CircleComment> getRepliesByCommentId(Long commentId);

    /**
     * 获取评论详情
     */
    Optional<CircleComment> getCommentById(Long id);

    /**
     * 删除评论/帖子
     */
    void deleteComment(Long id);

    /**
     * 点赞评论/帖子
     */
    void incrementLikes(Long id);

    /**
     * 审核通过主题帖
     */
    CircleComment approvePost(Long postId);

    /**
     * 删除主题帖及其所有回复
     */
    void deletePostWithReplies(Long postId);

    /**
     * 获取主题帖回复数
     */
    long countRepliesByPostId(Long postId);

    /**
     * 转换为响应对象
     */
    CircleCommentResponse convertToResponse(CircleComment comment);

    /**
     * 根据用户 ID 获取主题帖列表
     */
    List<CircleComment> getPostsByAuthorId(Long authorId);

    /**
     * 获取所有圈子的所有主题帖列表
     */
    List<CircleComment> getAllPosts();

    /**
     * 根据用户 ID 删除主题帖
     */
    void deletePostsByAuthorId(Long authorId);

    /**
     * 批量转换
     */
    List<CircleCommentResponse> convertToResponseList(List<CircleComment> comments);
}
