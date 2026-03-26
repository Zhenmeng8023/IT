/**
 * 项目任务服务接口
 * 定义项目任务相关的业务逻辑方法
 */
package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.dto.ProjectTaskCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTaskStatusUpdateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTaskUpdateRequest;
import com.alikeyou.itmoduleproject.vo.ProjectTaskVO;

import java.util.List;

/**
 * 项目任务服务接口
 * 定义项目任务相关的业务逻辑方法
 */
public interface ProjectTaskService {
    /**
     * 获取项目任务列表
     * @param projectId 项目ID
     * @param status 任务状态
     * @param priority 优先级
     * @param assigneeId 负责人ID
     * @param userId 用户ID
     * @return 任务列表
     */
    List<ProjectTaskVO> listTasks(Long projectId, String status, String priority, Long assigneeId, Long userId);

    /**
     * 获取我的任务列表
     * @param projectId 项目ID
     * @param currentUserId 当前用户ID
     * @return 任务列表
     */
    List<ProjectTaskVO> listMyTasks(Long projectId, Long currentUserId);

    /**
     * 创建项目任务
     */
    ProjectTaskVO createTask(ProjectTaskCreateRequest request, Long currentUserId);

    /**
     * 更新项目任务
     */
    ProjectTaskVO updateTask(Long taskId, ProjectTaskUpdateRequest request, Long currentUserId);

    /**
     * 更新任务状态
     */
    ProjectTaskVO updateTaskStatus(Long taskId, ProjectTaskStatusUpdateRequest request, Long currentUserId);

    /**
     * 删除项目任务
     */
    void deleteTask(Long taskId, Long currentUserId);


    default List<ProjectTaskVO> listTasks(Long projectId, Long currentUserId) {
        return listTasks(projectId, null, null, null, currentUserId);
    }

}
