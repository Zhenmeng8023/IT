package com.alikeyou.itmoduleproject.support;

import com.alikeyou.itmoduleproject.entity.Project;
import com.alikeyou.itmoduleproject.entity.ProjectFile;
import com.alikeyou.itmoduleproject.entity.ProjectFileVersion;
import com.alikeyou.itmoduleproject.entity.ProjectMember;
import com.alikeyou.itmoduleproject.entity.ProjectTask;
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
            .status(project.getStatus())
            .tags(project.getTags())
            .templateId(project.getTemplateId())
            .visibility(project.getVisibility())
            .createdAt(project.getCreatedAt())
            .updatedAt(project.getUpdatedAt())
            .build();
    }

    public static ProjectDetailVO toProjectDetailVO(Project project, List<ProjectMemberVO> members, List<ProjectTaskVO> tasks, List<ProjectFileVO> files) {
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
        return ProjectMemberVO.builder()
            .id(member.getId())
            .projectId(member.getProjectId())
            .userId(member.getUserId())
            .role(member.getRole())
            .status(member.getStatus())
            .joinedAt(member.getJoinedAt())
            .build();
    }

    public static ProjectTaskVO toProjectTaskVO(ProjectTask task) {
        return ProjectTaskVO.builder()
            .id(task.getId())
            .projectId(task.getProjectId())
            .title(task.getTitle())
            .description(task.getDescription())
            .status(task.getStatus())
            .priority(task.getPriority())
            .assigneeId(task.getAssigneeId())
            .createdBy(task.getCreatedBy())
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
}
