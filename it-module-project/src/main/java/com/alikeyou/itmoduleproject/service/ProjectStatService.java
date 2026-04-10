package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.entity.ProjectStatDaily;
import com.alikeyou.itmoduleproject.vo.PageResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ProjectStatService {

    Map<String, Object> getOverview(Long projectId, Long currentUserId);

    List<ProjectStatDaily> getTrend(Long projectId, LocalDate startDate, LocalDate endDate, Long currentUserId);

    PageResult<ProjectStatDaily> pageDaily(Long projectId, int page, int size, Long currentUserId);

    List<ProjectStatDaily> rebuildDailyStats(Long projectId, LocalDate startDate, LocalDate endDate, Long currentUserId);
}
