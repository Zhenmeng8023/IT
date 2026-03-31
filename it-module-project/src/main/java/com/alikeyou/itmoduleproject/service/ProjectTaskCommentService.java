package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.dto.TaskCommentCreateRequest;
import com.alikeyou.itmoduleproject.vo.TaskCommentVO;

import java.util.List;

public interface ProjectTaskCommentService {
    List<TaskCommentVO> listComments(Long taskId, Long currentUserId);

    TaskCommentVO addComment(Long taskId, TaskCommentCreateRequest request, Long currentUserId);

    TaskCommentVO replyComment(Long commentId, TaskCommentCreateRequest request, Long currentUserId);

    void deleteComment(Long commentId, Long currentUserId);
}
