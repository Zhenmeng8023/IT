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
            case "create_project" -> "创建项目";
            case "update_project" -> "编辑项目";
            case "save_as_template" -> "保存模板";
            case "apply_template" -> "套用模板";
            case "add_member" -> "新增成员";
            case "remove_member" -> "移除成员";
            case "quit_project" -> "退出项目";
            case "create_invite" -> "发起邀请";
            case "accept_invite" -> "接受邀请";
            case "cancel_invite" -> "取消邀请";
            case "submit_join_request" -> "提交加入申请";
            case "approve_join_request" -> "通过加入申请";
            case "reject_join_request" -> "拒绝加入申请";
            case "upload_file" -> "上传文件";
            case "delete_file" -> "删除文件";
            case "set_main_file" -> "设主文件";
            case "download_project_file" -> "下载文件";
            case "create_doc" -> "新建文档";
            case "update_doc" -> "更新文档";
            case "rollback_doc" -> "回滚文档";
            case "set_primary_doc" -> "设为主文档";
            case "delete_doc" -> "删除文档";
            case "create_task" -> "创建任务";
            case "update_task" -> "更新任务";
            case "change_task_status" -> "修改任务状态";
            case "delete_task" -> "删除任务";
            case "create_milestone" -> "创建里程碑";
            case "update_milestone" -> "更新里程碑";
            case "change_milestone_status" -> "修改里程碑状态";
            case "delete_milestone" -> "删除里程碑";
            case "create_release" -> "创建发布版本";
            case "update_release" -> "更新发布版本";
            case "publish_release" -> "发布版本";
            case "archive_release" -> "归档版本";
            case "bind_release_files" -> "绑定发布文件";
            case "remove_release_file" -> "移除发布文件";
        case "create_sprint" -> "创建迭代";
        case "update_sprint" -> "更新迭代";
        case "change_sprint_status" -> "修改迭代状态";
        case "mr_create" -> "创建合并请求";
        case "mr_review" -> "审核合并请求";
        case "mr_merge_check" -> "执行合并检查";
        case "mr_merge" -> "合并合并请求";
        default -> StringUtils.hasText(action) ? action : "项目动态";
        };
    }

    private String resolveActionTagType(String action) {
        return switch (Objects.toString(action, "")) {
        case "create_project", "create_doc", "create_task", "add_member", "upload_file", "save_as_template",
             "apply_template", "create_invite", "accept_invite", "approve_join_request", "create_milestone",
             "create_release", "publish_release", "create_sprint", "download_project_file", "mr_create",
             "mr_merge" -> "success";
            case "delete_doc", "delete_task", "delete_file", "remove_member", "quit_project", "cancel_invite",
                 "reject_join_request", "delete_milestone", "archive_release", "remove_release_file" -> "danger";
        case "rollback_doc", "change_task_status", "set_primary_doc", "set_main_file", "submit_join_request",
             "change_milestone_status", "update_release", "bind_release_files", "change_sprint_status",
             "mr_review", "mr_merge_check" -> "warning";
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
