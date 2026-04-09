package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.vo.PageResult;
import com.alikeyou.itmoduleproject.vo.ProjectActivityPositionVO;
import com.alikeyou.itmoduleproject.vo.ProjectActivityVO;

import java.util.List;

public interface ProjectActivityLogService {

    void record(Long projectId, Long operatorId, String action, String targetType, Long targetId, String content);

    List<ProjectActivityVO> listActivities(Long projectId, String action, String targetType, Long operatorId, String startTime, String endTime, Long currentUserId);

    PageResult<ProjectActivityVO> pageActivities(Long projectId, String action, String targetType, Long operatorId, String startTime, String endTime, int page, int size, Long currentUserId);

    ProjectActivityPositionVO getActivityPosition(Long projectId, Long activityId, String action, String targetType, Long operatorId, String startTime, String endTime, int size, Long currentUserId);

    default List<ProjectActivityVO> listActivities(Long projectId, Long currentUserId) {
        return listActivities(projectId, null, null, null, null, null, currentUserId);
    }
}
