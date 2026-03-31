package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.TaskChecklistItemCreateRequest;
import com.alikeyou.itmoduleproject.dto.TaskChecklistItemUpdateRequest;
import com.alikeyou.itmoduleproject.dto.TaskChecklistSortRequest;
import com.alikeyou.itmoduleproject.entity.ProjectTaskChecklistItem;
import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import com.alikeyou.itmoduleproject.repository.ProjectTaskChecklistItemRepository;
import com.alikeyou.itmoduleproject.service.ProjectTaskChecklistService;
import com.alikeyou.itmoduleproject.service.ProjectTaskLogService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectTaskAccessSupport;
import com.alikeyou.itmoduleproject.support.ProjectUserAssembler;
import com.alikeyou.itmoduleproject.support.ProjectVoMapper;
import com.alikeyou.itmoduleproject.vo.TaskChecklistItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectTaskChecklistServiceImpl implements ProjectTaskChecklistService {

    private final ProjectTaskChecklistItemRepository projectTaskChecklistItemRepository;
    private final ProjectTaskAccessSupport taskAccessSupport;
    private final ProjectUserAssembler projectUserAssembler;
    private final ProjectTaskLogService projectTaskLogService;

    @Override
    public List<TaskChecklistItemVO> listChecklist(Long taskId, Long currentUserId) {
        taskAccessSupport.assertTaskReadable(taskId, currentUserId);
        List<ProjectTaskChecklistItem> items = projectTaskChecklistItemRepository.findByTaskIdOrderBySortOrderAscIdAsc(taskId);
        return toVOList(items);
    }

    @Override
    @Transactional
    public TaskChecklistItemVO addItem(Long taskId, TaskChecklistItemCreateRequest request, Long currentUserId) {
        taskAccessSupport.assertTaskReadable(taskId, currentUserId);

        ProjectTaskChecklistItem saved = projectTaskChecklistItemRepository.save(
                ProjectTaskChecklistItem.builder()
                        .taskId(taskId)
                        .content(requireText(request == null ? null : request.getContent(), "检查项内容不能为空"))
                        .sortOrder(request != null && request.getSortOrder() != null ? request.getSortOrder() : defaultSort(taskId))
                        .checked(Boolean.FALSE)
                        .createdBy(currentUserId)
                        .checkedBy(null)
                        .checkedAt(null)
                        .build()
        );

        projectTaskLogService.recordFieldChange(taskId, currentUserId, "update", "checklist_item", null, saved.getContent());
        return toVO(saved);
    }

    @Override
    @Transactional
    public TaskChecklistItemVO updateItem(Long itemId, TaskChecklistItemUpdateRequest request, Long currentUserId) {
        ProjectTaskChecklistItem item = getItem(itemId);
        taskAccessSupport.assertTaskReadable(item.getTaskId(), currentUserId);

        String oldContent = item.getContent();
        Integer oldSort = item.getSortOrder();

        if (request != null && request.getContent() != null) {
            item.setContent(requireText(request.getContent(), "检查项内容不能为空"));
        }
        if (request != null && request.getSortOrder() != null) {
            item.setSortOrder(request.getSortOrder());
        }

        ProjectTaskChecklistItem saved = projectTaskChecklistItemRepository.save(item);

        if (!Objects.equals(oldContent, saved.getContent())) {
            projectTaskLogService.recordFieldChange(item.getTaskId(), currentUserId, "update", "checklist_item", oldContent, saved.getContent());
        }
        if (!Objects.equals(oldSort, saved.getSortOrder())) {
            projectTaskLogService.recordFieldChange(item.getTaskId(), currentUserId, "update", "checklist_sort", oldSort, saved.getSortOrder());
        }

        return toVO(saved);
    }

    @Override
    @Transactional
    public TaskChecklistItemVO toggleItem(Long itemId, Long currentUserId) {
        ProjectTaskChecklistItem item = getItem(itemId);
        taskAccessSupport.assertTaskReadable(item.getTaskId(), currentUserId);

        boolean next = !Boolean.TRUE.equals(item.getChecked());
        item.setChecked(next);

        if (next) {
            item.setCheckedBy(currentUserId);
            item.setCheckedAt(LocalDateTime.now());
        } else {
            item.setCheckedBy(null);
            item.setCheckedAt(null);
        }

        ProjectTaskChecklistItem saved = projectTaskChecklistItemRepository.save(item);
        projectTaskLogService.recordFieldChange(item.getTaskId(), currentUserId, "update", "checklist_checked", !next, next);
        return toVO(saved);
    }

    @Override
    @Transactional
    public void deleteItem(Long itemId, Long currentUserId) {
        ProjectTaskChecklistItem item = getItem(itemId);
        taskAccessSupport.assertTaskReadable(item.getTaskId(), currentUserId);

        String oldContent = item.getContent();
        Long taskId = item.getTaskId();

        projectTaskChecklistItemRepository.delete(item);
        projectTaskLogService.recordFieldChange(taskId, currentUserId, "update", "checklist_item", oldContent, null);
    }

    @Override
    @Transactional
    public List<TaskChecklistItemVO> sortItems(Long taskId, TaskChecklistSortRequest request, Long currentUserId) {
        taskAccessSupport.assertTaskReadable(taskId, currentUserId);

        Map<Long, Integer> orderMap = new HashMap<>();
        if (request != null && request.getItems() != null) {
            for (TaskChecklistSortRequest.Item item : request.getItems()) {
                if (item != null && item.getId() != null && item.getSortOrder() != null) {
                    orderMap.put(item.getId(), item.getSortOrder());
                }
            }
        }

        List<ProjectTaskChecklistItem> items = projectTaskChecklistItemRepository.findByTaskIdOrderBySortOrderAscIdAsc(taskId);
        for (ProjectTaskChecklistItem item : items) {
            Integer next = orderMap.get(item.getId());
            if (next != null) {
                item.setSortOrder(next);
            }
        }

        projectTaskChecklistItemRepository.saveAll(items);
        projectTaskLogService.recordFieldChange(taskId, currentUserId, "update", "checklist_sort", null, "reordered");

        return toVOList(projectTaskChecklistItemRepository.findByTaskIdOrderBySortOrderAscIdAsc(taskId));
    }

    private ProjectTaskChecklistItem getItem(Long itemId) {
        return projectTaskChecklistItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException("检查项不存在"));
    }

    private int defaultSort(Long taskId) {
        return (int) projectTaskChecklistItemRepository.countByTaskId(taskId);
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private List<TaskChecklistItemVO> toVOList(List<ProjectTaskChecklistItem> items) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> userIds = new ArrayList<>();
        for (ProjectTaskChecklistItem item : items) {
            if (item.getCreatedBy() != null) {
                userIds.add(item.getCreatedBy());
            }
            if (item.getCheckedBy() != null) {
                userIds.add(item.getCheckedBy());
            }
        }

        Map<Long, UserInfoLite> userMap = loadUserMap(userIds);
        List<TaskChecklistItemVO> result = new ArrayList<>();
        for (ProjectTaskChecklistItem item : items) {
            result.add(ProjectVoMapper.toTaskChecklistItemVO(
                    item,
                    userMap.get(item.getCreatedBy()),
                    userMap.get(item.getCheckedBy())
            ));
        }
        return result;
    }

    private TaskChecklistItemVO toVO(ProjectTaskChecklistItem item) {
        List<Long> userIds = new ArrayList<>();
        if (item.getCreatedBy() != null) {
            userIds.add(item.getCreatedBy());
        }
        if (item.getCheckedBy() != null) {
            userIds.add(item.getCheckedBy());
        }

        Map<Long, UserInfoLite> userMap = loadUserMap(userIds);
        return ProjectVoMapper.toTaskChecklistItemVO(
                item,
                userMap.get(item.getCreatedBy()),
                userMap.get(item.getCheckedBy())
        );
    }

    private Map<Long, UserInfoLite> loadUserMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new LinkedHashMap<>();
        }

        List<Long> distinctIds = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (distinctIds.isEmpty()) {
            return new LinkedHashMap<>();
        }

        return projectUserAssembler.mapByIds(distinctIds);
    }
}