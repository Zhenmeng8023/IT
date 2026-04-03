package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.vo.TaskLogVO;

import java.util.List;

public interface ProjectTaskLogService {
    List<TaskLogVO> listLogs(Long taskId, String action, Long operatorId, String startTime, String endTime, Long currentUserId);

    default List<TaskLogVO> listLogs(Long taskId, Long currentUserId) {
        return listLogs(taskId, null, null, null, null, currentUserId);
    }

    void recordCreate(Long taskId, Long operatorId);

    void recordDelete(Long taskId, Long operatorId, String oldValue);

    void recordFieldChange(Long taskId, Long operatorId, String action, String fieldName, Object oldValue, Object newValue);

    void recordComment(Long taskId, Long operatorId, Object oldValue, Object newValue);

    void recordAttachment(Long taskId, Long operatorId, Object oldValue, Object newValue);
}
