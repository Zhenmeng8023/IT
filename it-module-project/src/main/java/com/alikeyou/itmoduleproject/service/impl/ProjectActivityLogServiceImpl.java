package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.ProjectActivityLog;
import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import com.alikeyou.itmoduleproject.repository.ProjectActivityLogRepository;
import com.alikeyou.itmoduleproject.repository.UserInfoLiteRepository;
import com.alikeyou.itmoduleproject.service.ProjectActivityLogService;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.vo.PageResult;
import com.alikeyou.itmoduleproject.vo.ProjectActivityPositionVO;
import com.alikeyou.itmoduleproject.vo.ProjectActivityVO;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectActivityLogServiceImpl implements ProjectActivityLogService {

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ProjectActivityLogRepository projectActivityLogRepository;
    private final ProjectPermissionService projectPermissionService;
    private final UserInfoLiteRepository userInfoLiteRepository;

    @Override
    @Transactional
    public void record(Long projectId, Long operatorId, String action, String targetType, Long targetId, String content) {
        if (projectId == null || !StringUtils.hasText(action) || !StringUtils.hasText(targetType)) {
            return;
        }
        projectActivityLogRepository.save(ProjectActivityLog.builder()
                .projectId(projectId)
                .operatorId(operatorId)
                .action(action.trim())
                .targetType(targetType.trim())
                .targetId(targetId)
                .content(normalizeSummary(content))
                .details(null)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectActivityVO> listActivities(Long projectId, String action, String targetType, Long operatorId, String startTime, String endTime, Long currentUserId) {
        PageResult<ProjectActivityVO> pageResult = pageActivities(projectId, action, targetType, operatorId, startTime, endTime, 1, 50, currentUserId);
        return pageResult.getList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProjectActivityVO> pageActivities(Long projectId, String action, String targetType, Long operatorId, String startTime, String endTime, int page, int size, Long currentUserId) {
        projectPermissionService.assertProjectReadable(projectId, currentUserId);

        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        Specification<ProjectActivityLog> specification = buildSpecification(projectId, action, targetType, operatorId, startTime, endTime);
        PageRequest pageable = PageRequest.of(
                safePage - 1,
                safeSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );

        Page<ProjectActivityLog> pageData = projectActivityLogRepository.findAll(specification, pageable);
        List<ProjectActivityLog> logs = pageData.getContent();
        Map<Long, UserInfoLite> userMap = loadUserMap(logs);
        List<ProjectActivityVO> result = logs.stream()
                .map(item -> toVO(item, userMap.get(item.getOperatorId())))
                .toList();
        return new PageResult<>(result, pageData.getTotalElements(), safePage, safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectActivityPositionVO getActivityPosition(Long projectId, Long activityId, String action, String targetType, Long operatorId, String startTime, String endTime, int size, Long currentUserId) {
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        int safeSize = Math.max(size, 1);
        ProjectActivityLog target = projectActivityLogRepository.findByIdAndProjectId(activityId, projectId).orElse(null);
        if (target == null) {
            return ProjectActivityPositionVO.builder()
                    .activityId(activityId)
                    .page(1)
                    .size(safeSize)
                    .total(0L)
                    .exists(false)
                    .build();
        }

        Specification<ProjectActivityLog> baseSpecification = buildSpecification(projectId, action, targetType, operatorId, startTime, endTime);
        long total = projectActivityLogRepository.count(baseSpecification);
        long matchedTargetCount = projectActivityLogRepository.count(baseSpecification.and((root, query, cb) -> cb.equal(root.get("id"), activityId)));
        if (matchedTargetCount <= 0) {
            return ProjectActivityPositionVO.builder()
                    .activityId(activityId)
                    .page(1)
                    .size(safeSize)
                    .total(total)
                    .exists(false)
                    .build();
        }
        long beforeCount = countBeforeTarget(projectId, target, action, targetType, operatorId, startTime, endTime);
        int page = (int) (beforeCount / safeSize) + 1;

        return ProjectActivityPositionVO.builder()
                .activityId(activityId)
                .page(page)
                .size(safeSize)
                .total(total)
                .exists(true)
                .build();
    }

    private Specification<ProjectActivityLog> buildSpecification(Long projectId, String action, String targetType, Long operatorId, String startTime, String endTime) {
        LocalDateTime start = parseDateTime(startTime, false);
        LocalDateTime end = parseDateTime(endTime, true);
        return (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(cb.equal(root.get("projectId"), projectId));
            if (StringUtils.hasText(action)) {
                predicates.add(cb.equal(root.get("action"), action.trim()));
            }
            if (StringUtils.hasText(targetType)) {
                predicates.add(cb.equal(root.get("targetType"), targetType.trim()));
            }
            if (operatorId != null) {
                predicates.add(cb.equal(root.get("operatorId"), operatorId));
            }
            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
            }
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private long countBeforeTarget(Long projectId, ProjectActivityLog target, String action, String targetType, Long operatorId, String startTime, String endTime) {
        Specification<ProjectActivityLog> specification = (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(cb.equal(root.get("projectId"), projectId));
            if (StringUtils.hasText(action)) {
                predicates.add(cb.equal(root.get("action"), action.trim()));
            }
            if (StringUtils.hasText(targetType)) {
                predicates.add(cb.equal(root.get("targetType"), targetType.trim()));
            }
            if (operatorId != null) {
                predicates.add(cb.equal(root.get("operatorId"), operatorId));
            }
            LocalDateTime start = parseDateTime(startTime, false);
            LocalDateTime end = parseDateTime(endTime, true);
            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
            }
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
            }
            Predicate newerCreatedAt = cb.greaterThan(root.get("createdAt"), target.getCreatedAt());
            Predicate sameTimeHigherId = cb.and(
                    cb.equal(root.get("createdAt"), target.getCreatedAt()),
                    cb.greaterThan(root.get("id"), target.getId())
            );
            predicates.add(cb.or(newerCreatedAt, sameTimeHigherId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return projectActivityLogRepository.count(specification);
    }

    private Map<Long, UserInfoLite> loadUserMap(List<ProjectActivityLog> logs) {
        return userInfoLiteRepository.findByIdIn(collectUserIds(logs))
                .stream()
                .collect(Collectors.toMap(UserInfoLite::getId, Function.identity(), (a, b) -> a));
    }

    private Collection<Long> collectUserIds(List<ProjectActivityLog> logs) {
        return logs.stream()
                .map(ProjectActivityLog::getOperatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private ProjectActivityVO toVO(ProjectActivityLog item, UserInfoLite operator) {
        return ProjectActivityVO.builder()
                .id(item.getId())
                .projectId(item.getProjectId())
                .operatorId(item.getOperatorId())
                .operatorName(resolveDisplayName(operator))
                .operatorAvatar(operator == null ? null : operator.getAvatarUrl())
                .action(item.getAction())
                .actionLabel(resolveActionLabel(item.getAction()))
                .actionTagType(resolveActionTagType(item.getAction()))
                .targetType(item.getTargetType())
                .targetId(item.getTargetId())
                .content(item.getContent())
                .details(item.getDetails())
                .createdAt(item.getCreatedAt())
                .groupDay(item.getCreatedAt() == null ? null : item.getCreatedAt().format(DAY_FORMATTER))
                .build();
    }

    private String resolveDisplayName(UserInfoLite user) {
        if (user == null) {
            return null;
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname().trim();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername().trim();
        }
        return null;
    }

    private String resolveActionLabel(String action) {
        return switch (Objects.toString(action, "")) {
            case "create_project" -> "Create project";
            case "update_project" -> "Update project";
            case "save_as_template" -> "Save template";
            case "apply_template" -> "Apply template";
            case "add_member" -> "Add member";
            case "remove_member" -> "Remove member";
            case "quit_project" -> "Quit project";
            case "create_invite" -> "Create invite";
            case "accept_invite" -> "Accept invite";
            case "cancel_invite" -> "Cancel invite";
            case "submit_join_request" -> "Submit join request";
            case "approve_join_request" -> "Approve join request";
            case "reject_join_request" -> "Reject join request";
            case "upload_file" -> "Upload file";
            case "delete_file" -> "Delete file";
            case "set_main_file" -> "Set main file";
            case "download_project_file" -> "Download file";
            case "create_doc" -> "Create doc";
            case "update_doc" -> "Update doc";
            case "rollback_doc" -> "Rollback doc";
            case "set_primary_doc" -> "Set primary doc";
            case "delete_doc" -> "Delete doc";
            case "create_task" -> "Create task";
            case "update_task" -> "Update task";
            case "change_task_status" -> "Change task status";
            case "delete_task" -> "Delete task";
            case "create_milestone" -> "Create milestone";
            case "update_milestone" -> "Update milestone";
            case "change_milestone_status" -> "Change milestone status";
            case "delete_milestone" -> "Delete milestone";
            case "create_release" -> "Create release";
            case "update_release" -> "Update release";
            case "publish_release" -> "Publish release";
            case "archive_release" -> "Archive release";
            case "bind_release_files" -> "Bind release files";
            case "remove_release_file" -> "Remove release file";
            case "create_sprint" -> "Create sprint";
            case "update_sprint" -> "Update sprint";
            case "change_sprint_status" -> "Change sprint status";
            case "mr_create" -> "Create merge request";
            case "mr_review" -> "Review merge request";
            case "mr_merge_check" -> "Run merge check";
            case "mr_conflict_resolve" -> "Record conflict resolution";
            case "mr_conflict_resolve_start" -> "Start conflict resolution";
            case "mr_conflict_resolve_apply" -> "Apply conflict strategy";
            case "mr_conflict_resolve_recheck" -> "Auto recheck mergeability";
            case "mr_conflict_resolve_fail" -> "Conflict resolution not mergeable";
            case "mr_merge" -> "Merge merge request";
            default -> StringUtils.hasText(action) ? action : "Project activity";
        };
    }

    private String resolveActionTagType(String action) {
        return switch (Objects.toString(action, "")) {
            case "create_project", "create_doc", "create_task", "add_member", "upload_file", "save_as_template",
                 "apply_template", "create_invite", "accept_invite", "approve_join_request", "create_milestone",
                 "create_release", "publish_release", "create_sprint", "download_project_file", "mr_create",
                 "mr_merge", "mr_conflict_resolve", "mr_conflict_resolve_apply" -> "success";
            case "delete_doc", "delete_task", "delete_file", "remove_member", "quit_project", "cancel_invite",
                 "reject_join_request", "delete_milestone", "archive_release", "remove_release_file",
                 "mr_conflict_resolve_fail" -> "danger";
            case "rollback_doc", "change_task_status", "set_primary_doc", "set_main_file", "submit_join_request",
                 "change_milestone_status", "update_release", "bind_release_files", "change_sprint_status",
                 "mr_review", "mr_merge_check", "mr_conflict_resolve_start", "mr_conflict_resolve_recheck" -> "warning";
            default -> "info";
        };
    }

    private String normalizeSummary(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String text = value.trim();
        return text.length() > 255 ? text.substring(0, 255) : text;
    }

    private LocalDateTime parseDateTime(String value, boolean endOfDay) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim();
        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(text, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(text, DATE_TIME_MINUTE_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }
        try {
            LocalDate date = LocalDate.parse(text, DAY_FORMATTER);
            return endOfDay ? date.atTime(23, 59, 59) : date.atStartOfDay();
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }
}
