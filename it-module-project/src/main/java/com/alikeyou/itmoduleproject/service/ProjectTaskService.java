package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.dto.ProjectTaskCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTaskReopenApplyRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTaskReopenReviewRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTaskStatusUpdateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTaskUpdateRequest;
import com.alikeyou.itmoduleproject.vo.ProjectTaskReopenRequestVO;
import com.alikeyou.itmoduleproject.vo.ProjectTaskVO;

import java.util.List;

public interface ProjectTaskService {
    List<ProjectTaskVO> listTasks(Long projectId, String status, String priority, Long assigneeId, Long userId);

    List<ProjectTaskVO> listMyTasks(Long projectId, Long currentUserId);

    ProjectTaskVO createTask(ProjectTaskCreateRequest request, Long currentUserId);

    ProjectTaskVO updateTask(Long taskId, ProjectTaskUpdateRequest request, Long currentUserId);

    ProjectTaskVO updateTaskStatus(Long taskId, ProjectTaskStatusUpdateRequest request, Long currentUserId);

    ProjectTaskReopenRequestVO applyReopenRequest(Long taskId, ProjectTaskReopenApplyRequest request, Long currentUserId);

    List<ProjectTaskReopenRequestVO> listReopenRequests(Long taskId, Long currentUserId);

    ProjectTaskVO approveReopenRequest(Long taskId, Long requestId, ProjectTaskReopenReviewRequest request, Long currentUserId);

    ProjectTaskReopenRequestVO rejectReopenRequest(Long taskId, Long requestId, ProjectTaskReopenReviewRequest request, Long currentUserId);

    void deleteTask(Long taskId, Long currentUserId);

    default List<ProjectTaskVO> listTasks(Long projectId, Long currentUserId) {
        return listTasks(projectId, null, null, null, currentUserId);
    }
}
