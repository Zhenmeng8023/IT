package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.ProjectTaskLog;
import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import com.alikeyou.itmoduleproject.repository.ProjectTaskLogRepository;
import com.alikeyou.itmoduleproject.service.ProjectTaskLogService;
import com.alikeyou.itmoduleproject.support.ProjectTaskAccessSupport;
import com.alikeyou.itmoduleproject.support.ProjectUserAssembler;
import com.alikeyou.itmoduleproject.support.ProjectVoMapper;
import com.alikeyou.itmoduleproject.vo.TaskLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjectTaskLogServiceImpl implements ProjectTaskLogService {
    private final ProjectTaskLogRepository projectTaskLogRepository;
    private final ProjectTaskAccessSupport taskAccessSupport;
    private final ProjectUserAssembler projectUserAssembler;

    @Override
    public List<TaskLogVO> listLogs(Long taskId, Long currentUserId) {
        taskAccessSupport.assertTaskReadable(taskId, currentUserId);
        List<ProjectTaskLog> logs = projectTaskLogRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
        Map<Long, UserInfoLite> userMap = projectUserAssembler.mapByIds(logs.stream().map(ProjectTaskLog::getOperatorId).toList());
        return logs.stream().map(log -> ProjectVoMapper.toTaskLogVO(log, userMap.get(log.getOperatorId()))).toList();
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
        if (action == null || action.isBlank()) {
            return "update";
        }
        return action;
    }

    private boolean same(Object oldValue, Object newValue) {
        return stringify(oldValue).equals(stringify(newValue));
    }

    private String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
