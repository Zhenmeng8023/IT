package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.vo.ProjectActivityVO;

import java.util.List;

public interface ProjectActivityLogService {

    void record(Long projectId, Long operatorId, String action, String targetType, Long targetId, String content);

    List<ProjectActivityVO> listActivities(Long projectId, String action, String targetType, Long operatorId, String startTime, String endTime, Long currentUserId);

    default List<ProjectActivityVO> listActivities(Long projectId, Long currentUserId) {
        return listActivities(projectId, null, null, null, null, null, currentUserId);
    }
}
