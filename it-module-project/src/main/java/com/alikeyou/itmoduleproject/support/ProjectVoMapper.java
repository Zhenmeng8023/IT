package com.alikeyou.itmoduleproject.support;

import com.alikeyou.itmoduleproject.entity.Project;
import com.alikeyou.itmoduleproject.entity.ProjectFile;
import com.alikeyou.itmoduleproject.entity.ProjectFileVersion;
import com.alikeyou.itmoduleproject.entity.ProjectMember;
import com.alikeyou.itmoduleproject.entity.ProjectTask;
import com.alikeyou.itmoduleproject.entity.ProjectTaskAttachment;
import com.alikeyou.itmoduleproject.entity.ProjectTaskChecklistItem;
import com.alikeyou.itmoduleproject.entity.ProjectTaskComment;
import com.alikeyou.itmoduleproject.entity.ProjectTaskDependency;
import com.alikeyou.itmoduleproject.entity.ProjectTaskLog;
import com.alikeyou.itmoduleproject.entity.ProjectTaskReopenRequest;
import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import com.alikeyou.itmoduleproject.vo.ProjectDetailVO;
import com.alikeyou.itmoduleproject.vo.ProjectFileVO;
import com.alikeyou.itmoduleproject.vo.ProjectFileVersionVO;
import com.alikeyou.itmoduleproject.vo.ProjectListVO;
import com.alikeyou.itmoduleproject.vo.ProjectMemberVO;
import com.alikeyou.itmoduleproject.vo.ProjectTaskReopenRequestVO;
import com.alikeyou.itmoduleproject.vo.ProjectTaskVO;
import com.alikeyou.itmoduleproject.vo.TaskAttachmentVO;
import com.alikeyou.itmoduleproject.vo.TaskChecklistItemVO;
import com.alikeyou.itmoduleproject.vo.TaskCommentVO;
import com.alikeyou.itmoduleproject.vo.TaskDependencyVO;
import com.alikeyou.itmoduleproject.vo.TaskLogVO;
import org.springframework.util.StringUtils;

import java.util.List;

public final class ProjectVoMapper {
    private ProjectVoMapper() {
    }

    public static ProjectListVO toProjectListVO(Project project) {
        return toProjectListVO(project, null);
    }

