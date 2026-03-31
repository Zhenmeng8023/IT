package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.ProjectTask;
import com.alikeyou.itmoduleproject.entity.ProjectTaskAttachment;
import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import com.alikeyou.itmoduleproject.repository.ProjectTaskAttachmentRepository;
import com.alikeyou.itmoduleproject.service.ProjectTaskAttachmentService;
import com.alikeyou.itmoduleproject.service.ProjectTaskLogService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.FileStorageService;
import com.alikeyou.itmoduleproject.support.ProjectTaskAccessSupport;
import com.alikeyou.itmoduleproject.support.ProjectUserAssembler;
import com.alikeyou.itmoduleproject.support.ProjectVoMapper;
import com.alikeyou.itmoduleproject.support.StoredFileInfo;
import com.alikeyou.itmoduleproject.vo.TaskAttachmentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectTaskAttachmentServiceImpl implements ProjectTaskAttachmentService {
    private final ProjectTaskAttachmentRepository projectTaskAttachmentRepository;
    private final ProjectTaskAccessSupport taskAccessSupport;
    private final ProjectUserAssembler projectUserAssembler;
    private final ProjectTaskLogService projectTaskLogService;
    private final FileStorageService fileStorageService;

    @Override
    public List<TaskAttachmentVO> listAttachments(Long taskId, Long currentUserId) {
        taskAccessSupport.assertTaskReadable(taskId, currentUserId);
        List<ProjectTaskAttachment> items = projectTaskAttachmentRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
        Map<Long, UserInfoLite> userMap = projectUserAssembler.mapByIds(items.stream().map(ProjectTaskAttachment::getUploadedBy).toList());
        return items.stream().map(item -> ProjectVoMapper.toTaskAttachmentVO(item, userMap.get(item.getUploadedBy()))).toList();
    }

    @Override
    @Transactional
    public TaskAttachmentVO uploadAttachment(Long taskId, MultipartFile file, Long currentUserId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("附件不能为空");
        }
        taskAccessSupport.assertTaskReadable(taskId, currentUserId);
        ProjectTask task = taskAccessSupport.getTaskOrThrow(taskId);
        StoredFileInfo storedFileInfo = fileStorageService.store(task.getProjectId(), "task-attachment", file);
        ProjectTaskAttachment saved = projectTaskAttachmentRepository.save(ProjectTaskAttachment.builder()
                .taskId(taskId)
                .fileName(storedFileInfo.getOriginalFilename())
                .filePath(storedFileInfo.getStoredPath())
                .fileSizeBytes(storedFileInfo.getSize())
                .fileType(normalizeFileType(storedFileInfo.getExtension(), storedFileInfo.getOriginalFilename()))
                .uploadedBy(currentUserId)
                .build());
        projectTaskLogService.recordAttachment(taskId, currentUserId, null, saved.getFileName());
        UserInfoLite uploader = projectUserAssembler.mapByIds(List.of(currentUserId)).get(currentUserId);
        return ProjectVoMapper.toTaskAttachmentVO(saved, uploader);
    }

    @Override
    @Transactional
    public void deleteAttachment(Long attachmentId, Long currentUserId) {
        ProjectTaskAttachment attachment = getAttachmentEntity(attachmentId, currentUserId);
        boolean canManage = taskAccessSupport.canManageTask(attachment.getTaskId(), currentUserId);
        if (!Objects.equals(attachment.getUploadedBy(), currentUserId) && !canManage) {
            throw new BusinessException("无权删除该附件");
        }
        projectTaskAttachmentRepository.delete(attachment);
        fileStorageService.delete(attachment.getFilePath());
        projectTaskLogService.recordAttachment(attachment.getTaskId(), currentUserId, attachment.getFileName(), null);
    }

    @Override
    public ProjectTaskAttachment getAttachmentEntity(Long attachmentId, Long currentUserId) {
        ProjectTaskAttachment attachment = projectTaskAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new BusinessException("附件不存在"));
        taskAccessSupport.assertTaskReadable(attachment.getTaskId(), currentUserId);
        return attachment;
    }

    private String normalizeFileType(String extension, String fileName) {
        if (extension != null && !extension.isBlank()) {
            return extension.replace(".", "").trim().toLowerCase();
        }
        if (fileName == null || !fileName.contains(".")) {
            return null;
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).trim().toLowerCase();
    }
}
