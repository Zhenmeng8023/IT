package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.dto.TaskChecklistItemCreateRequest;
import com.alikeyou.itmoduleproject.dto.TaskChecklistItemUpdateRequest;
import com.alikeyou.itmoduleproject.dto.TaskChecklistSortRequest;
import com.alikeyou.itmoduleproject.vo.TaskChecklistItemVO;

import java.util.List;

public interface ProjectTaskChecklistService {
    List<TaskChecklistItemVO> listChecklist(Long taskId, Long currentUserId);

    TaskChecklistItemVO addItem(Long taskId, TaskChecklistItemCreateRequest request, Long currentUserId);

    TaskChecklistItemVO updateItem(Long itemId, TaskChecklistItemUpdateRequest request, Long currentUserId);

    TaskChecklistItemVO toggleItem(Long itemId, Long currentUserId);

    void deleteItem(Long itemId, Long currentUserId);

    List<TaskChecklistItemVO> sortItems(Long taskId, TaskChecklistSortRequest request, Long currentUserId);
}
