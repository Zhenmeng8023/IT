package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.TaskCommentCreateRequest;
import com.alikeyou.itmoduleproject.entity.ProjectTaskComment;
import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import com.alikeyou.itmoduleproject.repository.ProjectTaskCommentRepository;
import com.alikeyou.itmoduleproject.service.ProjectTaskCommentService;
import com.alikeyou.itmoduleproject.service.ProjectTaskLogService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectTaskAccessSupport;
import com.alikeyou.itmoduleproject.support.ProjectUserAssembler;
import com.alikeyou.itmoduleproject.support.ProjectVoMapper;
import com.alikeyou.itmoduleproject.vo.TaskCommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectTaskCommentServiceImpl implements ProjectTaskCommentService {
    private final ProjectTaskCommentRepository projectTaskCommentRepository;
    private final ProjectTaskAccessSupport taskAccessSupport;
    private final ProjectUserAssembler projectUserAssembler;
    private final ProjectTaskLogService projectTaskLogService;

    @Override
    public List<TaskCommentVO> listComments(Long taskId, Long currentUserId) {
        taskAccessSupport.assertTaskReadable(taskId, currentUserId);

        List<ProjectTaskComment> comments = projectTaskCommentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
        if (comments == null || comments.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> authorIds = comments.stream()
                .map(ProjectTaskComment::getAuthorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, UserInfoLite> userMap = authorIds.isEmpty()
                ? new LinkedHashMap<>()
                : projectUserAssembler.mapByIds(authorIds);

        Map<Long, TaskCommentVO> nodeMap = new LinkedHashMap<>();
        List<TaskCommentVO> roots = new ArrayList<>();
        boolean canManage = taskAccessSupport.canManageTask(taskId, currentUserId);

        for (ProjectTaskComment comment : comments) {
            TaskCommentVO current = toVO(
                    comment,
                    userMap.get(comment.getAuthorId()),
                    currentUserId,
                    canManage,
                    new ArrayList<>()
            );
            nodeMap.put(comment.getId(), current);
        }

        for (ProjectTaskComment comment : comments) {
            TaskCommentVO current = nodeMap.get(comment.getId());
            if (comment.getParentCommentId() == null) {
                roots.add(current);
                continue;
            }
            TaskCommentVO parent = nodeMap.get(comment.getParentCommentId());
            if (parent == null) {
                roots.add(current);
                continue;
            }
            if (parent.getReplies() == null) {
                parent.setReplies(new ArrayList<>());
            }
            parent.getReplies().add(current);
        }

        return roots;
    }

    @Override
    @Transactional
    public TaskCommentVO addComment(Long taskId, TaskCommentCreateRequest request, Long currentUserId) {
        taskAccessSupport.assertTaskReadable(taskId, currentUserId);
        String content = requireText(request == null ? null : request.getContent(), "评论内容不能为空");
        ProjectTaskComment saved = projectTaskCommentRepository.save(ProjectTaskComment.builder()
                .taskId(taskId)
                .authorId(currentUserId)
                .content(content)
                .status("normal")
                .build());
        projectTaskLogService.recordComment(taskId, currentUserId, null, content);
        UserInfoLite author = projectUserAssembler.mapByIds(List.of(currentUserId)).get(currentUserId);
        return toVO(saved, author, currentUserId, taskAccessSupport.canManageTask(taskId, currentUserId), new ArrayList<>());
    }

    @Override
    @Transactional
    public TaskCommentVO replyComment(Long commentId, TaskCommentCreateRequest request, Long currentUserId) {
        ProjectTaskComment parent = getComment(commentId);
        taskAccessSupport.assertTaskReadable(parent.getTaskId(), currentUserId);
        String content = requireText(request == null ? null : request.getContent(), "回复内容不能为空");
        ProjectTaskComment saved = projectTaskCommentRepository.save(ProjectTaskComment.builder()
                .taskId(parent.getTaskId())
                .authorId(currentUserId)
                .parentCommentId(parent.getId())
                .content(content)
                .status("normal")
                .build());
        projectTaskLogService.recordComment(parent.getTaskId(), currentUserId, null, content);
        UserInfoLite author = projectUserAssembler.mapByIds(List.of(currentUserId)).get(currentUserId);
        return toVO(saved, author, currentUserId, taskAccessSupport.canManageTask(parent.getTaskId(), currentUserId), new ArrayList<>());
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long currentUserId) {
        ProjectTaskComment comment = getComment(commentId);
        boolean canManage = taskAccessSupport.canManageTask(comment.getTaskId(), currentUserId);
        if (!Objects.equals(comment.getAuthorId(), currentUserId) && !canManage) {
            throw new BusinessException("无权删除该评论");
        }
        String old = comment.getContent();
        comment.setStatus("deleted");
        comment.setContent("[该评论已删除]");
        projectTaskCommentRepository.save(comment);
        projectTaskLogService.recordComment(comment.getTaskId(), currentUserId, old, "[deleted]");
    }

    private ProjectTaskComment getComment(Long commentId) {
        return projectTaskCommentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException("评论不存在"));
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private TaskCommentVO toVO(ProjectTaskComment comment, UserInfoLite author, Long currentUserId, boolean canManage, List<TaskCommentVO> replies) {
        boolean deleted = !"normal".equalsIgnoreCase(comment.getStatus());
        return ProjectVoMapper.toTaskCommentVO(
                comment,
                author,
                replies,
                Objects.equals(comment.getAuthorId(), currentUserId) || canManage,
                deleted
        );
    }
}