    public static ProjectListVO toProjectListVO(Project project, UserInfoLite author) {
        return ProjectListVO.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .category(project.getCategory())
                .sizeMb(project.getSizeMb())
                .stars(project.getStars())
                .starred(false)
                .downloads(project.getDownloads())
                .views(project.getViews())
                .authorId(project.getAuthorId())
                .authorName(resolveDisplayName(author))
                .authorAvatar(resolveAvatar(author))
                .status(project.getStatus())
                .tags(project.getTags())
                .templateId(project.getTemplateId())
                .visibility(project.getVisibility())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    public static ProjectDetailVO toProjectDetailVO(Project project, List<ProjectMemberVO> members, List<ProjectTaskVO> tasks, List<ProjectFileVO> files) {
        return toProjectDetailVO(project, null, members, tasks, files);
    }

    public static ProjectDetailVO toProjectDetailVO(Project project, UserInfoLite author, List<ProjectMemberVO> members, List<ProjectTaskVO> tasks, List<ProjectFileVO> files) {
        return ProjectDetailVO.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .category(project.getCategory())
                .sizeMb(project.getSizeMb())
                .stars(project.getStars())
                .starred(false)
                .downloads(project.getDownloads())
                .views(project.getViews())
                .authorId(project.getAuthorId())
                .authorName(resolveDisplayName(author))
                .authorAvatar(resolveAvatar(author))
                .status(project.getStatus())
                .tags(project.getTags())
                .templateId(project.getTemplateId())
                .visibility(project.getVisibility())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .members(members)
                .tasks(tasks)
                .files(files)
                .build();
    }

    public static ProjectMemberVO toProjectMemberVO(ProjectMember member) {
        return toProjectMemberVO(member, null);
    }

    public static ProjectMemberVO toProjectMemberVO(ProjectMember member, UserInfoLite user) {
        return ProjectMemberVO.builder()
                .id(member.getId())
                .projectId(member.getProjectId())
                .userId(member.getUserId())
                .username(user == null ? null : user.getUsername())
                .nickname(user == null ? null : user.getNickname())
                .avatar(resolveAvatar(user))
                .role(member.getRole())
                .status(member.getStatus())
                .joinedAt(member.getJoinedAt())
                .build();
    }

    public static ProjectTaskVO toProjectTaskVO(ProjectTask task) {
        return toProjectTaskVO(task, null, null);
    }

    public static ProjectTaskVO toProjectTaskVO(ProjectTask task, UserInfoLite assignee, UserInfoLite creator) {
        return ProjectTaskVO.builder()
                .id(task.getId())
                .projectId(task.getProjectId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .assigneeId(task.getAssigneeId())
                .assigneeName(resolveDisplayName(assignee))
                .assigneeAvatar(resolveAvatar(assignee))
                .createdBy(task.getCreatedBy())
                .creatorName(resolveDisplayName(creator))
                .dueDate(task.getDueDate())
                .completedAt(task.getCompletedAt())
                .completedBy(task.getCompletedBy())
                .completedMemberJoinedAt(task.getCompletedMemberJoinedAt())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    public static ProjectTaskReopenRequestVO toProjectTaskReopenRequestVO(ProjectTaskReopenRequest request, UserInfoLite applicant, UserInfoLite reviewer) {
        return ProjectTaskReopenRequestVO.builder()
                .id(request.getId())
                .taskId(request.getTaskId())
                .projectId(request.getProjectId())
                .applicantId(request.getApplicantId())
                .applicantName(resolveDisplayName(applicant))
                .applicantAvatar(resolveAvatar(applicant))
                .applicantMemberJoinedAt(request.getApplicantMemberJoinedAt())
                .fromStatus(request.getFromStatus())
                .targetStatus(request.getTargetStatus())
                .reason(request.getReason())
                .status(request.getStatus())
                .reviewerId(request.getReviewerId())
                .reviewerName(resolveDisplayName(reviewer))
                .reviewerAvatar(resolveAvatar(reviewer))
                .reviewedAt(request.getReviewedAt())
                .reviewRemark(request.getReviewRemark())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

    public static TaskCommentVO toTaskCommentVO(ProjectTaskComment comment, UserInfoLite author, List<TaskCommentVO> replies, boolean canDelete, boolean deleted) {
        return TaskCommentVO.builder()
                .id(comment.getId())
                .taskId(comment.getTaskId())
                .authorId(comment.getAuthorId())
                .authorName(resolveDisplayName(author))
                .authorAvatar(resolveAvatar(author))
                .parentCommentId(comment.getParentCommentId())
                .content(comment.getContent())
                .status(comment.getStatus())
                .canDelete(canDelete)
                .deleted(deleted)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replies(replies)
                .build();
    }

    public static TaskChecklistItemVO toTaskChecklistItemVO(ProjectTaskChecklistItem item, UserInfoLite creator, UserInfoLite checker) {
        return TaskChecklistItemVO.builder()
                .id(item.getId())
                .taskId(item.getTaskId())
                .content(item.getContent())
                .checked(Boolean.TRUE.equals(item.getChecked()))
                .sortOrder(item.getSortOrder())
                .createdBy(item.getCreatedBy())
                .creatorName(resolveDisplayName(creator))
                .checkedBy(item.getCheckedBy())
                .checkerName(resolveDisplayName(checker))
                .checkedAt(item.getCheckedAt())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    public static TaskAttachmentVO toTaskAttachmentVO(ProjectTaskAttachment attachment, UserInfoLite uploader) {
        return TaskAttachmentVO.builder()
                .id(attachment.getId())
                .taskId(attachment.getTaskId())
                .fileName(attachment.getFileName())
                .filePath(attachment.getFilePath())
                .fileSizeBytes(attachment.getFileSizeBytes())
                .fileType(attachment.getFileType())
                .uploadedBy(attachment.getUploadedBy())
                .uploaderName(resolveDisplayName(uploader))
                .createdAt(attachment.getCreatedAt())
                .previewSupported(isPreviewSupported(attachment.getFileType()))
                .build();
    }

    public static TaskDependencyVO toTaskDependencyVO(ProjectTaskDependency dependency, ProjectTask predecessor, ProjectTask successor, boolean blocked) {
        return TaskDependencyVO.builder()
                .id(dependency.getId())
                .projectId(dependency.getProjectId())
                .predecessorTaskId(dependency.getPredecessorTaskId())
                .predecessorTaskTitle(predecessor == null ? null : predecessor.getTitle())
                .predecessorTaskStatus(predecessor == null ? null : predecessor.getStatus())
                .successorTaskId(dependency.getSuccessorTaskId())
                .successorTaskTitle(successor == null ? null : successor.getTitle())
                .successorTaskStatus(successor == null ? null : successor.getStatus())
                .dependencyType(dependency.getDependencyType())
                .createdAt(dependency.getCreatedAt())
                .blocked(blocked)
                .build();
    }

    public static TaskLogVO toTaskLogVO(ProjectTaskLog log, UserInfoLite operator) {
        return TaskLogVO.builder()
                .id(log.getId())
                .taskId(log.getTaskId())
                .operatorId(log.getOperatorId())
                .operatorName(resolveDisplayName(operator))
                .action(log.getAction())
                .fieldName(log.getFieldName())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .createdAt(log.getCreatedAt())
                .build();
    }

    public static ProjectFileVO toProjectFileVO(ProjectFile file, List<ProjectFileVersionVO> versions) {
        return ProjectFileVO.builder()
                .id(file.getId())
                .projectId(file.getProjectId())
                .fileName(file.getFileName())
                .filePath(file.getFilePath())
                .relativePath(resolveProjectFileRelativePath(file))
                .fileSizeBytes(file.getFileSizeBytes())
                .fileType(file.getFileType())
                .uploadTime(file.getUploadTime())
                .isMain(file.getIsMain())
                .version(file.getVersion())
                .isLatest(file.getIsLatest())
                .versions(versions)
                .build();
    }

    public static ProjectFileVersionVO toProjectFileVersionVO(ProjectFileVersion version) {
        return ProjectFileVersionVO.builder()
                .id(version.getId())
                .fileId(version.getFileId())
                .version(version.getVersion())
                .serverPath(version.getServerPath())
                .fileSizeBytes(version.getFileSizeBytes())
                .uploadedBy(version.getUploadedBy())
                .commitMessage(version.getCommitMessage())
                .uploadedAt(version.getUploadedAt())
                .build();
    }

    private static String resolveDisplayName(UserInfoLite user) {
        if (user == null) {
            return null;
        }
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname().trim();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername().trim();
        }
        return null;
    }

    private static String resolveAvatar(UserInfoLite user) {
        return user == null ? null : user.getAvatarUrl();
    }

    private static String resolveProjectFileRelativePath(ProjectFile file) {
        if (file == null) {
            return null;
        }
        if (StringUtils.hasText(file.getCanonicalPath())) {
            return normalizeProjectPath(file.getCanonicalPath());
        }
        if (StringUtils.hasText(file.getFileKey())) {
            return normalizeProjectPath(file.getFileKey());
        }
        if (StringUtils.hasText(file.getFileName())) {
            return normalizeProjectPath(file.getFileName());
        }
        return null;
    }

    private static String normalizeProjectPath(String value) {
        String normalized = String.valueOf(value)
                .replace('\\', '/')
                .trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static boolean isPreviewSupported(String fileType) {
        if (fileType == null) {
            return false;
        }
        String normalized = fileType.trim().toLowerCase();
        return normalized.matches("png|jpg|jpeg|gif|webp|bmp|svg|txt|md|log|json|xml|yaml|yml|java|js|ts|vue|html|css|pdf");
    }
}
