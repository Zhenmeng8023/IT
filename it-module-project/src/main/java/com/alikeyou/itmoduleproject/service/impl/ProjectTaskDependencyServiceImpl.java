package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.TaskDependencyCreateRequest;
import com.alikeyou.itmoduleproject.entity.ProjectTask;
import com.alikeyou.itmoduleproject.entity.ProjectTaskDependency;
import com.alikeyou.itmoduleproject.enums.ProjectTaskStatusEnum;
import com.alikeyou.itmoduleproject.repository.ProjectTaskDependencyRepository;
import com.alikeyou.itmoduleproject.repository.ProjectTaskRepository;
import com.alikeyou.itmoduleproject.service.ProjectTaskDependencyService;
import com.alikeyou.itmoduleproject.service.ProjectTaskLogService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectTaskAccessSupport;
import com.alikeyou.itmoduleproject.support.ProjectVoMapper;
import com.alikeyou.itmoduleproject.vo.TaskDependencyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectTaskDependencyServiceImpl implements ProjectTaskDependencyService {
    private final ProjectTaskDependencyRepository projectTaskDependencyRepository;
    private final ProjectTaskRepository projectTaskRepository;
    private final ProjectTaskAccessSupport taskAccessSupport;
    private final ProjectTaskLogService projectTaskLogService;

    @Override
    public List<TaskDependencyVO> listDependencies(Long taskId, Long currentUserId) {
        taskAccessSupport.assertTaskReadable(taskId, currentUserId);
        List<ProjectTaskDependency> dependencies = new ArrayList<>();
        dependencies.addAll(projectTaskDependencyRepository.findBySuccessorTaskId(taskId));
        dependencies.addAll(projectTaskDependencyRepository.findByPredecessorTaskId(taskId));
        return toVOList(dependencies, taskId);
    }

    @Override
    @Transactional
    public TaskDependencyVO addDependency(Long taskId, TaskDependencyCreateRequest request, Long currentUserId) {
        taskAccessSupport.assertTaskReadable(taskId, currentUserId);
        if (request == null || request.getPredecessorTaskId() == null) {
            throw new BusinessException("前置任务不能为空");
        }
        if (taskId.equals(request.getPredecessorTaskId())) {
            throw new BusinessException("任务不能依赖自己");
        }
        ProjectTask successor = taskAccessSupport.getTaskOrThrow(taskId);
        ProjectTask predecessor = taskAccessSupport.getTaskOrThrow(request.getPredecessorTaskId());
        if (!successor.getProjectId().equals(predecessor.getProjectId())) {
            throw new BusinessException("依赖任务必须属于同一个项目");
        }
        if (projectTaskDependencyRepository.existsByPredecessorTaskIdAndSuccessorTaskId(predecessor.getId(), successor.getId())) {
            throw new BusinessException("该依赖关系已存在");
        }
        validateNoCycle(predecessor.getId(), successor.getId());
        ProjectTaskDependency saved = projectTaskDependencyRepository.save(ProjectTaskDependency.builder()
                .projectId(successor.getProjectId())
                .predecessorTaskId(predecessor.getId())
                .successorTaskId(successor.getId())
                .dependencyType(normalizeType(request.getDependencyType()))
                .build());
        projectTaskLogService.recordFieldChange(taskId, currentUserId, "update", "dependency", null, predecessor.getTitle());
        return toVOList(List.of(saved), taskId).get(0);
    }

    @Override
    @Transactional
    public void deleteDependency(Long dependencyId, Long currentUserId) {
        ProjectTaskDependency dependency = projectTaskDependencyRepository.findById(dependencyId)
                .orElseThrow(() -> new BusinessException("依赖关系不存在"));
        taskAccessSupport.assertTaskReadable(dependency.getSuccessorTaskId(), currentUserId);
        ProjectTask predecessor = taskAccessSupport.getTaskOrThrow(dependency.getPredecessorTaskId());
        projectTaskDependencyRepository.delete(dependency);
        projectTaskLogService.recordFieldChange(dependency.getSuccessorTaskId(), currentUserId, "update", "dependency", predecessor.getTitle(), null);
    }

    @Override
    public void assertTaskCanBeDone(Long taskId) {
        List<ProjectTaskDependency> dependencies = projectTaskDependencyRepository.findBySuccessorTaskId(taskId);
        if (dependencies.isEmpty()) {
            return;
        }
        List<Long> predecessorIds = dependencies.stream().map(ProjectTaskDependency::getPredecessorTaskId).toList();
        Map<Long, ProjectTask> taskMap = mapTasks(predecessorIds);
        for (ProjectTaskDependency dependency : dependencies) {
            if (!"finish_to_start".equalsIgnoreCase(dependency.getDependencyType())) {
                continue;
            }
            ProjectTask predecessor = taskMap.get(dependency.getPredecessorTaskId());
            if (predecessor == null) {
                continue;
            }
            if (!ProjectTaskStatusEnum.DONE.getValue().equals(predecessor.getStatus())) {
                throw new BusinessException("前置任务未完成，不能标记为已完成：" + predecessor.getTitle());
            }
        }
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "finish_to_start";
        }
        return type.trim();
    }

    private void validateNoCycle(Long predecessorTaskId, Long successorTaskId) {
        ArrayDeque<Long> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.offer(successorTaskId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (!visited.add(current)) {
                continue;
            }
            if (current.equals(predecessorTaskId)) {
                throw new BusinessException("不能创建循环依赖");
            }
            for (ProjectTaskDependency next : projectTaskDependencyRepository.findByPredecessorTaskId(current)) {
                queue.offer(next.getSuccessorTaskId());
            }
        }
    }

    private List<TaskDependencyVO> toVOList(List<ProjectTaskDependency> dependencies, Long taskId) {
        Set<Long> ids = new HashSet<>();
        for (ProjectTaskDependency dependency : dependencies) {
            ids.add(dependency.getPredecessorTaskId());
            ids.add(dependency.getSuccessorTaskId());
        }
        Map<Long, ProjectTask> taskMap = mapTasks(ids);
        List<TaskDependencyVO> result = new ArrayList<>();
        for (ProjectTaskDependency dependency : dependencies) {
            ProjectTask predecessor = taskMap.get(dependency.getPredecessorTaskId());
            ProjectTask successor = taskMap.get(dependency.getSuccessorTaskId());
            result.add(ProjectVoMapper.toTaskDependencyVO(
                    dependency,
                    predecessor,
                    successor,
                    taskId.equals(dependency.getSuccessorTaskId()) && predecessor != null && !ProjectTaskStatusEnum.DONE.getValue().equals(predecessor.getStatus())
            ));
        }
        return result;
    }

    private Map<Long, ProjectTask> mapTasks(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, ProjectTask> taskMap = new HashMap<>();
        for (ProjectTask task : projectTaskRepository.findAllById(ids)) {
            taskMap.put(task.getId(), task);
        }
        return taskMap;
    }
}
