package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.entity.ProjectTaskAttachment;
import com.alikeyou.itmoduleproject.vo.TaskAttachmentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProjectTaskAttachmentService {
    List<TaskAttachmentVO> listAttachments(Long taskId, Long currentUserId);

    TaskAttachmentVO uploadAttachment(Long taskId, MultipartFile file, Long currentUserId);

    void deleteAttachment(Long attachmentId, Long currentUserId);

    ProjectTaskAttachment getAttachmentEntity(Long attachmentId, Long currentUserId);
}
