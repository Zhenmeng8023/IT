package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.dto.TaskDependencyCreateRequest;
import com.alikeyou.itmoduleproject.vo.TaskDependencyVO;

import java.util.List;

public interface ProjectTaskDependencyService {
    List<TaskDependencyVO> listDependencies(Long taskId, Long currentUserId);

    TaskDependencyVO addDependency(Long taskId, TaskDependencyCreateRequest request, Long currentUserId);

    void deleteDependency(Long dependencyId, Long currentUserId);

    void assertTaskCanBeDone(Long taskId);
}
