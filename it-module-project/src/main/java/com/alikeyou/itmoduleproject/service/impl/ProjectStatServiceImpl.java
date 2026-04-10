package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.Project;
import com.alikeyou.itmoduleproject.entity.ProjectDownloadRecord;
import com.alikeyou.itmoduleproject.entity.ProjectMember;
import com.alikeyou.itmoduleproject.entity.ProjectSprint;
import com.alikeyou.itmoduleproject.entity.ProjectStatDaily;
import com.alikeyou.itmoduleproject.entity.ProjectStar;
import com.alikeyou.itmoduleproject.entity.ProjectTask;
import com.alikeyou.itmoduleproject.repository.ProjectDownloadRecordRepository;
import com.alikeyou.itmoduleproject.repository.ProjectMemberRepository;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import com.alikeyou.itmoduleproject.repository.ProjectSprintRepository;
import com.alikeyou.itmoduleproject.repository.ProjectStarRepository;
import com.alikeyou.itmoduleproject.repository.ProjectStatDailyRepository;
import com.alikeyou.itmoduleproject.repository.ProjectTaskRepository;
import com.alikeyou.itmoduleproject.service.ProjectStatService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectStatServiceImpl implements ProjectStatService {

    private final ProjectRepository projectRepository;
    private final ProjectStatDailyRepository projectStatDailyRepository;
    private final ProjectDownloadRecordRepository projectDownloadRecordRepository;
    private final ProjectStarRepository projectStarRepository;
    private final ProjectTaskRepository projectTaskRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectSprintRepository projectSprintRepository;
    private final ProjectPermissionService projectPermissionService;

    @Override
    public Map<String, Object> getOverview(Long projectId, Long currentUserId) {
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new BusinessException("项目不存在"));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("projectId", projectId);
        map.put("viewCount", project.getViews() == null ? 0 : project.getViews());
        map.put("downloadCount", projectDownloadRecordRepository.countByProjectId(projectId));
        map.put("uniqueDownloadUserCount", projectDownloadRecordRepository.countDistinctUserIdByProjectId(projectId));
        map.put("starCount", projectStarRepository.countByProjectId(projectId));
        map.put("activeMemberCount", projectMemberRepository.findByProjectIdAndStatusOrderByJoinedAtAsc(projectId, "active").size());
        map.put("taskCount", projectTaskRepository.findByProjectIdOrderByCreatedAtDesc(projectId).size());
        map.put("activeSprint", projectSprintRepository.findFirstByProjectIdAndStatusOrderByCreatedAtDesc(projectId, "active").orElse(null));
        List<ProjectStatDaily> last7Days = projectStatDailyRepository.findByProjectIdAndStatDateBetweenOrderByStatDateAsc(projectId, LocalDate.now().minusDays(6), LocalDate.now());
        map.put("last7Days", last7Days);
        return map;
    }

    @Override
    public List<ProjectStatDaily> getTrend(Long projectId, LocalDate startDate, LocalDate endDate, Long currentUserId) {
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        LocalDate start = startDate == null ? LocalDate.now().minusDays(29) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        if (end.isBefore(start)) {
            throw new BusinessException("结束日期不能早于开始日期");
        }
        return projectStatDailyRepository.findByProjectIdAndStatDateBetweenOrderByStatDateAsc(projectId, start, end);
    }

    @Override
    public PageResult<ProjectStatDaily> pageDaily(Long projectId, int page, int size, Long currentUserId) {
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 10;
        }
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        Page<ProjectStatDaily> result = projectStatDailyRepository.findByProjectIdOrderByStatDateDesc(projectId, PageRequest.of(page - 1, size));
        return new PageResult<>(result.getContent(), result.getTotalElements(), page, size);
    }

    @Override
    @Transactional
    public List<ProjectStatDaily> rebuildDailyStats(Long projectId, LocalDate startDate, LocalDate endDate, Long currentUserId) {
        projectPermissionService.assertProjectManageMembers(projectId, currentUserId);
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new BusinessException("项目不存在"));
        LocalDate start = startDate == null ? LocalDate.now().minusDays(29) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        if (end.isBefore(start)) {
            throw new BusinessException("结束日期不能早于开始日期");
        }

        List<ProjectDownloadRecord> downloadRecords = projectDownloadRecordRepository.findByProjectIdAndDownloadedAtBetweenOrderByDownloadedAtAsc(projectId, start.atStartOfDay(), LocalDateTime.of(end, LocalTime.MAX));
        List<ProjectStar> stars = projectStarRepository.findAll().stream().filter(item -> projectId.equals(item.getProjectId())).toList();
        List<ProjectTask> tasks = projectTaskRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        List<ProjectMember> members = projectMemberRepository.findByProjectIdOrderByJoinedAtAsc(projectId);
        List<ProjectStatDaily> result = new java.util.ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            LocalDate day = d;
            int downloadCount = (int) downloadRecords.stream().filter(item -> isSameDay(item.getDownloadedAt(), day)).count();
            Set<Long> downloadUsers = new LinkedHashSet<>();
            downloadRecords.stream().filter(item -> isSameDay(item.getDownloadedAt(), day) && item.getUserId() != null).forEach(item -> downloadUsers.add(item.getUserId()));
            int starCount = (int) stars.stream().filter(item -> isSameDay(item.getCreatedAt(), day)).count();
            int newMemberCount = (int) members.stream().filter(item -> isSameDay(item.getJoinedAt(), day)).count();
            int memberActiveCount = (int) members.stream().filter(item -> "active".equals(item.getStatus())).count();
            int taskCreatedCount = (int) tasks.stream().filter(item -> isSameDay(item.getCreatedAt(), day)).count();
            int taskCompletedCount = (int) tasks.stream().filter(item -> isSameDay(item.getCompletedAt(), day)).count();
            ProjectStatDaily stat = projectStatDailyRepository.findByProjectIdAndStatDate(projectId, day).orElse(ProjectStatDaily.builder()
                    .projectId(projectId)
                    .statDate(day)
                    .build());
            stat.setViewCount(0);
            stat.setUniqueVisitorCount(0);
            stat.setDownloadCount(downloadCount);
            stat.setUniqueDownloadUserCount(downloadUsers.size());
            stat.setStarCount(starCount);
            stat.setCommentCount(0);
            stat.setMemberActiveCount(memberActiveCount);
            stat.setNewMemberCount(newMemberCount);
            stat.setTaskCreatedCount(taskCreatedCount);
            stat.setTaskCompletedCount(taskCompletedCount);
            stat.setRevenueAmount(BigDecimal.ZERO);
            result.add(projectStatDailyRepository.save(stat));
        }

        if (project.getDownloads() == null || project.getDownloads() < downloadRecords.size()) {
            project.setDownloads(downloadRecords.size());
            projectRepository.save(project);
        }
        return result;
    }

    private boolean isSameDay(LocalDateTime time, LocalDate day) {
        return time != null && time.toLocalDate().equals(day);
    }
}
