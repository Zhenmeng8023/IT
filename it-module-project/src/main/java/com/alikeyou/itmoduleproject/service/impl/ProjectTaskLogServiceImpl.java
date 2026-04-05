package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.ProjectTaskLog;
import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import com.alikeyou.itmoduleproject.repository.ProjectTaskLogRepository;
import com.alikeyou.itmoduleproject.service.ProjectTaskLogService;
import com.alikeyou.itmoduleproject.support.ProjectTaskAccessSupport;
import com.alikeyou.itmoduleproject.support.ProjectUserAssembler;
import com.alikeyou.itmoduleproject.vo.TaskLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectTaskLogServiceImpl implements ProjectTaskLogService {
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ProjectTaskLogRepository projectTaskLogRepository;
    private final ProjectTaskAccessSupport taskAccessSupport;
    private final ProjectUserAssembler projectUserAssembler;

    @Override
    public List<TaskLogVO> listLogs(Long taskId, String action, Long operatorId, String startTime, String endTime, Long currentUserId) {
        taskAccessSupport.assertTaskReadable(taskId, currentUserId);
        List<ProjectTaskLog> logs = projectTaskLogRepository.findByTaskIdOrderByCreatedAtDesc(taskId);

        LocalDateTime start = parseDateTime(startTime, false);
        LocalDateTime end = parseDateTime(endTime, true);

        List<Long> relatedUserIds = new ArrayList<>();
        for (ProjectTaskLog log : logs) {
            if (log.getOperatorId() != null) {
                relatedUserIds.add(log.getOperatorId());
            }
            if ("assignee_id".equalsIgnoreCase(log.getFieldName())) {
                Long oldUserId = parseLong(log.getOldValue());
                Long newUserId = parseLong(log.getNewValue());
                if (oldUserId != null) {
                    relatedUserIds.add(oldUserId);
                }
                if (newUserId != null) {
                    relatedUserIds.add(newUserId);
                }
            }
        }

        Map<Long, UserInfoLite> userMap = projectUserAssembler.mapByIds(relatedUserIds);

        return logs.stream()
                .filter(log -> matchAction(log, action))
                .filter(log -> matchOperator(log, operatorId))
                .filter(log -> matchTime(log, start, end))
                .map(log -> toTaskLogVO(log, userMap))
                .toList();
    }

    @Override
    @Transactional
    public void recordCreate(Long taskId, Long operatorId) {
        save(taskId, operatorId, "create", null, null, null);
    }

    @Override
    @Transactional
    public void recordDelete(Long taskId, Long operatorId, String oldValue) {
        save(taskId, operatorId, "delete", "task", oldValue, null);
    }

    @Override
    @Transactional
    public void recordFieldChange(Long taskId, Long operatorId, String action, String fieldName, Object oldValue, Object newValue) {
        if (same(oldValue, newValue)) {
            return;
        }
        save(taskId, operatorId, normalizeAction(action), fieldName, stringify(oldValue), stringify(newValue));
    }

    @Override
    @Transactional
    public void recordComment(Long taskId, Long operatorId, Object oldValue, Object newValue) {
        save(taskId, operatorId, "comment", "comment", stringify(oldValue), stringify(newValue));
    }

    @Override
    @Transactional
    public void recordAttachment(Long taskId, Long operatorId, Object oldValue, Object newValue) {
        save(taskId, operatorId, "attach", "attachment", stringify(oldValue), stringify(newValue));
    }

    private TaskLogVO toTaskLogVO(ProjectTaskLog log, Map<Long, UserInfoLite> userMap) {
        UserInfoLite operator = userMap.get(log.getOperatorId());
        String oldValueDisplay = formatDisplayValue(log.getFieldName(), log.getOldValue(), userMap);
        String newValueDisplay = formatDisplayValue(log.getFieldName(), log.getNewValue(), userMap);
        LocalDateTime createdAt = log.getCreatedAt();
        return TaskLogVO.builder()
                .id(log.getId())
                .taskId(log.getTaskId())
                .operatorId(log.getOperatorId())
                .operatorName(resolveDisplayName(operator))
                .operatorAvatar(operator == null ? null : operator.getAvatarUrl())
                .action(log.getAction())
                .actionLabel(resolveActionLabel(log.getAction()))
                .actionTagType(resolveActionTagType(log.getAction()))
                .fieldName(log.getFieldName())
                .fieldLabel(resolveFieldLabel(log.getFieldName()))
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .oldValueDisplay(oldValueDisplay)
                .newValueDisplay(newValueDisplay)
                .createdAt(createdAt)
                .groupDay(createdAt == null ? null : createdAt.format(DAY_FORMATTER))
                .build();
    }

    private boolean matchAction(ProjectTaskLog log, String action) {
        if (!StringUtils.hasText(action)) {
            return true;
        }
        return action.trim().equalsIgnoreCase(Objects.toString(log.getAction(), ""));
    }

    private boolean matchOperator(ProjectTaskLog log, Long operatorId) {
        if (operatorId == null) {
            return true;
        }
        return Objects.equals(operatorId, log.getOperatorId());
    }

    private boolean matchTime(ProjectTaskLog log, LocalDateTime start, LocalDateTime end) {
        if (log.getCreatedAt() == null) {
            return start == null && end == null;
        }
        if (start != null && log.getCreatedAt().isBefore(start)) {
            return false;
        }
        if (end != null && log.getCreatedAt().isAfter(end)) {
            return false;
        }
        return true;
    }

    private void save(Long taskId, Long operatorId, String action, String fieldName, String oldValue, String newValue) {
        projectTaskLogRepository.save(ProjectTaskLog.builder()
                .taskId(taskId)
                .operatorId(operatorId)
                .action(action)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .build());
    }

    private String normalizeAction(String action) {
        if (!StringUtils.hasText(action)) {
            return "update";
        }
        String value = action.trim();
        return switch (value) {
            case "create", "update", "assign", "change_status", "change_priority", "comment", "attach", "complete", "reopen", "delete", "reopen_request", "reopen_approve", "reopen_reject", "reopen_cancel" -> value;
            case "approve_reopen", "approve_reopen_task" -> "reopen_approve";
            case "reject_reopen", "reject_reopen_task" -> "reopen_reject";
            case "cancel_reopen", "cancel_reopen_task" -> "reopen_cancel";
            case "request_reopen", "request_reopen_task" -> "reopen_request";
            default -> "update";
        };
    }

    private boolean same(Object oldValue, Object newValue) {
        return stringify(oldValue).equals(stringify(newValue));
    }

    private String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
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
            case "create" -> "创建任务";
            case "update" -> "更新任务";
            case "assign" -> "变更负责人";
            case "change_status" -> "修改状态";
            case "change_priority" -> "修改优先级";
            case "comment" -> "发表评论";
            case "attach" -> "上传附件";
            case "complete" -> "完成任务";
            case "reopen" -> "重新打开任务";
            case "reopen_request" -> "申请重开";
            case "reopen_approve" -> "通过重开";
            case "reopen_reject" -> "驳回重开";
            case "reopen_cancel" -> "撤销重开";
            case "delete" -> "删除内容";
            default -> StringUtils.hasText(action) ? action : "任务动态";
        };
    }

    private String resolveActionTagType(String action) {
        return switch (Objects.toString(action, "")) {
            case "create", "complete", "reopen_approve" -> "success";
            case "delete", "reopen_reject" -> "danger";
            case "change_priority", "reopen_request" -> "warning";
            case "comment", "attach" -> "primary";
            default -> "info";
        };
    }

    private String resolveFieldLabel(String fieldName) {
        return switch (Objects.toString(fieldName, "")) {
            case "title" -> "标题";
            case "description" -> "描述";
            case "priority" -> "优先级";
            case "assignee_id" -> "负责人";
            case "status" -> "状态";
            case "due_date" -> "截止时间";
            case "comment" -> "评论";
            case "attachment" -> "附件";
            case "task" -> "任务";
            case "reopen_reason" -> "重开原因";
            default -> StringUtils.hasText(fieldName) ? fieldName : "";
        };
    }

    private String formatDisplayValue(String fieldName, String rawValue, Map<Long, UserInfoLite> userMap) {
        if (!StringUtils.hasText(rawValue)) {
            return "";
        }
        String normalizedField = Objects.toString(fieldName, "").toLowerCase(Locale.ROOT);
        return switch (normalizedField) {
            case "status" -> mapStatus(rawValue);
            case "priority" -> mapPriority(rawValue);
            case "assignee_id" -> mapUser(rawValue, userMap);
            case "due_date", "created_at", "updated_at", "completed_at" -> mapDateTime(rawValue);
            default -> rawValue;
        };
    }

    private String mapStatus(String rawValue) {
        return switch (rawValue.trim().toLowerCase(Locale.ROOT)) {
            case "todo" -> "待处理";
            case "in_progress" -> "进行中";
            case "done" -> "已完成";
            default -> rawValue;
        };
    }

    private String mapPriority(String rawValue) {
        return switch (rawValue.trim().toLowerCase(Locale.ROOT)) {
            case "low" -> "低";
            case "medium" -> "中";
            case "high" -> "高";
            case "urgent" -> "紧急";
            default -> rawValue;
        };
    }

    private String mapUser(String rawValue, Map<Long, UserInfoLite> userMap) {
        Long userId = parseLong(rawValue);
        if (userId == null) {
            return rawValue;
        }
        UserInfoLite user = userMap.get(userId);
        String displayName = resolveDisplayName(user);
        if (StringUtils.hasText(displayName)) {
            return displayName;
        }
        return "用户#" + userId;
    }

    private String mapDateTime(String rawValue) {
        LocalDateTime value = parseDateTime(rawValue, false);
        if (value == null) {
            return rawValue;
        }
        return value.format(DATE_TIME_FORMATTER);
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
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
