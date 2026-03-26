package com.alikeyou.itmoduleproject.support;

import com.alikeyou.itmoduleproject.entity.Project;
import com.alikeyou.itmoduleproject.entity.ProjectFile;
import com.alikeyou.itmoduleproject.entity.ProjectFileVersion;
import com.alikeyou.itmoduleproject.entity.ProjectMember;
import com.alikeyou.itmoduleproject.entity.ProjectTask;
import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import com.alikeyou.itmoduleproject.vo.ProjectDetailVO;
import com.alikeyou.itmoduleproject.vo.ProjectFileVO;
import com.alikeyou.itmoduleproject.vo.ProjectFileVersionVO;
import com.alikeyou.itmoduleproject.vo.ProjectListVO;
import com.alikeyou.itmoduleproject.vo.ProjectMemberVO;
import com.alikeyou.itmoduleproject.vo.ProjectTaskVO;

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

    public static ProjectDetailVO toProjectDetailVO(Project project,
                                                    List<ProjectMemberVO> members,
                                                    List<ProjectTaskVO> tasks,
                                                    List<ProjectFileVO> files) {
        return toProjectDetailVO(project, null, members, tasks, files);
    }

    public static ProjectDetailVO toProjectDetailVO(Project project,
                                                    UserInfoLite author,
                                                    List<ProjectMemberVO> members,
                                                    List<ProjectTaskVO> tasks,
                                                    List<ProjectFileVO> files) {
        return ProjectDetailVO.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .category(project.getCategory())
                .sizeMb(project.getSizeMb())
                .stars(project.getStars())
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
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    public static ProjectFileVO toProjectFileVO(ProjectFile file, List<ProjectFileVersionVO> versions) {
        return ProjectFileVO.builder()
                .id(file.getId())
                .projectId(file.getProjectId())
                .fileName(file.getFileName())
                .filePath(file.getFilePath())
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
}
