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
     * 获取圈子的主题帖列表
     */
    List<CircleComment> getPostsByCircleId(Long circleId);

    /**
     * 获取帖子的回复列表
     */
    List<CircleComment> getRepliesByPostId(Long postId);

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
     * 转换为响应对象
     */
    CircleCommentResponse convertToResponse(CircleComment comment);

    /**
     * 批量转换
     */
    List<CircleCommentResponse> convertToResponseList(List<CircleComment> comments);
}
