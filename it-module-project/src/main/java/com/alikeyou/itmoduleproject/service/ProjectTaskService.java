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
     * @param userId 用户ID
     * @return 任务列表
     */
    List<ProjectTaskVO> listTasks(Long projectId, Long userId);
    
    /**
     * 创建项目任务
     * @param request 创建任务请求参数
     * @param currentUserId 当前用户ID
     * @return 任务信息
     */
    ProjectTaskVO createTask(ProjectTaskCreateRequest request, Long currentUserId);
    
    /**
     * 更新项目任务
     * @param taskId 任务ID
     * @param request 更新任务请求参数
     * @param currentUserId 当前用户ID
     * @return 任务信息
     */
    ProjectTaskVO updateTask(Long taskId, ProjectTaskUpdateRequest request, Long currentUserId);
    
    /**
     * 更新任务状态
     * @param taskId 任务ID
     * @param request 更新状态请求参数
     * @param currentUserId 当前用户ID
     * @return 任务信息
     */
    ProjectTaskVO updateTaskStatus(Long taskId, ProjectTaskStatusUpdateRequest request, Long currentUserId);
    
    /**
     * 删除项目任务
     * @param taskId 任务ID
     * @param currentUserId 当前用户ID
     */
    void deleteTask(Long taskId, Long currentUserId);
}